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
package com.expedia.www.haystack.service.graph.node.finder.utils

import com.expedia.www.haystack.TestSpec

class SpanUtilsSpec extends TestSpec {
  describe("discovering a span type") {
    it("should return CLIENT when both 'cr' and 'cs' is present") {
      Given("a span with 'cr' and 'cs' event logs")
      val (span, _) = newSpan("foo-service", "bar", 6000, client = true, server = false)
      When("getSpanType is called")
      val spanType = SpanUtils.getSpanType(span)
      Then("it is marked as CLIENT")
      spanType should be (SpanType.CLIENT)
    }
    it("should return OTHER when more when 'cr', 'cs' and 'sr' is present") {
      Given("a span with 'cr','cs', 'sr' and 'ss' event logs")
      val (span, _) = newSpan("foo-service", "bar", 6000, client = true, server = true)
      When("getSpanType is called")
      val spanType = SpanUtils.getSpanType(span)
      Then("it is marked as OTHER")
      spanType should be (SpanType.OTHER)
    }
    it("should return SERVER when just 'sr' and 'ss' are present") {
      Given("a span with  'sr' and 'ss' event logs")
      val (span, _) = newSpan("foo-service", "bar", 6000, client = false, server = true)
      When("getSpanType is called")
      val spanType = SpanUtils.getSpanType(span)
      Then("it is marked as SERVER")
      spanType should be (SpanType.SERVER)
    }
  }

  describe("finding an event time") {
    it("should return None with the spanType is OTHER") {
      Given("a span with no event logs")
      val (span, _) = newSpan("foo-service", "bar", 6000, client = false, server = false)
      When("getEventTime is called")
      val eventTime = SpanUtils.getEventTimestamp(span, SpanUtils.SERVER_SEND_EVENT)
      Then("it is marked as OTHER")
      eventTime should be (None)
    }
    it("should return None with the spanType is SERVER and we look for CLIENT_SEND") {
      Given("a span with no event logs")
      val (span, _) = newSpan("foo-service", "bar", 6000, client = false, server = true)
      When("getEventTime is called")
      val eventTime = SpanUtils.getEventTimestamp(span, SpanUtils.CLIENT_SEND_EVENT)
      Then("it is marked as OTHER")
      eventTime should be (None)
    }
    it("should return timeStamp with the spanType is SERVER and we look for SERVER_SEND") {
      Given("a span with no event logs")
      val (span, _) = newSpan("foo-service", "bar", 6000, client = false, server = true)
      When("getEventTime is called")
      val eventTime = SpanUtils.getEventTimestamp(span, SpanUtils.SERVER_SEND_EVENT)
      Then("it is marked as OTHER")
      (eventTime.get > 0) should be (true)
    }
  }

}
