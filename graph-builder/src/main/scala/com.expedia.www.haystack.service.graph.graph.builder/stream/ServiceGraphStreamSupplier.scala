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
package com.expedia.www.haystack.service.graph.graph.builder.stream

import java.util.function.Supplier

import com.expedia.www.haystack.commons.entities.GraphEdge
import com.expedia.www.haystack.commons.kstreams.serde.graph.GraphEdgeSerde
import com.expedia.www.haystack.service.graph.graph.builder.config.entities.KafkaConfiguration
import com.expedia.www.haystack.service.graph.graph.builder.model.{EdgeStats, EdgeStatsSerde}
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.{Consumed, StreamsBuilder, Topology}

class ServiceGraphStreamSupplier(kafkaConfiguration: KafkaConfiguration) extends Supplier[Topology] {
  override def get(): Topology = initialize(new StreamsBuilder)

  private def initialize(builder: StreamsBuilder): Topology = {

    builder
      //
      // read edges from graph-nodes topic
      // graphEdge is both the key and value
      // use wallclock time
      .stream(
        kafkaConfiguration.consumerTopic,
        Consumed.`with`(
          new GraphEdgeSerde,
          new GraphEdgeSerde,
          new WallclockTimestampExtractor,
          kafkaConfiguration.autoOffsetReset
        )
      )
      //
      // group by key for doing aggregations on edges
      // this will not cause any repartition
      .groupByKey(
        Serialized.`with`(new GraphEdgeSerde, new GraphEdgeSerde)
      )
      //
      // calculate stats for edges
      // keep the resulting ktable as materialized view in memory
      // enabled logging to persist ktable changelog topic and replicated to multiple brokers
      .aggregate[EdgeStats](
        () => EdgeStats(0, 0),
        edgeStatsAggregator,
        Materialized
          .as(Stores.inMemoryKeyValueStore(kafkaConfiguration.producerTopic))
          .withKeySerde(new GraphEdgeSerde)
          .withValueSerde(new EdgeStatsSerde)
          .withCachingEnabled()
          .withLoggingEnabled(kafkaConfiguration.producerTopicConfig)
      )

    // build stream topology and return
    builder.build()
  }

  // TODO find out why scala is not able to infer types of direct lambda for aggregator
  private val edgeStatsAggregator = new Aggregator[GraphEdge, GraphEdge, EdgeStats] {
    override def apply(key: GraphEdge, value: GraphEdge, aggregate: EdgeStats): EdgeStats =
      EdgeStats(aggregate.count + 1, System.currentTimeMillis())
  }
}
