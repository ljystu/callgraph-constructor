/*
 * Copyright 2021 Delft University of Technology
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
package eu.fasten.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssertsTest {

    @Test
    public void assertNotNull_fail() {
        assertThrows(IllegalStateException.class, () -> {
            Asserts.assertNotNull(null);
        });
    }

    @Test
    public void assertNotNull_ok() {
        Asserts.assertNotNull("");
    }

    @Test
    public void assertContains_fail() {
        assertThrows(IllegalStateException.class, () -> {
            Asserts.assertContains(new String[]{"a", "b", "c"}, "x");
        });
    }

    @Test
    public void assertContains_ok() {
        Asserts.assertContains(new String[]{"a", "b", "c"}, "a");
    }

    @Test
    public void assertContains_okForNull() {
        Asserts.assertContains(new String[]{"a", null}, null);
    }

    @Test
    public void assertNotNullOrEmpty_failNull() {
        assertThrows(IllegalStateException.class, () -> {
            Asserts.assertNotNullOrEmpty(null);
        });
    }

    @Test
    public void assertNotNullOrEmpty_failEmpty() {
        assertThrows(IllegalStateException.class, () -> {
            Asserts.assertNotNullOrEmpty("");
        });
    }

    @Test
    public void assertNotNullOrEmpty_ok() {
        Asserts.assertNotNullOrEmpty("...");
    }

    @Test
    public void assertTrue_okStd() {
        Asserts.assertTrue(true);
    }

    @Test
    public void assertTrue_okCustom() {
        Asserts.assertTrue(true, "...");
    }

    @Test
    public void assertTrue_failStd() {
        var e = assertThrows(IllegalStateException.class, () -> {
            Asserts.assertTrue(false);
        });
        assertEquals("Expected condition not met.", e.getMessage());
    }

    @Test
    public void assertTrue_failCustom() {
        var e = assertThrows(IllegalStateException.class, () -> {
            Asserts.assertTrue(false, "abc");
        });
        assertEquals("abc", e.getMessage());
    }
}