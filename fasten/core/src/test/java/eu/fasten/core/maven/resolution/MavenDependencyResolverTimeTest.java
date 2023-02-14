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

import eu.fasten.core.maven.data.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MavenDependencyResolverTimeTest extends AbstractMavenDependencyResolverTest {

    @Test
    public void failsWhenSinglePomCannotBeFound() {
        sut.setData(mock(MavenResolverData.class));
        var e = assertThrows(MavenResolutionException.class, () -> {
            sut.resolve(Set.of("a:a:1"), config);
        });
        assertEquals("Cannot find coordinate a:a:1", e.getMessage());
    }

    @Test
    public void checkThatConfigFieldIsUsedForSinglePoms() {
        var resolveAt = 1;

        mockDepGraph();
        addMockData("a", "b", "1", resolveAt, getPom("a", "b", "1", 0));

        resolve(resolveAt, "a:b:1");
        verify(data).find(new GA("a", "b"), Set.of(new VersionConstraint("1")), resolveAt);
    }

    @Test
    public void resolutionOfMultiPomsDoesNotFail() {
        mockDepGraph();
        resolve(10L, "a:b:1", "b:c:2");
    }

    @Test
    public void resolutionOfDependenciesPropagatesTimestamp() {
        var resolveAt = 10L;
        mockDepGraph();
        resolve(resolveAt, "a:b:1", "b:c:2");
        verify(data).find(eq(new GA("a", "b")), anySet(), eq(resolveAt));
        verify(data).find(eq(new GA("b", "c")), anySet(), eq(resolveAt));
    }

    @Test
    public void failIfFoundSinglePomIsTooNew() {
        var resolveAt = 0;

        mockDepGraph();
        addMockData("a", "b", "1", resolveAt, getPom("a", "b", "1", 1));

        var e = assertThrows(MavenResolutionException.class, () -> {
            resolve(resolveAt, "a:b:1");
        });
        assertEquals("Requested POM has been released after resolution timestamp", e.getMessage());
    }

    @Test
    public void basic() {
        add(1, "a:1", "b:1");
        add(2, "b:1");
        assertThrows(MavenResolutionException.class, () -> {
            assertResolutionAt(0, "a:1");
        });
        assertResolutionAt(1, "a:1");
        assertResolutionAt(2, "a:1", "b:1");
    }

    @Test
    public void version() {
        add(1, "a:1", "b:[1,2]");
        add(2, "b:1.1");
        add(3, "b:1.2");
        add(4, "b:1.3");
        assertResolutionAt(1, "a:1");
        assertResolutionAt(2, "a:1", "b:1.1");
        assertResolutionAt(3, "a:1", "b:1.2");
        assertResolutionAt(4, "a:1", "b:1.3");
    }

    private void add(long releaseDate, String from, String... tos) {
        var pb = new PomBuilder();
        var parts = from.split(":");
        pb.groupId = parts[0];
        pb.artifactId = parts[0];
        pb.version = parts[1];
        pb.releaseDate = releaseDate;

        for (var to : tos) {
            var partsTo = to.split(":");
            var d = new Dependency(partsTo[0], partsTo[0], partsTo[1]);
            pb.dependencies.add(d);
        }

        data.add(pb.pom());
    }

    private void assertResolutionAt(long resolveAt, String... deps) {
        config.resolveAt = resolveAt;
        assertResolution(deps);
    }

    private static Pom getPom(String g, String a, String v, long releaseDate) {
        var pb = new PomBuilder();
        pb.groupId = g;
        pb.artifactId = a;
        pb.version = v;
        pb.releaseDate = releaseDate;
        return pb.pom();
    }

    private void mockDepGraph() {
        data = mock(MavenResolverData.class);
        sut.setData(data);
    }

    private void addMockData(String g, String a, String v, long resolveAt, Pom pom) {
        var ga = new GA(g, a);
        when(data.find(ga, Set.of(new VersionConstraint(v)), resolveAt)).thenReturn(pom);
    }

    private void resolve(long resolveAt, String... coords) {
        config.resolveAt = resolveAt;
        sut.resolve(Set.of(coords), config);
    }
}