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

package eu.fasten.core.dependents.data;

import eu.fasten.core.data.Constants;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.json.JSONObject;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

public class Revision extends Product implements Serializable {

    public DefaultArtifactVersion version;
    public Timestamp createdAt;

    public Revision() {
    }

    public Revision(final String packageName, final String version, final Timestamp createdAt) {
        super(packageName);

        this.version = new DefaultArtifactVersion(version);
        this.createdAt = createdAt;
    }

    public Revision(final long id, final String packageName,
                    final String version, final Timestamp createdAt) {
        super(id, packageName);

        this.version = new DefaultArtifactVersion(version);
        this.createdAt = createdAt;
    }

    public Product product() {
        return new Product(this.packageName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Revision revision = (Revision) o;
        return version.equals(revision.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageName, version);
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", packageName, Constants.mvnCoordinateSeparator, version);
    }

    public JSONObject toJSON() {
        var json = new JSONObject();
        json.put("id", id);
        json.put("package", packageName);
        json.put("version", version.toString());
        json.put("createdAt", createdAt.getTime());
        return json;
    }
}
