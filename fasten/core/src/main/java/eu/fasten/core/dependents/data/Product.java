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

import java.util.Objects;

/**
 * A versionless Product
 */
public class Product {

    public long id;
    public String packageName;

    public Product() {
    }

    public Product(final String packageName) {
        this.id = 0;
        this.packageName = packageName;
    }

    public Product(long id, String packageName) {
        this.id = id;
        this.packageName = packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product that = (Product) o;
        return packageName.equals(that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, packageName);
    }

    @Override
    public String toString() {
        return packageName;
    }
}
