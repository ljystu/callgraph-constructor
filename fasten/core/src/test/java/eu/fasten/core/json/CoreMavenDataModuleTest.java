/*
 * Copyright 2022 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fasten.core.json;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.fasten.core.maven.data.Dependency;
import eu.fasten.core.maven.data.Exclusion;
import eu.fasten.core.maven.data.PomBuilder;
import eu.fasten.core.maven.data.Scope;
import eu.fasten.core.maven.data.VersionConstraint;

public class CoreMavenDataModuleTest {

    private ObjectMapper om;

    @BeforeEach
    public void setup() {
        om = new ObjectMapperBuilder() {
            @Override
            protected ObjectMapper addMapperOptions(ObjectMapper om) {
                return om.enable(SerializationFeature.INDENT_OUTPUT);
            }
        }.build();
    }

    @Test
    public void testPom() throws Exception {
        var pom = new PomBuilder().pom();
        var json = om.writeValueAsString(pom);
        var obj = new JSONObject(json);
        assertFalse(obj.has("ga"));
        assertFalse(obj.has("gav"));
        assertFalse(obj.has("hashCode"));
    }

    @Test
    public void testDependencyOldCanBeRead() throws Exception {
        var json = "{\"versionConstraints\":[\"[1,2]\"],\"groupId\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"artifactId\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":false,\"type\":\"type\"}";
        var actual = om.readValue(json, Dependency.class);
        var expected = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")),
                Set.of(new Exclusion("g2", "a2")), Scope.TEST, false, "type", "sources");
        assertEquals(expected, actual);
    }

    @Test
    public void testDependencyFixNonEmptyVersionConstraintsOld() throws Exception {
        var json = "{\"versionConstraints\":[\"\"],\"groupId\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"artifactId\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":true,\"type\":\"type\"}";
        var out = om.readValue(json, Dependency.class);
        assertTrue(out.getVersionConstraints().isEmpty());
    }

    @Test
    public void testDependency() {
        var d = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")), Set.of(new Exclusion("g2", "a2")),
                Scope.TEST, true, "type", "sources");
        var json = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"a\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":true,\"type\":\"type\"}";
        test(d, json);
    }

    @Test
    public void testDependencyDefault() {
        var d = new Dependency("g", "a", "v");
        var json = "{\"v\":[\"v\"],\"g\":\"g\",\"a\":\"a\"}";
        test(d, json);
    }

    @Test
    public void testDependency_compileScope() {
        var d = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")), Set.of(new Exclusion("g2", "a2")),
                Scope.COMPILE, true, "type", "sources");
        var json = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"classifier\":\"sources\",\"a\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":true,\"type\":\"type\"}";
        test(d, json);
    }

    @Test
    public void testDependency_nonOptional() {
        var d = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")), Set.of(new Exclusion("g2", "a2")),
                Scope.TEST, false, "type", "sources");
        var json = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"a\":\"a1\",\"exclusions\":[\"g2:a2\"],\"type\":\"type\"}";
        test(d, json);
    }

    @Test
    public void testDependency_noExclusions() {
        var d = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")), Set.of(), Scope.TEST, true, "type",
                "sources");
        var json = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"a\":\"a1\",\"optional\":true,\"type\":\"type\"}";
        test(d, json);
    }

    @Test
    public void testDependency_invalidExclusion() throws JsonMappingException, JsonProcessingException {
        // This is a regression test that reflects old json data, which can contain
        // broken serializations. This does not happen anymore for newly generated data.
        var rawDep = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"a\":\"a1\",\"exclusions\":[%s],\"type\":\"type\"}";
        for (var jsonExcl : new String[] { "", "\"\"", "\"x\"", "\":\"", "\"x:\"", "\":x\"", "\"x:y:\"", "\":x:y\"",
                "\"x:y:z\"" }) {

            var jsonDep = String.format(rawDep, jsonExcl);

            var actual = om.readValue(jsonDep, Dependency.class).getExclusions();
            var expected = Set.of();
            // not just equals, needs to be the same singleton
            assertSame(expected, actual);
        }
    }

    @Test
    public void testDependency_jarType() {
        var d = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")), Set.of(new Exclusion("g2", "a2")),
                Scope.TEST, true, "jar", "sources");
        var json = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"a\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":true}";
        test(d, json);
    }

    @Test
    public void testDependency_noClassifier() {
        var d = new Dependency("g1", "a1", Set.of(new VersionConstraint("[1,2]")), Set.of(new Exclusion("g2", "a2")),
                Scope.TEST, true, "type", "");
        var json = "{\"v\":[\"[1,2]\"],\"g\":\"g1\",\"scope\":\"test\",\"a\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":true,\"type\":\"type\"}";
        test(d, json);
    }

    @Test
    public void testDependencyFixNonEmptyVersionConstraints() throws Exception {
        var json = "{\"v\":[\"\"],\"g\":\"g1\",\"scope\":\"test\",\"classifier\":\"sources\",\"a\":\"a1\",\"exclusions\":[\"g2:a2\"],\"optional\":true,\"type\":\"type\"}";
        var out = om.readValue(json, Dependency.class);
        assertTrue(out.getVersionConstraints().isEmpty());
    }

    @Test
    public void testVersionConstraint() {
        var vc = new VersionConstraint("[1,2]");
        test(vc, "\"[1,2]\"");
    }

    @Test
    public void testExclusion() {
        var e = new Exclusion("gid", "aid");
        test(e, "\"gid:aid\"");
    }

    @Test
    public void testHashCodeDependency() {
        assertHashCodeAfterSerialization(new Dependency("g", "a", "v"));
    }

    @Test
    public void testHashCodePom() {
        var pb = new PomBuilder();
        pb.artifactId = "a";
        pb.groupId = "g";
        assertHashCodeAfterSerialization(pb.pom());
    }

    @Test
    public void testHashCodeExclusion() {
        assertHashCodeAfterSerialization(new Exclusion("gid", "aid"));
    }

    @Test
    public void testHashCodeVersionConstraint() {
        assertHashCodeAfterSerialization(new VersionConstraint("1.2.3"));
    }

    private void assertHashCodeAfterSerialization(Object in) {
        try {
            assertNotEquals(0, in.hashCode());
            var json = om.writeValueAsString(in);
            var out = om.readValue(json, in.getClass());
            assertEquals(in.hashCode(), out.hashCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDefaultArtifactVersion() {
        test(new DefaultArtifactVersion("1.2.3"), "\"1.2.3\"");
        test(new DefaultArtifactVersion("2.3.4-classifier"), "\"2.3.4-classifier\"");
        test(new DefaultArtifactVersion("3"), "\"3\"");
        test(new DefaultArtifactVersion("4.5"), "\"4.5\"");
    }

    private void test(Object in, String expectedJson) {
        try {
            assertNotEquals(0, in.hashCode());
            var json = om.writeValueAsString(in);
            assertJsonEquals(expectedJson, json);
            var out = om.readValue(json, in.getClass());
            assertEquals(in, out);
            assertEquals(in.hashCode(), out.hashCode());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertJsonEquals(String expectedJson, String actualJson) {
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT);
    }
}