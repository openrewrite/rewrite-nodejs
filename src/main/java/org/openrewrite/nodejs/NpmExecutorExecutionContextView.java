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

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

@SuppressWarnings("unused")
public class NpmExecutorExecutionContextView extends DelegatingExecutionContext {
    private static final String NPM_EXECUTOR = "org.openrewrite.nodejs.npmExecutor";

    public NpmExecutorExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static NpmExecutorExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof NpmExecutorExecutionContextView) {
            return (NpmExecutorExecutionContextView) ctx;
        }
        return new NpmExecutorExecutionContextView(ctx);
    }

    public NpmExecutorExecutionContextView setNpmExecutor(NpmExecutor npmExecutor) {
        putMessage(NPM_EXECUTOR, npmExecutor);
        return this;
    }

    public NpmExecutor getNpmExecutor() {
        return getMessage(NPM_EXECUTOR, new NpmExecutor());
    }
}
