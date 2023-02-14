/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fasten.server.plugins.kafka;

import eu.fasten.core.exceptions.UnrecoverableError;
import eu.fasten.core.plugins.KafkaPlugin;
import eu.fasten.core.plugins.KafkaPlugin.ProcessingLane;
import eu.fasten.core.plugins.KafkaPlugin.SingleRecord;
import eu.fasten.server.plugins.FastenServerPlugin;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.jooq.exception.DataAccessException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class FastenKafkaPlugin implements FastenServerPlugin {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);

    private final Logger logger = LoggerFactory.getLogger(FastenKafkaPlugin.class);
    private final KafkaPlugin plugin;

    private final AtomicBoolean shouldFinishProcessing = new AtomicBoolean(false);
    private KafkaConsumer<String, String> connNorm;
    private KafkaConsumer<String, String> connPrio;
    private KafkaProducer<String, String> producer;

    private List<String> normTopics;
    private List<String> prioTopics;
    private final String outputTopic;

    private final int skipOffsets;

    private final String writeDirectory;
    private final String writeLink;

    // Configuration for consumer timeout.
    private final boolean consumeTimeoutEnabled;
    private final long consumeTimeout;
    private final boolean exitOnTimeout;

    // Local storage for duplicate processing.
    private final LocalStorage localStorage;

    // Executor service which creates a thread pool and re-uses threads when
    // possible.
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private boolean hadMessagesOnLastPollCycle;

    /**
     * Constructs a FastenKafkaConsumer.
     *
     * @param consumerNormProperties properties of a consumer
     * @param plugin                 Kafka plugin
     * @param skipOffsets            skip offset number
     */
    public FastenKafkaPlugin(boolean enableKafka, Properties consumerNormProperties, Properties consumerPrioProperties,
            Properties producerProperties, KafkaPlugin plugin, int skipOffsets, String writeDirectory, String writeLink,
            String outputTopic, boolean consumeTimeoutEnabled, long consumeTimeout, boolean exitOnTimeout,
            boolean enableLocalStorage, String localStorageDir) {
        this.plugin = plugin;

        if (enableKafka) {
            this.connNorm = new KafkaConsumer<>(consumerNormProperties);
            // For the priority connection, the client name should be different from the
            // normal one
            this.connPrio = new KafkaConsumer<>(consumerPrioProperties);
            this.producer = new KafkaProducer<>(producerProperties);
        }

        this.skipOffsets = skipOffsets;
        if (writeDirectory != null) {
            this.writeDirectory = writeDirectory.endsWith(File.separator)
                    ? writeDirectory.substring(0, writeDirectory.length() - 1)
                    : writeDirectory;
        } else {
            this.writeDirectory = null;
        }
        if (writeLink != null) {
            this.writeLink = writeLink.endsWith(File.separator) ? writeLink.substring(0, writeLink.length() - 1)
                    : writeLink;

        } else {
            this.writeLink = null;
        }

        // If the write link is not null, and local storage is enabled. Initialize it.
        if (enableLocalStorage) {
            this.localStorage = new LocalStorage(localStorageDir);
        } else {
            this.localStorage = null;
        }

        this.outputTopic = outputTopic;
        this.consumeTimeoutEnabled = consumeTimeoutEnabled;
        this.consumeTimeout = consumeTimeout;
        this.exitOnTimeout = exitOnTimeout;
        registerShutDownHook();
        logger.debug("Constructed a Kafka plugin for " + plugin.getClass().getCanonicalName());
    }

    public FastenKafkaPlugin(Properties consumerNormProperties, Properties consumerPrioProperties,
            Properties producerProperties, KafkaPlugin plugin, int skipOffsets, String writeDirectory, String writeLink,
            String outputTopic, boolean consumeTimeoutEnabled, long consumeTimeout, boolean exitOnTimeout,
            boolean enableLocalStorage, String localStorageDir) {
        this(true, consumerNormProperties, consumerPrioProperties, producerProperties, plugin, skipOffsets,
                writeDirectory, writeLink, outputTopic, consumeTimeoutEnabled, consumeTimeout, exitOnTimeout,
                enableLocalStorage, localStorageDir);
    }

    @Override
    public void run() {
        subscribeToTopics();

        if (this.skipOffsets == 1) {
            skipPartitionOffsets();
        }

        try {
            while (!shouldFinishProcessing.get()) {
                try {
                    if (plugin.consumeTopic().isPresent()) {
                        handleConsuming();
                    } else {
                        doCommitSync(ProcessingLane.NORMAL);
                        handleProducing(null, System.currentTimeMillis(), ProcessingLane.NORMAL);
                    }
                } catch (WakeupException e) {
                    if (shouldFinishProcessing.get()) {
                        // WakeupException is used to interrupt long-running polls, e.g., we are use it
                        // to interrupt polls when handling shutdown signals. The exception can be
                        // ignored.
                    }
                }
            }
        } finally {
            connNorm.close();
            connPrio.close();
            logger.info("Plugin {} stopped gracefully", plugin.name());
        }
    }

    private void subscribeToTopics() {
        if (plugin.consumeTopic().isPresent()) {
            normTopics = plugin.consumeTopic().get().stream().filter(s -> !s.contains("priority"))
                    .collect(Collectors.toList());
            connNorm.subscribe(normTopics);

            prioTopics = plugin.consumeTopic().get().stream().filter(s -> s.contains("priority"))
                    .collect(Collectors.toList());
            if (!prioTopics.isEmpty()) {
                connPrio.subscribe(prioTopics);
            }
        }
    }

    /**
     * Starts the plugin.
     */
    public void start() {
        this.run();
    }

    /**
     * Sends a wake up signal to Kafka consumer and stops it.
     */
    public void stop() {
        shouldFinishProcessing.set(true);
    }

    /**
     * Consumes a message from a Kafka topics and passes it to a plugin.
     */
    public void handleConsuming() {
        // normally, we ONLY wait on PRIO and ONLY when there have been no messages in any lane...
        var prioTimeout = hadMessagesOnLastPollCycle ? Duration.ZERO : POLL_TIMEOUT;
        // ... unless there is no subscription for PRIO, then we wait in NORMAL instead
        var normTimeout = prioTopics.isEmpty() ? POLL_TIMEOUT : Duration.ZERO;

        hadMessagesOnLastPollCycle = false;
        
        if (!prioTopics.isEmpty()) {
            // Refresh connection timeout of normal consumer when priority records are
            // processed
            sendHeartBeat(connNorm);

            var prioRecords = connPrio.poll(prioTimeout);
            ArrayList<ImmutablePair<Long, Integer>> priorityMessagesProcessed = new ArrayList<>();
            String topicName = null;

            for (var r : prioRecords) {
                hadMessagesOnLastPollCycle = true;
                topicName = r.topic();
                logger.info("Read priority message offset {} from partition {}.", r.offset(), r.partition());
                processRecord(r, ProcessingLane.PRIORITY);
                logger.info("Successfully processed priority message offset {} from partition {}.", r.offset(),
                        r.partition());

                priorityMessagesProcessed.add(new ImmutablePair<>(r.offset(), r.partition()));
            }
            doCommitSync(ProcessingLane.PRIORITY);

            if (localStorage != null && topicName != null) {
                localStorage.clear(priorityMessagesProcessed.stream().map((x) -> x.right).collect(Collectors.toList()), topicName);
            }
        }

        if(hadMessagesOnLastPollCycle) {
            return; // skip normal
        }

        if (!normTopics.isEmpty()) {
            sendHeartBeat(connPrio);
            var records = connNorm.poll(normTimeout);

            // Keep a list of all records and offsets we processed (by default this is only
            // 1).
            ArrayList<ImmutablePair<Long, Integer>> messagesProcessed = new ArrayList<ImmutablePair<Long, Integer>>();
            String topicName = null;

            // Although we loop through all records, by default we only poll 1 record.
            for (var r : records) {
                hadMessagesOnLastPollCycle = true;
                topicName = r.topic();
                logger.info("Read normal message offset {} from partition {}.", r.offset(), r.partition());
                processRecord(r, ProcessingLane.NORMAL);
                logger.info("Successfully processed normal message offset {} from partition {}.", r.offset(),
                        r.partition());

                messagesProcessed.add(new ImmutablePair<>(r.offset(), r.partition()));
            }

            // Commit only after _all_ records are processed.
            // For most plugins, this loop will only process 1 record (since
            // max.poll.records is 1).
            doCommitSync(ProcessingLane.NORMAL);

            // More logging.
            String allOffsets = messagesProcessed.stream().map((x) -> x.left).map(Object::toString)
                    .collect(Collectors.joining(", "));
            String allPartitions = messagesProcessed.stream().map((x) -> x.right).map(Object::toString)
                    .collect(Collectors.joining(", "));

            if (!records.isEmpty()) {
                logger.info("Committed offsets [" + allOffsets + "] of partitions [" + allPartitions + "].");
            }

            // If local storage is enabled, clear the correct partitions after offsets are
            // committed.
            if (localStorage != null && topicName != null) {
                localStorage.clear(messagesProcessed.stream().map((x) -> x.right).collect(Collectors.toList()), topicName);
            }
        }
    }

    /**
     * Consumer strategy (using local storage):
     * <p>
     * 1. Poll one record (by default). 2. If the record hash is in local storage:
     * a. Produce to error topic (this record is probably processed before and
     * caused a crash or timeout). b. Commit the offset, if producer confirmed
     * sending the message. c. Delete record in local storage. d. Go back to 1. 3.
     * If the record hash is _not_ in local storage: a. Process the record. b.
     * Produce its results (either to the error topic, or output topic). c. Commit
     * the offset if producer confirmed sending the message. d. Delete record in
     * local storage. e. Go back to 1.
     * <p>
     * This strategy provides at-least-once semantics.
     */
    public void processRecord(ConsumerRecord<String, String> record, ProcessingLane lane) {
        long consumeTimestamp = System.currentTimeMillis();

        try {
            if (localStorage != null) { // If local storage is enabled.
                if (localStorage.exists(record.value(), record.partition(), record.topic())) { // This plugin already consumed this
                    // record before, we will not process it
                    // now.
                    String hash = localStorage.getSHA1(record.value());
                    logger.info("Already processed record with hash: {}, skipping it now.", hash);
                    plugin.setPluginError(new ExistsInLocalStorageException());
                } else {
                    try {
                        localStorage.store(record.value(), record.partition(), record.topic());
                    } catch (IOException e) {
                        // We couldn't store the message SHA. Will just continue processing, but log the
                        // error.
                        // This strategy might result in the deadlock/retry behavior of the same
                        // coordinate.
                        // However, if local storage is failing we can't store the CG's either and
                        // that's already a problem.
                        logger.error("Trying to store the hash of a record, but failed due to an IOException", e);
                    } finally { // Event if we hit an IOException, we will execute this finally block.
                        if (consumeTimeoutEnabled) {
                            consumeWithTimeout(record.value(), consumeTimeout, exitOnTimeout, lane);
                        } else {
                            plugin.consume(record.value(), lane);
                        }
                    }
                }
            } else { // If local storage is not enabled.
                if (consumeTimeoutEnabled) {
                    consumeWithTimeout(record.value(), consumeTimeout, exitOnTimeout, lane);
                } else {
                    plugin.consume(record.value(), lane);
                }
            }
        } catch (UnrecoverableError e) {
            // In rare circumstances, plug-ins throw UnrecoverableError to crash and
            // therefore K8s will restart the plug-in.
            logger.error("Forced to stop the plug-in due to ", e);
            throw e;

        } catch (DataAccessException e) {
            // Database-related errors
            throw new UnrecoverableError("Could not connect to or query the Postgres DB and the plug-in should be stopped and restarted.",
                    e.getCause());
        } catch (Exception e) {
            logger.error("An error occurred in " + plugin.getClass().getCanonicalName(), e);
            plugin.setPluginError(e);
        }

        // We always produce, it does not matter if local storage is enabled or not.
        handleProducing(record.value(), consumeTimestamp, lane);
    }

    /**
     * Writes messages to server log and stdout/stderr topics.
     *
     * @param input input message [can be null]
     */
    public void handleProducing(String input, long consumeTimestamp, ProcessingLane lane) {
        String outputTopicName;
        if (lane == ProcessingLane.PRIORITY) {
            outputTopicName = String.format("fasten.%s.priority.out", outputTopic);
        } else {
            outputTopicName = String.format("fasten.%s.out", outputTopic);
        }
        if (plugin.getPluginError() != null) {
            emitMessage(this.producer, String.format("fasten.%s.err", outputTopic),
                    getStdErrMsg(input, plugin.getPluginError(), consumeTimestamp));
        } else {
            var results = plugin.produceMultiple(lane);
            for (var res : results) {
                String payload = res.payload;
                if (writeDirectory != null && !writeDirectory.equals("")) {
                    // replace payload with file link in case it is written
                    try {
                        payload = writeToFile(res);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write the file " + this.writeDirectory + res.outputPath + "because" + e.getMessage());
                    }
                }
                emitMessage(this.producer, outputTopicName, getStdOutMsg(input, payload, consumeTimestamp));
            }
        }
    }

    /**
     * Send message to Kafka topic.
     *
     * @param producer Kafka producer
     * @param topic    topic to send to
     * @param msg      message
     */
    private void emitMessage(KafkaProducer<String, String> producer, String topic, String msg) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, msg);

        producer.send(record, (recordMetadata, e) -> {
            if (recordMetadata != null) {
                logger.debug("Sent: {} to {}", msg, topic);
            } else {
                e.printStackTrace();
            }
        });

        producer.flush();
    }

    /**
     * Writes output or error message to JSON file and return JSON object containing
     * a link to to written file.
     *
     * @param result message to write
     * @return Path to a newly written JSON file
     */
    private String writeToFile(SingleRecord result) throws NullPointerException, IOException {
        var path = result.outputPath;
        var pathWithoutFilename = path.substring(0, path.lastIndexOf(File.separator));

        File directory = new File(this.writeDirectory + pathWithoutFilename);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Failed to create parent directories at " + this.writeDirectory + pathWithoutFilename);
        }

        File file = new File(this.writeDirectory + path);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        fw.write(result.payload);
        fw.flush();
        fw.close();

        JSONObject link = new JSONObject();
        link.put("dir", file.getAbsolutePath());

        if (this.writeLink != null && !this.writeLink.equals("")) {
            link.put("link", this.writeLink + path);
        }
        return link.toString();
    }

    /**
     * Create a message that will be send to STDOUT of a given plugin.
     *
     * @param input   consumed record
     * @param payload output of the plugin
     * @return stdout message
     */
    private String getStdOutMsg(String input, String payload, long consumeTimestamp) {
        var stdoutMsg = getStdMsg(input, consumeTimestamp);
        stdoutMsg.put("payload", StringUtils.isNotEmpty(payload) ? new JSONObject(payload) : "");
        return stdoutMsg.toString();
    }

    /**
     * Create a message that will be send to STDERR of a given plugin.
     *
     * @param input consumed record
     * @return stderr message
     */
    private String getStdErrMsg(String input, Throwable pluginError, long consumeTimestamp) {

        var error = new JSONObject();
        error.put("type", pluginError.getClass().getName());
        error.put("message", pluginError.getMessage());
        error.put("stacktrace", ExceptionUtils.getStackTrace(pluginError));

        var stderrMsg = getStdMsg(input, consumeTimestamp);
        stderrMsg.put("error", error);

        return stderrMsg.toString();
    }

    private JSONObject getStdMsg(String input, long consumeTimestamp) {
        var stdoutMsg = new JSONObject();
        stdoutMsg.put("createdAt", System.currentTimeMillis());
        stdoutMsg.put("consumedAt", consumeTimestamp);
        stdoutMsg.put("plugin", plugin.getClass().getName());
        stdoutMsg.put("version", plugin.version());
        try {
            stdoutMsg.put("host", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            stdoutMsg.put("host", "unknown");
        }
        stdoutMsg.put("input", StringUtils.isNotEmpty(input) ? new JSONObject(input) : "");
        return stdoutMsg;
    }

    /**
     * This is a synchronous commits and will block until either the commit succeeds
     * or an unrecoverable error is encountered.
     */
    private void doCommitSync(ProcessingLane kafkaRecordKind) {
        try {
            if (kafkaRecordKind == ProcessingLane.PRIORITY) {
                connPrio.commitSync();
            } else {
                connNorm.commitSync();
            }
        } catch (WakeupException e) {
            // we're shutting down, but finish the commit first and then
            // rethrow the exception so that the main loop can exit
            doCommitSync(kafkaRecordKind);
            throw e;
        } catch (CommitFailedException e) {
            // the commit failed with an unrecoverable error. if there is any
            // internal state which depended on the commit, you can clean it
            // up here. otherwise it's reasonable to ignore the error and go on
            logger.error("Commit failed", e);
        }
    }

    /**
     * This method adds one to the offset of all the partitions of a topic. This is
     * useful when you want to skip an offset with FATAL errors when the FASTEN
     * server is restarted. Please note that this is NOT the most efficient way to
     * restart FASTEN server in the case of FATAL errors.
     */
    private void skipPartitionOffsets() {
        ArrayList<TopicPartition> topicPartitions = new ArrayList<>();
        List<String> topics = new ArrayList<>();
        this.plugin.consumeTopic().ifPresentOrElse(topics::addAll, () -> {});
        if (topics.isEmpty()) {
            return;
        }
        // Note that this assumes that the consumer is subscribed to one topic only
        for (PartitionInfo p : this.connNorm.partitionsFor(topics.get(0))) {
            topicPartitions.add(new TopicPartition(topics.get(0), p.partition()));
        }

        ConsumerRecords<String, String> records = dummyPoll(this.connNorm);

        if (records.count() != 0) {
            for (TopicPartition tp : topicPartitions) {
                logger.debug("Topic: {} | Current offset for partition {}: {}", topics.get(0), tp,
                        this.connNorm.position(tp));

                this.connNorm.seek(tp, this.connNorm.position(tp) + 1);

                logger.debug("Topic: {} | Offset for partition {} is set to {}", topics.get(0), tp,
                        this.connNorm.position(tp));
            }
        }
    }

    /**
     * This method can be used to simulate Kafka's heartbeat to avoid the eviction
     * of a consumer.
     *
     * See https://stackoverflow.com/a/43722731
     */
    private static void sendHeartBeat(KafkaConsumer<String, String> kafkaConn) {
        if(!kafkaConn.subscription().isEmpty()) {
            var currentlyAssignedPartitions = kafkaConn.assignment();
            kafkaConn.pause(currentlyAssignedPartitions);
            kafkaConn.poll(Duration.ZERO);
            kafkaConn.resume(currentlyAssignedPartitions);
        }
    }

    /**
     * This is a dummy poll method for calling lazy methods such as seek.
     *
     * @param consumer Kafka consumer
     * @return consumed Kafka record
     */
    private ConsumerRecords<String, String> dummyPoll(KafkaConsumer<String, String> consumer) {
        ConsumerRecords<String, String> statusRecords;
        int i = 0;
        do {
            statusRecords = consumer.poll(Duration.ofMillis(100));
            i++;
        } while (i <= 5 && statusRecords.count() == 0);

        return statusRecords;
    }

    /**
     * Consumes an input with a timeout. If the timeout is exceeded the thread
     * handling the message is killed.
     *
     * @param input   the input message to be consumed.
     * @param timeout the timeout in seconds. I.e. the maximum time a plugin can
     *                spend on processing a record.
     *                <p>
     *                Based on:
     *                https://stackoverflow.com/questions/1164301/how-do-i-call-some-blocking-method-with-a-timeout-in-java
     */
    public void consumeWithTimeout(String input, long timeout, boolean exitOnTimeout, ProcessingLane lane) {
        Runnable consumeTask = () -> plugin.consume(input, lane);

        // Submit the consume task to a thread.
        var futureConsumeTask = executorService.submit(consumeTask);

        try {
            futureConsumeTask.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            // In this situation the consumeTask took longer than the timeout.
            // We will send an error to the err topic by setting the plugin error.
            plugin.setPluginError(timeoutException);
            logger.error("A TimeoutException occurred, processing a record took more than " + timeout + " seconds.");
            if (exitOnTimeout) { // Exit if the timeout is reached.
                System.exit(0);
            }
        } catch (InterruptedException interruptedException) {
            // The consumeTask thread was interrupted.
            plugin.setPluginError(interruptedException);
            logger.error("A InterruptedException occurred", interruptedException);
        } catch (ExecutionException executionException) {
            // In this situation the consumeTask threw an exception during computation.
            // We kind of expect this not to happen, because (at least for OPAL) plugins
            // should with exception themselves.
            plugin.setPluginError(executionException);
            logger.error("A ExecutionException occurred", executionException);
        } finally {
            // Finally we will kill the current thread if it's still running so we can
            // continue processing the next record.
            futureConsumeTask.cancel(true);
        }
    }

    /**
     * Verify is the consumer timeout is enabled.
     *
     * @return if a consumer timeout is enabled.
     */
    public boolean isConsumeTimeoutEnabled() {
        return consumeTimeoutEnabled;
    }

    /**
     * Get the consume timeout (in seconds).
     *
     * @return consume timeout.
     */
    public long getConsumeTimeout() {
        return consumeTimeout;
    }

    /**
     * It cleans up resources after receiving the SIGTERM signal. E.g. closing Kafka
     * connections
     */
    private void registerShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            // It aborts (long-running) polls for handling process signals in this case.
            connNorm.wakeup();
            connPrio.wakeup();
            logger.info("Waking up the Kafka consumers before shutting down the JVM");
        }));
    }
}