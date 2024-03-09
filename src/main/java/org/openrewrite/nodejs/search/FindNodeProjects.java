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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.JsonPathMatcher;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.nodejs.table.NodeProjects;

import java.util.concurrent.atomic.AtomicBoolean;

public class FindNodeProjects extends ScanningRecipe<AtomicBoolean> {
    private final transient NodeProjects nodeProjects = new NodeProjects(this);

    @Override
    public String getDisplayName() {
        return "Find Node.js projects";
    }

    @Override
    public String getDescription() {
        return "Find Node.js projects and summarize data about them.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean hasPackageLock) {
        return Preconditions.check(new IsPackageLockJson<>(), new JsonVisitor<ExecutionContext>() {
            @Override
            public Json visitDocument(Json.Document document, ExecutionContext ctx) {
                hasPackageLock.set(true);
                return document;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean hasPackageLock) {
        JsonPathMatcher name = new JsonPathMatcher("$.name");
        JsonPathMatcher version = new JsonPathMatcher("$.version");
        return Preconditions.check(new IsPackageJson<>(), new JsonVisitor<ExecutionContext>() {
            @Override
            public Json visitDocument(Json.Document document, ExecutionContext ctx) {
                hasPackageLock.set(true);
                super.visitDocument(document, ctx);
                nodeProjects.insertRow(ctx, new NodeProjects.Row(
                        getCursor().getMessage("name", ""),
                        getCursor().getMessage("version", ""),
                        hasPackageLock.get()
                ));
                return SearchResult.found(document);
            }

            @Override
            public Json visitMember(Json.Member member, ExecutionContext ctx) {
                if (name.matches(getCursor())) {
                    String name = ((Json.Literal) member.getValue()).toString();
                    getCursor().putMessageOnFirstEnclosing(Json.Document.class, "name", name);
                }
                if (version.matches(getCursor())) {
                    String version = ((Json.Literal) member.getValue()).toString();
                    getCursor().putMessageOnFirstEnclosing(Json.Document.class, "version", version);
                }
                return member;
            }
        });
    }
}
