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

package eu.fasten.analyzer.javacgopal;

import static eu.fasten.core.plugins.KafkaPlugin.ProcessingLane.NORMAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.fasten.core.data.PartialJavaCallGraph;
import eu.fasten.core.data.opal.exceptions.EmptyCallGraphException;
import eu.fasten.core.data.opal.exceptions.MissingArtifactException;

class OPALPluginTest {

    private OPALPlugin.OPAL plugin;

    @BeforeEach
    public void setUp() {
        plugin = new OPALPlugin.OPAL();
    }

    @Test
    public void testConsumerTopicNotSetByDefault() {
        assertThrows(RuntimeException.class, ()->{
            plugin.consumeTopic();
        });
    }

    @Test
    public void testSetTopic() {
        List<String> consumeTopics = Collections.singletonList("fasten.mvn.pkg");
        plugin.setTopics(consumeTopics);
        assertTrue(plugin.consumeTopic().isPresent());
        assertEquals(Optional.of(consumeTopics), plugin.consumeTopic());
    }

    @Test
    public void testConsumeFromDifferentRepo() throws JSONException {
        JSONObject coordinateJSON = new JSONObject("{" +
                "\"payload\": {" +
                "\"artifactId\": \"common\"," +
                "\"groupId\": \"android.arch.core\"," +
                "\"version\": \"1.1.1\"," +
                "\"artifactRepository\": \"https://dl.google.com/android/maven2/\"" +
                "}}");

        plugin.consume(coordinateJSON.toString(), NORMAL);

        assertTrue(plugin.produce().isPresent());
        assertFalse(new PartialJavaCallGraph(new JSONObject(plugin.produce().get()))
                .isCallGraphEmpty());
    }

    @Test
    public void testConsume() throws JSONException {

        JSONObject coordinateJSON = new JSONObject("{\n" +
                "    \"groupId\": \"org.slf4j\",\n" +
                "    \"artifactId\": \"slf4j-api\",\n" +
                "    \"version\": \"1.7.29\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}");

        plugin.consume(coordinateJSON.toString(), NORMAL);

        assertTrue(plugin.produce().isPresent());
        assertFalse(new PartialJavaCallGraph(new JSONObject(plugin.produce().get()))
                .isCallGraphEmpty());
    }

    @Test
    public void testConsumeWithPayload() throws JSONException {

        JSONObject coordinateJSON = new JSONObject("{\"payload\":" +
                "{\n" +
                "    \"groupId\": \"org.slf4j\",\n" +
                "    \"artifactId\": \"slf4j-api\",\n" +
                "    \"version\": \"1.7.29\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}}");

        plugin.consume(coordinateJSON.toString(), NORMAL);

        assertTrue(plugin.produce().isPresent());
        assertFalse(new PartialJavaCallGraph(new JSONObject(plugin.produce().get()))
                .isCallGraphEmpty());
    }

    @Test
    public void testFileNotFoundException() {
        JSONObject noJARFile = new JSONObject("{\n" +
                "    \"groupId\": \"com.visionarts\",\n" +
                "    \"artifactId\": \"power-jambda-pom\",\n" +
                "    \"version\": \"0.9.10\",\n" +
                "    \"date\":\"1521511260\"\n" +
                "}");

        plugin.consume(noJARFile.toString(), NORMAL);
        var error = plugin.getPluginError();
        assertFalse(plugin.produce().isPresent());
        assertEquals(MissingArtifactException.class.getSimpleName(), error.getClass().getSimpleName());
    }

    @Test
    public void testEmptyCallGraph() throws JSONException {
        JSONObject emptyCGCoordinate = new JSONObject("{\n"
                + "    \"groupId\": \"activemq\",\n"
                + "    \"artifactId\": \"activemq\",\n"
                + "    \"version\": \"release-1.5\",\n"
                + "    \"date\":\"1574072773\"\n"
                + "}");

        plugin.consume(emptyCGCoordinate.toString(), NORMAL);
        assertFalse(plugin.produce().isPresent());
        assertEquals(EmptyCallGraphException.class, plugin.getPluginError().getClass());
    }

    @Disabled
    @Test
    public void testShouldNotFaceClassReadingError() throws JSONException {

        JSONObject coordinateJSON1 = new JSONObject("{\n" +
                "    \"groupId\": \"com.zarbosoft\",\n" +
                "    \"artifactId\": \"coroutines-core\",\n" +
                "    \"version\": \"0.0.3\",\n" +
                "    \"date\":\"1574072773\"\n" +
                "}");

        plugin.consume(coordinateJSON1.toString(), NORMAL);

        assertTrue(plugin.produce().isPresent());
        assertFalse(new PartialJavaCallGraph(new JSONObject(plugin.produce().get()))
                .isCallGraphEmpty());
    }

    @Test
    public void testName() {
        assertEquals("OPAL", plugin.name());
    }
}
