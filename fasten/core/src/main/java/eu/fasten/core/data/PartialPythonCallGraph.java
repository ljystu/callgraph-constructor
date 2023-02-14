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

package eu.fasten.core.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

//Map<PythonScope, Map<String, PythonType>>
public class PartialPythonCallGraph extends PartialCallGraph {

    public static final String classHierarchyJSONKey = "modules";

    protected EnumMap<PythonScope, Map<String, PythonType>> classHierarchy;

    /**
     * Includes all the edges of the revision call graph (internal, external,
     * and resolved).
     */
    protected CPythonGraph graph;

    /**
     * Creates {@link PartialPythonCallGraph} with the given data.
     *
     * @param forge          the forge.
     * @param product        the product.
     * @param version        the version.
     * @param timestamp      the timestamp (in seconds from UNIX epoch); optional: if not present,
     *                       it is set to -1.
     * @param cgGenerator    The name of call graph generator that generated this call graph.
     * @param classHierarchy class hierarchy of this revision including all classes of the revision
     * @param graph          the call graph (no control is done on the graph) {@link CPythonGraph}
     */
    public PartialPythonCallGraph(final String forge, final String product, final String version,
                                  final long timestamp, final String cgGenerator,
                                  final EnumMap<PythonScope, Map<String, PythonType>> classHierarchy,
                                  final CPythonGraph graph) {
        super(forge, product, version, timestamp, cgGenerator);
        this.classHierarchy = classHierarchy;
        this.graph = graph;
    }

    /**
     * Creates {@link PartialCallGraph} for the given JSONObject.
     *
     * @param json JSONObject of a revision call graph.
     */
    public PartialPythonCallGraph(final JSONObject json) throws JSONException {
        super(json);
        this.classHierarchy = getCHAFromJSON(json.getJSONObject(classHierarchyJSONKey));
        this.graph = new CPythonGraph(json.getJSONObject("graph"));
    }

    /**
     * Creates a class hierarchy for the given JSONObject.
     *
     * @param cha JSONObject of a cha.
     */
    public EnumMap<PythonScope, Map<String, PythonType>> getCHAFromJSON(final JSONObject cha) {
        final Map<PythonScope, Map<String, PythonType>> methods = new HashMap<>();

        final var internal = cha.getJSONObject("internal");
        final var external = cha.getJSONObject("external");

        methods.put(PythonScope.internal, parseModules(internal));
        methods.put(PythonScope.external, parseModules(external));

        return new EnumMap<>(methods);
    }

    /**
     * Helper method to parse modules.
     *
     * @param json JSONObject that contains methods.
     */
    public static Map<String, PythonType> parseModules(final JSONObject json) {
        final Map<String, PythonType> modules = new HashMap<>();

        for (final var module : json.keySet()) {
            final var typeJson = json.getJSONObject(module);
            final var type = new PythonType(typeJson);
            modules.put(module, type);
        }
        return modules;
    }

    @Override
    public int getNodeCount() {
        return this.mapOfAllMethods().size();
    }

    /**
     * Returns the map of all the methods of this object.
     *
     * @return a Map of method ids and their corresponding {@link FastenURI}
     */
    public Map<Integer, PythonNode> mapOfAllMethods() {
        Map<Integer, PythonNode> result = new HashMap<>();
        for (final var aClass : this.getClassHierarchy().get(PythonScope.internal).entrySet()) {
            result.putAll(aClass.getValue().getMethods());
        }
        for (final var aClass : this.getClassHierarchy().get(PythonScope.external).entrySet()) {
            result.putAll(aClass.getValue().getMethods());
        }
        return result;
    }

    /**
     * Produces the JSON representation of class hierarchy.
     *
     * @param cha class hierarchy
     * @return the JSON representation
     */
    public JSONObject classHierarchyToJSON(final EnumMap<PythonScope, Map<String, PythonType>> cha) {
        final var result = new JSONObject();

        final var internal = methodsToJSON(cha.get(PythonScope.internal));
        final var external = methodsToJSON(cha.get(PythonScope.external));

        result.put("internal", internal);
        result.put("external", external);

        return result;
    }

    @Override
    public CPythonGraph getGraph() {
        return graph;
    }

    public JSONObject toJSON() {
        final var result = super.toJSON();
        result.put(classHierarchyJSONKey, classHierarchyToJSON(classHierarchy));
        result.put("graph", graph.toJSON());
        return result;
    }

    /**
     * Produces the JSON of methods
     *
     * @param types the python types
     */
    public static JSONObject methodsToJSON(final Map<String, PythonType> types) {
        final var result = new JSONObject();
        for (final var type : types.entrySet()) {
            result.put(type.getKey(), type.getValue().toJSON());
        }
        return result;
    }

    public EnumMap<PythonScope, Map<String, PythonType>> getClassHierarchy() {
        return classHierarchy;
    }

    /**
     * Returns a string representation of the revision.
     *
     * @return String representation of the revision.
     */
    public String getRevisionName() {
        return this.product + "_" + this.version;
    }

    /**
     * Checks whether this {@link PartialCallGraph} is empty, e.g. has no calls.
     *
     * @return true if this {@link PartialCallGraph} is empty
     */

    public boolean isCallGraphEmpty() {
        return this.graph.getInternalCalls().isEmpty()
                && this.graph.getExternalCalls().isEmpty()
                && this.graph.getResolvedCalls().isEmpty();
    }
}
