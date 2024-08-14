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
package org.openrewrite.nodejs.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.JsonPathMatcher;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.nodejs.Dependency;
import org.openrewrite.nodejs.NodeResolutionResult;
import org.openrewrite.nodejs.table.DependenciesInUse;
import org.openrewrite.semver.Semver;

import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = false)
public class DependencyInsight extends ScanningRecipe<AtomicReference<NodeResolutionResult>> {
    transient DependenciesInUse dependenciesInUse = new DependenciesInUse(this);

    @Option(displayName = "Name pattern",
            description = "Name glob pattern used to match dependencies.",
            example = "@apollo*")
    String namePattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used." +
                          "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    @Override
    public String getDisplayName() {
        return "Node.js dependency insight";
    }

    @Override
    public String getDescription() {
        return "Identify the direct and transitive Node.js dependencies used in a project.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Override
    public AtomicReference<NodeResolutionResult> getInitialValue(ExecutionContext ctx) {
        return new AtomicReference<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicReference<NodeResolutionResult> resolution) {
        return Preconditions.check(new IsPackageLockJson<>(), new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Document visitDocument(Json.Document document, ExecutionContext ctx) {
                resolution.set(NodeResolutionResult.fromPackageLockJson(document));
                return document;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicReference<NodeResolutionResult> resolution) {
        JsonPathMatcher dependency = new JsonPathMatcher("$.dependencies");
        JsonPathMatcher devDependencies = new JsonPathMatcher("$.devDependencies");

        return Preconditions.check(new IsPackageJson<>(), new JsonVisitor<ExecutionContext>() {
            @Override
            public Json visitMember(Json.Member member, ExecutionContext ctx) {
                Json m = super.visitMember(member, ctx);
                Cursor maybeDependencies = getCursor().getParent(2);
                if (maybeDependencies != null && (dependency.matches(maybeDependencies) || devDependencies.matches(maybeDependencies))) {
                    String name = ((Json.Literal) member.getKey()).getValue().toString();
                    if (StringUtils.matchesGlob(name, namePattern)) {
                        String requestedVersion = ((Json.Literal) member.getValue()).getValue().toString();
                        Dependency dependency = resolution.get() == null ? null :
                                resolution.get().getDependency(name);
                        String resolvedVersion = dependency == null || dependency.getResolved() == null ?
                                "" : dependency.getResolved().getVersion();
                        dependenciesInUse.insertRow(ctx, new DependenciesInUse.Row(
                                name,
                                requestedVersion,
                                resolvedVersion
                        ));
                        m = SearchResult.found(m, resolvedVersion);
                    }
                }
                return m;
            }
        });
    }
}
