package com.expedia.www.haystack.service.graph.node.finder.config

import com.expedia.www.haystack.UnitTestSpec
import com.expedia.www.haystack.commons.kstreams.SpanTimestampExtractor
import com.typesafe.config.ConfigException

class AppConfigurationSpec extends UnitTestSpec {
  describe("loading application configuration") {
    it("should fail creating KafkaConfiguration if no application id is specified") {
      Given("a test configuration file")
      val file = "test_no_app_id.conf"
      When("Application configuration is loaded")
      Then("it should throw an exception")
      intercept[IllegalArgumentException] {
        new AppConfiguration(file).kafkaConfig
      }
    }
    it("should fail creating KafkaConfiguration if no bootstrap is specified") {
      Given("a test configuration file")
      val file = "test_no_bootstrap.conf"
      When("Application configuration is loaded")
      Then("it should throw an exception")
      intercept[IllegalArgumentException] {
        new AppConfiguration(file).kafkaConfig
      }
    }
    it("should fail creating KafkaConfiguration if no metrics topic is specified") {
      Given("a test configuration file")
      val file = "test_no_metrics_topic.conf"
      When("Application configuration is loaded")
      Then("it should throw an exception")
      intercept[ConfigException] {
        new AppConfiguration(file).kafkaConfig
      }
    }
    it("should fail creating KafkaConfiguration if no consumer is specified") {
      Given("a test configuration file")
      val file = "test_no_consumer.conf"
      When("Application configuration is loaded")
      Then("it should throw an exception")
      intercept[ConfigException] {
        new AppConfiguration(file).kafkaConfig
      }
    }
    it("should fail creating KafkaConfiguration if no producer is specified") {
      Given("a test configuration file")
      val file = "test_no_producer.conf"
      When("Application configuration is loaded")
      Then("it should throw an exception")
      intercept[ConfigException] {
        new AppConfiguration(file).kafkaConfig
      }
    }
    it("should create KafkaConfiguration as specified") {
      Given("a test configuration file")
      val file = "test.conf"
      When("Application configuration is loaded and KafkaConfiguration is obtained")
      val config = new AppConfiguration(file).kafkaConfig
      Then("it should load as expected")
      config.streamsConfig.defaultTimestampExtractor() shouldBe a [SpanTimestampExtractor]
      config.serviceCallTopic should be ("graph-nodes")
      config.aggregatorInterval should be (60000)
    }
  }

}
