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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class UpgradeDependencyVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void direct() {
        //noinspection JsonStandardCompliance
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("lodash*", "^4")),
          json(
            //language=json
            """
              {
                "name": "example",
                "version": "1.0.0",
                "dependencies": {
                  "jwt-decode": "^4.0.0",
                  "lodash.camelcase": "^4.3.0",
                  "lodash.kebabcase": "^4.1.0"
                }
              }
              """,
            //language=json
            """
              {
                "name": "example",
                "version": "1.0.0",
                "dependencies": {
                  "jwt-decode": "^4.0.0",
                  "lodash.camelcase": "^4",
                  "lodash.kebabcase": "^4"
                }
              }
              """,
            spec -> spec.path("package.json")
          ),
          json(
            //language=json
            """
              {
                "name": "example",
                "version": "1.0.0",
                "lockfileVersion": 3,
                "requires": true,
                "packages": {
                  "": {
                    "name": "example",
                    "version": "1.0.0",
                    "dependencies": {
                      "jwt-decode": "^4.0.0",
                      "lodash.camelcase": "^4.3.0",
                      "lodash.kebabcase": "^4.1.0"
                    }
                  },
                  "node_modules/jwt-decode": {
                    "version": "4.0.0",
                    "resolved": "https://registry.npmjs.org/jwt-decode/-/jwt-decode-4.0.0.tgz",
                    "integrity": "sha512-+KJGIyHgkGuIq3IEBNftfhW/LfWhXUIY6OmyVWjliu5KH1y0fw7VQ8YndE2O4qZdMSd9SqbnC8GOcZEy0Om7sA==",
                    "engines": {
                      "node": ">=18"
                    }
                  },
                  "node_modules/lodash.camelcase": {
                    "version": "4.3.0",
                    "resolved": "https://registry.npmjs.org/lodash.camelcase/-/lodash.camelcase-4.3.0.tgz",
                    "integrity": "sha512-TwuEnCnxbc3rAvhf/LbG7tJUDzhqXyFnv3dtzLOPgCG/hODL7WFnsbwktkD7yUV0RrreP/l1PALq/YSg6VvjlA=="
                  },
                  "node_modules/lodash.kebabcase": {
                    "version": "4.1.1",
                    "resolved": "https://registry.npmjs.org/lodash.kebabcase/-/lodash.kebabcase-4.1.1.tgz",
                    "integrity": "sha512-N8XRTIMMqqDgSy4VLKPnJ/+hpGZN+PHQiJnSenYqPaVV/NCqEogTnAdZLQiGKhxX+JCs8waWq2t1XHWKOmlY8g=="
                  }
                }
              }
              """,
            spec -> spec.path("package-lock.json")
          )
        );
    }
}
