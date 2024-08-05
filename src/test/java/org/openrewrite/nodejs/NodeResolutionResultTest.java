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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.nodejs.search.IsPackageLockJson;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NodeResolutionResultTest implements RewriteTest {

    @Test
    @DocumentExample
    void invalidVersionsShouldNotBreakResolution() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> Preconditions.check(
            new IsPackageLockJson<>(),
            new JsonIsoVisitor<>() {
                @Override
                public Json.Document visitDocument(Json.Document document, ExecutionContext ctx) {
                    NodeResolutionResult nodeResolutionResult = NodeResolutionResult.fromPackageLockJson(document);
                    assertThat(nodeResolutionResult.getDependencies()).singleElement()
                      .satisfies(dependency -> assertThat(dependency.getName()).isEqualTo("rxfire"));
                    // rxjs version is invalid, but it should not break resolution
                    return document;
                }
            }
          ))),
          json(
            //language=json
            """
              {
                "name": "nebular",
                "version": "13.0.0",
                "license": "MIT",
                "author": "akveo <contact@akveo.com>",
                "private": true,
                "bugs": {
                  "url": "https://github.com/akveo/nebular/issues"
                },
                "homepage": "https://github.com/akveo/nebular#readme",
                "repository": "git+https://github.com/akveo/nebular.git",
                "dependencies": {
                  "rxfire": "^6.0.0",
                  "rxjs": "^6.5.3 || ^7.4.0"
                }
              }
              """,
            spec -> spec.path("package.json")
          ),
          json(
            //language=json
            """
              {
                "name": "nebular",
                "version": "13.0.0",
                "lockfileVersion": 2,
                "requires": true,
                "packages": {
                  "": {
                    "name": "nebular",
                    "version": "13.0.0",
                    "hasInstallScript": true,
                    "license": "MIT",
                    "dependencies": {
                      "rxfire": "^6.0.0",
                      "rxjs": "^6.5.3 || ^7.4.0"
                    }
                  },
                  "node_modules/rxfire": {
                    "version": "6.0.5",
                    "resolved": "https://registry.npmjs.org/rxfire/-/rxfire-6.0.5.tgz",
                    "integrity": "sha512-ycBsANGbya3GNtOBKzZVATLEV+0S9gUrlTfwnN15TCXtgG8OgIMAuv2k9+kMeVaevp/DRp1KT+vYf6Wkop6gvw==",
                    "peerDependencies": {
                      "rxjs": "^6.0.0 || ^7.0.0"
                    }
                  },
                  "node_modules/rxjs": {
                    "version": "6.6.7",
                    "resolved": "https://registry.npmjs.org/rxjs/-/rxjs-6.6.7.tgz",
                    "integrity": "sha512-hTdwr+7yYNIT5n4AMYp85KA6yw2Va0FLa3Rguvbpa4W3I5xynaBZo41cM3XM+4Q6fRMj3sBYIR1VAmZMXYJvRQ==",
                    "dependencies": {
                      "tslib": "^1.9.0"
                    },
                    "engines": {
                      "npm": ">=2.0.0"
                    }
                  }
                }
              }
              """,
            spec -> spec.path("package-lock.json")
          )
        );
    }
}
