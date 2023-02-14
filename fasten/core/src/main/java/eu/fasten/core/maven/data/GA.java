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
package eu.fasten.core.maven.data;

import eu.fasten.core.data.Constants;

public class GA {

    public final String groupId;
    public final String artifactId;

    private final int hashCode;

    public GA(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.hashCode = getHashCode();
    }

    private int getHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GA other = (GA) obj;
        if (hashCode != other.hashCode) {
            return false;
        }
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return groupId + Constants.mvnCoordinateSeparator + artifactId;
    }

    public static GA fromString(String product) throws IllegalArgumentException {
        var parts = product.split(Constants.mvnCoordinateSeparator);
        if (parts.length < 2) {
            throw new IllegalArgumentException(product + " is not a valid Maven groupId:artifactId string");
        } else {
            return new GA(parts[0], parts[1]);
        }
    }
}