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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import eu.fasten.core.data.Constants;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A dependency declaration. Denotes a Revision's will to use the functionality of the
 * {@class Product} that matches the dependency's qualifiers.
 */
public class Dependency extends Product {
    public static final Dependency empty = new Dependency("", "");

    public final List<VersionConstraint> versionConstraints;

    /**
     * Constructor for Dependency object.
     *
     * @param packageName
     * @param versionConstraints List of version constraints of the dependency
     */
    public Dependency(final String packageName,
                      final List<VersionConstraint> versionConstraints) {
        super(packageName);
        this.versionConstraints = versionConstraints;
    }

    public Dependency(final String packageName, final String version) {
        this(packageName, VersionConstraint.resolveMultipleVersionConstraints(version));
    }

    public Product product() {
        return new Product(packageName);
    }

    /**
     * Turns list of version constraints into string array of specifications.
     *
     * @return String array representation of the dependency version constraints
     */
    public String[] getVersionConstraints() {
        var constraints = new String[this.versionConstraints.size()];
        for (int i = 0; i < versionConstraints.size(); i++) {
            constraints[i] = versionConstraints.get(i).toString();
        }
        return constraints;
    }

    /**
     * Converts Dependency object into JSON.
     *
     * @return JSONObject representation of dependency
     */
    public JSONObject toJSON() {
        final var json = new JSONObject();
        json.put("package", this.packageName);
        final var constraintsJson = new JSONArray();
        for (var constraint : this.versionConstraints) {
            constraintsJson.put(constraint.toJSON());
        }
        json.put("versionConstraints", constraintsJson);       
        return json;
    }


    public String getPackageName() {
        return this.packageName;
    }

    public String getVersion() {
        return String.join(",", this.getVersionConstraints());
    }

    public String toCanonicalForm() {
        var builder = new StringBuilder();
        builder.append(this.packageName);
        builder.append(Constants.mvnCoordinateSeparator);
        builder.append(this.getVersion());
        return builder.toString();
    }

    public String toMavenCoordinate() {
        return this.packageName +
                Constants.mvnCoordinateSeparator +
                this.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Dependency that = (Dependency) o;
        if (!packageName.equals(that.packageName)) {
            return false;
        }
        return versionConstraints.equals(that.versionConstraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageName, this.getVersion());
    }

    @Override
    public String toString() {
        return toCanonicalForm();
    }

    /**
     * Creates a Dependency object from JSON.
     *
     * @param json JSONObject representation of dependency
     * @return Dependency object
     */
    public static Dependency fromJSON(JSONObject json) {
        var packageName = json.getString("package");
        var versionConstraints = new ArrayList<VersionConstraint>();
        if (json.has("versionConstraints")) {
            var constraintsJson = json.getJSONArray("versionConstraints");
            for (var i = 0; i < constraintsJson.length(); i++) {
                versionConstraints.add(VersionConstraint.fromJSON(constraintsJson.getJSONObject(i)));
            }
        }
        return new Dependency(packageName, versionConstraints);
    }


    public static class VersionConstraint {

        public final String lowerBound;
        public final boolean isLowerHardRequirement;
        public final String upperBound;
        public final boolean isUpperHardRequirement;

        /**
         * Constructor for VersionConstraint object.
         *
         * @param lowerBound             Lower bound on the version range
         * @param isLowerHardRequirement Is lower bound a hard requirement
         * @param upperBound             Upper bound on the version range
         * @param isUpperHardRequirement Is upper bound a hard requirement
         */
        public VersionConstraint(final String lowerBound, final boolean isLowerHardRequirement,
                                 final String upperBound, final boolean isUpperHardRequirement) {
            this.lowerBound = lowerBound;
            this.isLowerHardRequirement = isLowerHardRequirement;
            this.upperBound = upperBound;
            this.isUpperHardRequirement = isUpperHardRequirement;
        }

        /**
         * Constructs a VersionConstraint object from specification.
         * (From https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification)
         *
         * @param spec String specification of version constraint
         */
        public VersionConstraint(final String spec) {
            this.isLowerHardRequirement = spec.startsWith("[");
            this.isUpperHardRequirement = spec.endsWith("]");
            if (!spec.contains(",")) {
                var version = spec;
                if (version.startsWith("[") && version.endsWith("]")) {
                    version = version.substring(1, spec.length() - 1);
                }
                this.upperBound = version;
                this.lowerBound = version;

            } else {
                final var versionSplit = startsAndEndsWithBracket(spec)
                        ? spec.substring(1, spec.length() - 1).split(",")
                        : spec.split(",");
                this.lowerBound = versionSplit[0];
                this.upperBound = (versionSplit.length > 1) ? versionSplit[1] : "";
            }
        }

        private boolean startsAndEndsWithBracket(String str) {
            return (str.startsWith("(") || str.startsWith("["))
                    && (str.endsWith(")") || str.endsWith("]"));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VersionConstraint that = (VersionConstraint) o;
            if (isLowerHardRequirement != that.isLowerHardRequirement) {
                return false;
            }
            if (isUpperHardRequirement != that.isUpperHardRequirement) {
                return false;
            }
            if (!lowerBound.equals(that.lowerBound)) {
                return false;
            }
            return upperBound.equals(that.upperBound);
        }

        /**
         * Turns version constraint back into string specification.
         *
         * @return String representation of the version constraint
         */
        @Override
        public String toString() {
            var constraintBuilder = new StringBuilder();
            if (this.lowerBound.equals(this.upperBound)) {
                if (this.isLowerHardRequirement && this.isUpperHardRequirement) {
                    constraintBuilder.append("[");
                    constraintBuilder.append(this.lowerBound);
                    constraintBuilder.append("]");
                } else {
                    constraintBuilder.append(this.lowerBound);
                }
            } else {
                if (this.isLowerHardRequirement) {
                    constraintBuilder.append("[");
                } else {
                    constraintBuilder.append("(");
                }
                constraintBuilder.append(this.lowerBound);
                constraintBuilder.append(",");
                constraintBuilder.append(this.upperBound);
                if (this.isUpperHardRequirement) {
                    constraintBuilder.append("]");
                } else {
                    constraintBuilder.append(")");
                }
            }
            return constraintBuilder.toString();
        }

        /**
         * Converts VersionConstraint object into JSON.
         *
         * @return JSONObject representation of version constraint
         */
        public JSONObject toJSON() {
            var json = new JSONObject();
            json.put("lowerBound", this.lowerBound);
            json.put("isLowerHardRequirement", this.isLowerHardRequirement);
            json.put("upperBound", this.upperBound);
            json.put("isUpperHardRequirement", this.isUpperHardRequirement);
            return json;
        }

        /**
         * Creates a VersionConstraint object from JSON.
         *
         * @param json JSONObject representation of version constraint
         * @return VersionConstraint object
         */
        public static VersionConstraint fromJSON(JSONObject json) {
            var lowerBound = json.getString("lowerBound");
            var upperBound = json.getString("upperBound");
            var isLowerHardRequirement = json.getBoolean("isLowerHardRequirement");
            var isUpperHardRequirement = json.getBoolean("isUpperHardRequirement");
            return new VersionConstraint(lowerBound, isLowerHardRequirement,
                    upperBound, isUpperHardRequirement);
        }

        /**
         * Creates full list of version constraints from specification.
         * (From https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification)
         *
         * @param spec String specification of version constraints
         * @return List of Version Constraints
         */
        public static List<VersionConstraint> resolveMultipleVersionConstraints(String spec) {
            if (spec == null) {
                return List.of(new VersionConstraint("*"));
            }
            if (spec.startsWith("$")) {
                return List.of(new VersionConstraint(spec));
            }
            final var versionRangesCount = (StringUtils.countMatches(spec, ",") + 1) / 2;
            var versionConstraints = new ArrayList<VersionConstraint>(versionRangesCount);
            int count = 0;
            for (int i = 0; i < spec.length(); i++) {
                if (spec.charAt(i) == ',') {
                    count++;
                    if (count % 2 == 0) {
                        var specBuilder = new StringBuilder(spec);
                        specBuilder.setCharAt(i, ';');
                        spec = specBuilder.toString();
                    }
                }
            }
            var versionRanges = spec.split(";");
            for (var versionRange : versionRanges) {
                versionConstraints.add(new VersionConstraint(versionRange));
            }
            return versionConstraints;
        }
    }
}