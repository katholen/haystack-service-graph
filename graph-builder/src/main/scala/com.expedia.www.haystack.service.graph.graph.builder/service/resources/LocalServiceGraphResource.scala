/*
 *
 *     Copyright 2018 Expedia, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */
package com.expedia.www.haystack.service.graph.graph.builder.service.resources

import com.expedia.www.haystack.service.graph.graph.builder.model.ServiceGraph
import com.expedia.www.haystack.service.graph.graph.builder.service.fetchers.LocalEdgesFetcher

class LocalServiceGraphResource(localEdgesFetcher: LocalEdgesFetcher) extends Resource("servicegraph.local") {
  private val edgeCount = metricRegistry.histogram("servicegraph.local.edges")

  protected override def get(): ServiceGraph = {
    val localGraph = ServiceGraph(localEdgesFetcher.fetchEdges())
    edgeCount.update(localGraph.edges.length)
    localGraph
  }
}
