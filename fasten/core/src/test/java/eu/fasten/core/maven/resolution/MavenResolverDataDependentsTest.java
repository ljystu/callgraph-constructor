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
package eu.fasten.core.maven.resolution;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.fasten.core.maven.data.Dependency;
import eu.fasten.core.maven.data.GA;
import eu.fasten.core.maven.data.GAV;
import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.maven.data.PomBuilder;

public class MavenResolverDataDependentsTest {

    private static final GAV GA1 = new GAV("g", "a", "1");

    private static final GA AB = new GA("a", "b");

    private static final long SOME_TIME = 1234;

    private MavenResolverData sut;

    @BeforeEach
    public void setup() {
        sut = new MavenResolverData();
    }

    @Test
    public void nonExistingPomIsNull() {
        var actual = sut.findPom(GA1, SOME_TIME);
        assertNull(actual);
    }

    @Test
    public void pomCanBeAdded() {
        var a = add(SOME_TIME, "g:a:1", "a:b:1");
        var b = sut.findPom(GA1, SOME_TIME);
        assertSame(a, b);
        var dpds = sut.findPotentialDependents(AB, SOME_TIME);
        assertEquals(1, dpds.size());
    }

    @Test
    public void pomAreNotAutomaticallyReplaced() {
        var a = add(SOME_TIME, "g:a:1", "a:b:1");
        var b = add(SOME_TIME + 1, "g:a:1", "a:b:1");
        var actual = sut.findPom(GA1, SOME_TIME + 1);
        assertSame(b, actual);
        var actuals = sut.findPotentialDependents(AB, SOME_TIME + 1);
        assertEquals(Set.of(a, b), actuals);
    }

    @Test
    public void pomsCanBeCleanedUp() {
        add(SOME_TIME, "g:a:1", "a:b:1");
        var b = add(SOME_TIME + 1, "g:a:1", "a:b:1");
        sut.removeOutdatedPomRegistrations();
        var actual = sut.findPom(GA1, SOME_TIME + 1);
        assertSame(b, actual);
        var actuals = sut.findPotentialDependents(AB, SOME_TIME + 1);
        assertEquals(Set.of(b), actuals);
    }

    @Test
    public void pomIsNotFoundWhenTooRecent() {
        add(SOME_TIME + 1, "g:a:1", "a:b:1");
        var actual = sut.findPom(GA1, SOME_TIME);
        assertNull(actual);
    }

    @Test
    public void findDependents() {
        var expected = add(SOME_TIME, "g:a:1", "a:b:1");
        var actuals = sut.findPotentialDependents(AB, SOME_TIME);
        assertEquals(Set.of(expected), actuals);
    }

    @Test
    public void findDependentsMultiple() {
        var e1 = add(SOME_TIME, "a:a:1", "t:t:1");
        var e2 = add(SOME_TIME, "b:b:1", "t:t:1");
        var actuals = sut.findPotentialDependents(new GA("t", "t"), SOME_TIME);
        assertEquals(Set.of(e1, e2), actuals);
    }

    @Test
    public void noInterferenceOfOtherDepdencies() {
        var e1 = add(SOME_TIME, "a:a:1", "t:t:1");
        add(SOME_TIME, "b:b:1", "u:u:1");
        var actuals = sut.findPotentialDependents(new GA("t", "t"), SOME_TIME);
        assertEquals(Set.of(e1), actuals);
    }

    @Test
    public void dependentsNotFoundWhenTooRecent() {
        add(SOME_TIME + 1, "a:a:1", "t:t:1");
        var actuals = sut.findPotentialDependents(new GA("t", "t"), SOME_TIME);
        assertEquals(Set.of(), actuals);
    }

    private Pom add(long releasedAt, String gav, String... deps) {
        var parts = gav.split(":");
        var pb = new PomBuilder();
        pb.groupId = parts[0];
        pb.artifactId = parts[1];
        pb.version = parts[2];
        pb.releaseDate = releasedAt;

        for (var depGAV : deps) {
            var depParts = depGAV.split(":");
            var dep = new Dependency(depParts[0], depParts[1], depParts[2]);
            pb.dependencies.add(dep);
        }

        var pom = pb.pom();
        sut.add(pom);
        return pom;
    }
}