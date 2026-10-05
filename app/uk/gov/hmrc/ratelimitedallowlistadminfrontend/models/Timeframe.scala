/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.models

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}
import play.api.mvc.QueryStringBindable

enum Timeframe(final val value: String) {
  case Hourly    extends Timeframe("hourly")
  case Daily     extends Timeframe("daily")
  case Weekdaily extends Timeframe("weekdaily")
  case Weekly    extends Timeframe("weekly")
  case Unbounded extends Timeframe("unbounded")

  override def toString: String = value
}


object Timeframe:
  
  given QueryStringBindable[Timeframe] = new QueryStringBindable[Timeframe] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Timeframe]] =
      summon[QueryStringBindable[String]].bind(key, params).map(
        _.fold(
          x => Timeframe.values.find(_.value == x).toRight("Invalid value for timeframe"),
          Left.apply
        )
      )

    override def unbind(key: String, value: Timeframe): String =
      summon[QueryStringBindable[String]].unbind(key, value.value)
  }


  given format: Format[Timeframe] = Format[Timeframe](
    Reads[Timeframe] {
      case JsString(Hourly.value) => JsSuccess(Hourly)
      case JsString(Daily.value) => JsSuccess(Daily)
      case JsString(Weekdaily.value) => JsSuccess(Weekdaily)
      case JsString(Weekly.value) => JsSuccess(Weekly)
      case JsString(Unbounded.value) => JsSuccess(Unbounded)
      case other => JsError(s"Timeframe must be a known JsString value, found: $other")
    },
    Writes[Timeframe] { t =>
      JsString(t.value)
    }
  )
