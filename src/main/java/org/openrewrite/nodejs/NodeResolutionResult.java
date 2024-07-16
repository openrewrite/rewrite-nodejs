/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.nodejs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.tree.Json;
import org.openrewrite.semver.Semver;

import java.util.*;

import static java.util.Collections.emptyList;

@Value
public class NodeResolutionResult {
    Collection<Dependency> dependencies;
    Collection<Dependency> devDependencies;

    public @Nullable Dependency getDependency(String name) {
        for (Dependency d : dependencies) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        for (Dependency d : devDependencies) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return null;
    }

    public static NodeResolutionResult fromPackageLockJson(Json.Document lockfileJson) {
        Collection<Dependency> dependencies = new ArrayList<>();
        Collection<Dependency> devDependencies = new ArrayList<>();
        try {
            Lockfile lock = JsonMapper.builder()
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                    .build()
                    .registerModule(new ParameterNamesModule())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(lockfileJson.printAll(), Lockfile.class);

            // multiple requested dependencies will all resolve to one version in the end
            Map<String, Collection<Dependency>> byName = new HashMap<>();

            lock.getPackages().forEach((pkgName, pkg) -> {
                boolean isRoot = "".equals(pkgName);
                List<Dependency> transitive;
                if (!isRoot) {
                    if (pkg.getDependencies() != null) {
                        transitive = new ArrayList<>(pkg.getDependencies().size());
                        pkg.getDependencies().forEach((name, version) -> {
                            Dependency d = new Dependency(name, Semver.validate(version, null).getValue(), null);
                            transitive.add(d);
                            byName.computeIfAbsent(name, n -> new ArrayList<>()).add(d);
                        });
                    } else {
                        transitive = emptyList();
                    }
                    String name = pkgName.replaceFirst("node_modules/", "");
                    ResolvedDependency resolved = new ResolvedDependency(
                            name,
                            pkg.getVersion(),
                            pkg.getLicense(),
                            transitive
                    );
                    byName.getOrDefault(name, emptyList()).forEach(d -> d.unsafeSetResolved(resolved));
                }
                if (pkg.getDependencies() != null) {
                    pkg.getDependencies().forEach((name, version) -> {
                        Dependency dep = new Dependency(name, Semver.validate(version, null).getValue(), null);
                        if (isRoot) {
                            dependencies.add(dep);
                        }
                        byName.computeIfAbsent(name, n -> new ArrayList<>()).add(dep);
                    });
                }
                if (pkg.getDevDependencies() != null) {
                    pkg.getDevDependencies().forEach((name, version) -> {
                        Dependency dep = new Dependency(name, Semver.validate(version, null).getValue(), null);
                        if (isRoot) {
                            devDependencies.add(dep);
                        }
                        byName.computeIfAbsent(name, n -> new ArrayList<>()).add(dep);
                    });
                }
            });
        } catch (
                JsonProcessingException ignored) {
        }
        return new

                NodeResolutionResult(dependencies, devDependencies);
    }

    @Value
    static class Lockfile {
        Map<String, Package> packages;

        @Value
        static class Package {
            String version;
            String license;
            Map<String, String> dependencies;
            Map<String, String> devDependencies;
        }
    }
}
