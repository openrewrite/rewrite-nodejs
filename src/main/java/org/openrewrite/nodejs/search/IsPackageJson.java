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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class IsPackageJson<P> extends TreeVisitor<Tree, P> {
    @Override
    public Tree visit(@Nullable Tree tree, P p) {
        if (tree instanceof Json.Document) {
            Json.Document cu = (Json.Document) requireNonNull(tree);
            if (matches(cu.getSourcePath())) {
                return SearchResult.found(cu);
            }
        }
        return super.visit(tree, p);
    }

    public static boolean matches(Path sourcePath) {
        return sourcePath.toFile().getName().equals("package.json");
    }
}
