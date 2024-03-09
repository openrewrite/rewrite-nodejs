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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.JsonPathMatcher;
import org.openrewrite.json.tree.Json;
import org.openrewrite.nodejs.search.IsPackageJson;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends Recipe {

    @Option(displayName = "Name pattern",
            description = "Name glob pattern used to match dependencies",
            example = "@apollo*")
    String namePattern;

    @Option(displayName = "Version",
            description = "Set the version to upgrade to." +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used.",
            example = "1.x")
    String version;

    @Override
    public String getDisplayName() {
        return "Upgrade Node.js dependencies";
    }

    @Override
    public String getDescription() {
        return "Upgrade matching Node.js direct dependencies.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // NOTE that `npm upgrade <pkg>` will update the package-lock.json as well.
        // we might consider modifying the package.json with the new version and then
        // triggering that.

        JsonPathMatcher dependency = new JsonPathMatcher("$.dependencies");
        JsonPathMatcher devDependencies = new JsonPathMatcher("$.devDependencies");
        return Preconditions.check(new IsPackageJson<>(), new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member m = super.visitMember(member, ctx);
                Cursor maybeDependencies = getCursor().getParent(2);
                if (maybeDependencies != null && (dependency.matches(maybeDependencies) || devDependencies.matches(maybeDependencies))) {
                    String name = ((Json.Literal) member.getKey()).getValue().toString();
                    if (StringUtils.matchesGlob(name, namePattern)) {
                        Json.Literal versionLiteral = (Json.Literal) member.getValue();
                        String requestedVersion = versionLiteral.getValue().toString();
                        if (!requestedVersion.equals(version)) {
                            m = m.withValue(versionLiteral
                                    .withValue(version)
                                    .withSource("\"" + version + "\""));
                        }
                    }
                }
                return m;
            }
        });
    }
}
