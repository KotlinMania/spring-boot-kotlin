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
package org.springframework.boot.build.cycledetection

import org.gradle.kotlin.dsl.*

import org.gradle.api.*
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.initialization.Settings
import org.jgrapht.Graph
import org.jgrapht.alg.cycle.TarjanSimpleCycles
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.lang.String
import java.util.*
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A [Settings] [plugin][Plugin] to detect cycles between a build's projects.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class CycleDetectionPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.getGradle().getTaskGraph()
            .whenReady { taskGraph: TaskExecutionGraph -> this.detectCycles(taskGraph) }
    }

    private fun detectCycles(taskGraph: TaskExecutionGraph) {
        val dependenciesByProject = getProjectsAndDependencies(taskGraph)
        assertNoCycles(
            createGraph(dependenciesByProject, Function { obj: Project? -> obj!!.getPath() }),
            "Project cycles detected:\n"
        )
        assertNoCycles(
            createGraph(dependenciesByProject, Function { project1: Project? -> this.getLayer(project1!!) }),
            "Layer cycles detected:\n"
        )
    }

    private fun getProjectsAndDependencies(taskGraph: TaskExecutionGraph): MutableMap<Project?, MutableSet<Project?>> {
        val dependenciesByProject: MutableMap<Project?, MutableSet<Project?>> =
            HashMap<Project?, MutableSet<Project?>>()
        for (task in taskGraph.getAllTasks()) {
            val project = task.project
            val dependencies =
                dependenciesByProject.computeIfAbsent(project) { key: Project? -> LinkedHashSet<Project?>() }
            taskGraph.getDependencies(task)
                .stream()
                .map<Project> { obj: Task? -> obj!!.project }
                .filter { taskProject: Project? -> taskProject != project }
                .forEach { e: Project? -> dependencies.add(e) }
        }
        return dependenciesByProject
    }

    private fun createGraph(
        dependenciesByProject: MutableMap<Project?, MutableSet<Project?>>,
        vertexExtractor: Function<Project?, String?>?
    ): Graph<String?, DefaultEdge?> {
        val graph: Graph<String?, DefaultEdge?> = DefaultDirectedGraph<String?, DefaultEdge?>(DefaultEdge::class.java)
        dependenciesByProject.keys.stream().map<String>(vertexExtractor)
            .filter { obj: String? -> Objects.nonNull(obj) }.forEach { v: String? -> graph.addVertex(v) }
        dependenciesByProject.forEach { (project: Project?, dependencies: MutableSet<Project?>?) ->
            val source = vertexExtractor!!.apply(project)
            dependencies!!.stream().map<String>(vertexExtractor).filter { obj: String? -> Objects.nonNull(obj) }
                .forEach { target: String? ->
                    if (source != null && source != target) {
                        graph.addEdge(source, target)
                    }
                }
        }
        return graph
    }

    private fun getLayer(project1: Project): String? {
        val matcher: Matcher = layerPattern.matcher(project1.getPath())
        return if (matcher.matches()) matcher.group(1) else null
    }

    private fun assertNoCycles(projects: Graph<String?, DefaultEdge?>, str: String) {
        val cycles = TarjanSimpleCycles<String?, DefaultEdge?>(projects).findSimpleCycles()
        if (!cycles.isEmpty()) {
            val message = StringBuilder(str)
            for (cycle in cycles) {
                cycle.add(cycle.get(0))
                message.append("  " + String.join(" -> ", cycle) + "\n")
            }
            throw GradleException(message.toString())
        }
    }

    companion object {
        private val layerPattern: Pattern = Pattern.compile("^:(.+?):.*")
    }
}
