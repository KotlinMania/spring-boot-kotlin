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
package org.springframework.boot.build

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.IConventionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.Facet
import java.util.concurrent.Callable

/**
 * Conventions that are applied in the presence of the {WarPlugin}. When the plugin is
 * applied:
 * 
 *  * Update Eclipse WTP Plugin facets to use Servlet 5.0
 * 
 * 
 * @author Phillip Webb
 */
class WarConventions {
    fun apply(project: Project) {
        project.plugins
            .withType<EclipseWtpPlugin>(EclipseWtpPlugin::class.java) { wtp: EclipseWtpPlugin ->
                project.getTasks().getByName(EclipseWtpPlugin.ECLIPSE_WTP_FACET_TASK_NAME).doFirst(
                    Action { task: Task ->
                        val eclipseModel = project.getExtensions().getByType<EclipseModel>(EclipseModel::class.java)
                        (eclipseModel.getWtp().getFacet() as IConventionAware).getConventionMapping()
                            .map("facets", Callable { getFacets(project) })
                    })
            }
    }

    private fun getFacets(project: Project): MutableList<Facet?> {
        val javaVersion: JavaVersion =
            project.getExtensions().getByType<JavaPluginExtension>(JavaPluginExtension::class.java)
                .sourceCompatibility
        val facets: MutableList<Facet?> = ArrayList<Facet?>()
        facets.add(Facet(Facet.FacetType.fixed, "jst.web", null))
        facets.add(Facet(Facet.FacetType.installed, "jst.web", "5.0"))
        facets.add(Facet(Facet.FacetType.installed, "jst.java", javaVersion.toString()))
        return facets
    }
}
