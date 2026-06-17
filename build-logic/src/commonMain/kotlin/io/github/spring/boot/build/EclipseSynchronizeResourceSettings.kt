/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.boot.build

import org.gradle.api.internal.PropertiesTransformer
import org.gradle.plugins.ide.api.PropertiesGeneratorTask
import java.util.*

/**
 * [Task] to synchronize Eclipse resource settings.
 * 
 * @author Phillip Webb
 */
abstract class EclipseSynchronizeResourceSettings :
    PropertiesGeneratorTask<EclipseSynchronizeResourceSettings.Configuration?>() {
    override fun create(): Configuration {
        return Configuration(getTransformer())
    }

    override fun configure(configuration: Configuration?) {
    }

    class Configuration constructor(transformer: PropertiesTransformer?) :
        EmptyPropertiesPersistableConfigurationObject(transformer) {
        override fun store(properties: Properties) {
            properties.put("encoding/<project>", "UTF-8")
        }
    }
}
