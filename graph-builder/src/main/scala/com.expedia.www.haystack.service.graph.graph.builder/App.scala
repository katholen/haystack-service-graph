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
package com.expedia.www.haystack.service.graph.graph.builder

import com.codahale.metrics.JmxReporter
import com.expedia.www.haystack.commons.health.{HealthController, HealthStatusController, UpdateHealthStatusFile}
import com.expedia.www.haystack.commons.kstreams.app.ManagedKafkaStreams
import com.expedia.www.haystack.commons.metrics.MetricsSupport
import com.expedia.www.haystack.service.graph.graph.builder.config.AppConfiguration
import com.expedia.www.haystack.service.graph.graph.builder.config.entities.{KafkaConfiguration, ServiceConfiguration}
import com.expedia.www.haystack.service.graph.graph.builder.service.fetchers.{LocalEdgesFetcher, RemoteEdgesFetcher}
import com.expedia.www.haystack.service.graph.graph.builder.service.resources.{GlobalServiceGraphResource, IsWorkingResource, LocalServiceGraphResource}
import com.expedia.www.haystack.service.graph.graph.builder.service.{HttpService, ManagedHttpService}
import com.expedia.www.haystack.service.graph.graph.builder.stream.{ServiceGraphStreamSupplier, StreamSupplier}
import com.netflix.servo.util.VisibleForTesting
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

/**
  * Starting point for graph-builder application
  */
object App extends MetricsSupport {
  private val LOGGER = LoggerFactory.getLogger(App.getClass)

  def main(args: Array[String]): Unit = {
    val appConfiguration = new AppConfiguration()

    // instantiate the application
    // if any exception occurs during instantiation
    // gracefully handles teardown and does system exit
    val app = createApp(appConfiguration)

    // start the application
    // if any exception occurs during startup
    // gracefully handles teardown and does system exit
    app.start()

    // add a shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        LOGGER.info("Shutdown hook is invoked, tearing down the application.")
        app.stop()
      }
    })

    // mark the status of app as 'healthy'
    HealthController.setHealthy()
  }

  @VisibleForTesting
  def createApp(appConfiguration: AppConfiguration): ManagedApplication = {
    val jmxReporter: JmxReporter = JmxReporter.forRegistry(metricRegistry).build()
    val healthStatusController = new HealthStatusController
    healthStatusController.addListener(new UpdateHealthStatusFile(appConfiguration.healthStatusFilePath))

    var stream: KafkaStreams = null
    var service: HttpService = null
    try {

      // build kafka stream to create service graph
      // it ingests graph edges and create service graph out of it
      // graphs are stored as materialized ktable in stream state store
      stream = createStream(appConfiguration.kafkaConfig, healthStatusController)

      // build http service to query current service graph
      // it performs interactive query on ktable
      service = createService(appConfiguration.serviceConfig, stream, appConfiguration.kafkaConfig.producerTopic)

      // wrap service and stream in a managed application instance
      // ManagedApplication makes sure that startup/shutdown sequence is right
      // and startup/shutdow errors are handling appropriately
      new ManagedApplication(
        new ManagedHttpService(service),
        new ManagedKafkaStreams(stream),
        jmxReporter)

    } catch {
      case ex: Exception =>
        LOGGER.error("Observed fatal exception instantiating the app", ex)
        if(stream != null) stream.close()
        if(service != null) service.close()
        System.exit(1)
        null
    }
  }

  @VisibleForTesting
  def createStream(kafkaConfig: KafkaConfiguration, healthStatusController: HealthStatusController): KafkaStreams = {
    // service graph kafka stream supplier
    val serviceGraphStreamSupplier = new ServiceGraphStreamSupplier(kafkaConfig)

    // create kstream using application topology
    val streamsSupplier = new StreamSupplier(
      serviceGraphStreamSupplier,
      healthStatusController,
      kafkaConfig.streamsConfig,
      kafkaConfig.consumerTopic)

    // build kstream app
    streamsSupplier.get()
  }

  @VisibleForTesting
  def createService(serviceConfig: ServiceConfiguration, stream: KafkaStreams, storeName: String): HttpService = {
    val localEdgesFetcher = new LocalEdgesFetcher(stream, storeName)
    val remoteEdgesFetcher = new RemoteEdgesFetcher(serviceConfig.client)

    val servlets = Map(
      "/servicegraph/local" -> new LocalServiceGraphResource(localEdgesFetcher),
      "/servicegraph" -> new GlobalServiceGraphResource(stream,
        storeName,
        serviceConfig,
        localEdgesFetcher,
        remoteEdgesFetcher),
      "/isWorking" -> new IsWorkingResource
    )

    new HttpService(serviceConfig, servlets)
  }
}