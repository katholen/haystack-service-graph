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
package com.expedia.www.haystack.service.graph.graph.builder.app

import com.expedia.www.haystack.service.graph.graph.builder.model.ServiceGraph
import com.expedia.www.haystack.service.graph.graph.builder.writer.ServiceGraphWriter
import org.apache.kafka.streams.processor.{Processor, ProcessorContext, ProcessorSupplier}

class ServiceGraphSinkSupplier(writer: ServiceGraphWriter) extends ProcessorSupplier[String, ServiceGraph] {
  override def get(): Processor[String, ServiceGraph] = new ServiceGraphSink(writer)
}

class ServiceGraphSink(writer: ServiceGraphWriter) extends Processor[String, ServiceGraph] {
  private var context: ProcessorContext = _

  override def init(context: ProcessorContext): Unit = {
    this.context = context
  }

  override def process(key: String, serviceGraph: ServiceGraph): Unit = {
    writer.write(context.taskId.toString, serviceGraph)
    context.commit()
  }

  override def punctuate(timestamp: Long): Unit = {}

  override def close(): Unit = {}
}
