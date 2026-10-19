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

import play.api.libs.json.{JsError, JsString, Json, OFormat, Reads, Writes, __}
import play.api.libs.functional.syntax.*
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.AllowListConfiguration.AllowListLimits
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Timeframe.{Daily, Hourly, Unbounded, Weekdaily, Weekly}

import java.time.Instant

case class AllowListConfiguration(service: String,
                                  feature: String,
                                  isEnabled: Boolean,
                                  limits: AllowListLimits,
                                  percentageLoad: Int,
                                  acceptedCounter: Int,
                                  created: Instant,
                                  lastUpdated: Instant) {
  def allowList: String = feature
}

object AllowListConfiguration {
  given OFormat[AllowListConfiguration] = Json.format

  case class TimeframeLimit(bound: Timeframe, max: Option[Int])
  object TimeframeLimit {
    def unbounded: TimeframeLimit = TimeframeLimit(Unbounded, None)
    def bounded(bound: Timeframe, max: Int): TimeframeLimit = TimeframeLimit(bound, Some(max))

    given Reads[TimeframeLimit] =
      (__.read[String].filter(_ == "unbounded").map(_ => TimeframeLimit.unbounded) or
        (__ \ "hourly").read[Int].map(TimeframeLimit.bounded(Hourly, _)) or
        (__ \ "daily").read[Int].map(TimeframeLimit.bounded(Daily, _)) or
        (__ \ "weekdaily").read[Int].map(TimeframeLimit.bounded(Weekdaily, _)) or
        (__ \ "weekly").read[Int].map(TimeframeLimit.bounded(Weekly, _)))
      .orElse(jsValue => JsError(s"Failed to read timeframe value: $jsValue"))
    
    
    given Writes[TimeframeLimit] = {
      case TimeframeLimit(Unbounded, _) => JsString(Unbounded.value)
      case TimeframeLimit(bound, Some(value)) => Json.obj(bound.value -> value)
      case t @ TimeframeLimit(bound, None) => throw new IllegalArgumentException(s"Invalid TimeframeLimit $t")
    }
  }
  
  case class AllowListLimits(max: Option[Int], timeframe: TimeframeLimit)
  object AllowListLimits {
    given OFormat[AllowListLimits] = Json.format
  }

}
