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

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.service.graph.node.finder.utils.SpanType.SpanType
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConversions._

/**
  * Object with utility methods to process a Span
  */
object SpanUtils {

  val SERVER_SEND_EVENT = "ss"
  val SERVER_RECV_EVENT = "sr"
  val CLIENT_SEND_EVENT = "cs"
  val CLIENT_RECV_EVENT = "cr"

  private val ONE = math.pow(2, 0).asInstanceOf[Int]
  private val TWO = math.pow(2, 1).asInstanceOf[Int]
  private val FOUR = math.pow(2, 2).asInstanceOf[Int]
  private val EIGHT = math.pow(2, 3).asInstanceOf[Int]

  private val THREE = ONE | TWO
  private val TWELVE = FOUR | EIGHT

  private val SPAN_MARKERS = Map(
    CLIENT_SEND_EVENT -> Flag(ONE), CLIENT_RECV_EVENT -> Flag(TWO),
    SERVER_SEND_EVENT -> Flag(FOUR), SERVER_RECV_EVENT -> Flag(EIGHT))

  private val SPAN_TYPE_MAP = Map(Flag(THREE) -> SpanType.CLIENT, Flag(TWELVE) -> SpanType.SERVER)

  /**
    * Given a span check if it is eligible for accumulation and can be a weighable span
    * @param span span to validate
    * @return
    */
  def isAccumulableSpan(span: Span): Boolean =
    StringUtils.isNotBlank(span.getSpanId)&&
    StringUtils.isNotBlank(span.getServiceName) &&
    StringUtils.isNotBlank(span.getOperationName) &&
    span.getStartTime > 0

  /**
    * Given a span, this method looks for ('cs', 'cr') and ('sr', 'ss') pairs in log fields with key as "event"
    * to identify a span type. Presence of ('cs', 'cr') events will result in SpanType.CLIENT and presence of
    * events ('sr', 'ss') events will result in SpanType.SERVER. All other spans will be identified as OTHER
    * @param span Span to identify
    * @return Some(SpanType) of the given span or None
    */
  def getSpanType(span: Span): SpanType = {
    var flag = Flag(0)
    span.getLogsList.forEach(log => {
      log.getFieldsList.foreach(tag => {
        if (tag.getKey.equalsIgnoreCase("event") && StringUtils.isNotEmpty(tag.getVStr)) {
          flag = flag | SPAN_MARKERS.getOrElse(tag.getVStr.toLowerCase, Flag(0))
        }
      })
    })
    SPAN_TYPE_MAP.getOrElse(flag, SpanType.OTHER)
  }

  /**
    * Finds the timestamp of the log entry in the given span that has a key named "event" with value that matches
    * the given eventValue
    * @param span Span from which event timestamp to be read
    * @param eventValue value if the "event" field to match
    * @return Some(Long) of the timestamp read or None
    */
  def getEventTimestamp(span: Span, eventValue: String): Option[Long] =
    span.getLogsList.find(log => {
      log.getFieldsList.exists(tag => {
        tag.getKey.equalsIgnoreCase("event") && StringUtils.isNotEmpty(tag.getVStr) &&
          tag.getVStr.equalsIgnoreCase(eventValue)
      })
    }) match {
      case Some(log) => Option(log.getTimestamp)
      case _ => None
    }
}

/**
  * Enum for different span types processed
  * by the node finder application
  */
object SpanType extends Enumeration {
  type SpanType = Value
  val SERVER, CLIENT, OTHER = Value
}

/**
  * Simple case class representing a flag
  * @param value : value of the flag
  */
case class Flag(value: Int) {
  def | (that: Flag): Flag = Flag(this.value | that.value)

  override def equals(obj: scala.Any): Boolean = {
    obj.asInstanceOf[Flag].value == value
  }
}


