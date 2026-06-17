/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.build.architecture.objects.noRequireNonNull

import org.springframework.util.Assert
import java.util.function.Consumer

internal class NoRequireNonNull {
    fun exampleMethod() {
        Assert.notNull(Any(), "Object must not be null")
        // Compilation of a method reference generates code that uses
        // Objects.requireNonNull(Object). Check that it doesn't cause a failure.
        mutableListOf<Any?>().forEach(Consumer { x: Any? -> println(x) })
    }
}
