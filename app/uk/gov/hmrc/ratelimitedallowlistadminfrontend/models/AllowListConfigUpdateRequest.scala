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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.AllowListConfiguration.TimeframeLimit

case class AllowListLimitsUpdateRequest(max: Option[Int], timeframe: Option[TimeframeLimit])
object AllowListLimitsUpdateRequest {
  given OFormat[AllowListLimitsUpdateRequest] = Json.format
}

case class AllowListConfigUpdateRequest(
  limits: Option[AllowListLimitsUpdateRequest],
  percentageLoad: Option[Int],
  isEnabled: Option[Boolean]
)

object AllowListConfigUpdateRequest {
  val empty: AllowListConfigUpdateRequest = AllowListConfigUpdateRequest(None, None, None)

  def apply(timeframe: Option[TimeframeLimit] = None,
            userLimit: Option[Int] = None,
            percentageLoad: Option[Int] = None,
            isEnabled: Option[Boolean] = None): AllowListConfigUpdateRequest = {
    val limit = Option.when(timeframe.isDefined || userLimit.isDefined) {
      AllowListLimitsUpdateRequest(userLimit, timeframe)
    }
    AllowListConfigUpdateRequest(limit, percentageLoad, isEnabled)
  }
  
  def timeframeLimit(timeframeLimit: TimeframeLimit): AllowListConfigUpdateRequest =
    empty.copy(limits = Some(AllowListLimitsUpdateRequest(max = None, timeframe = Some(timeframeLimit))))

  def maxUsers(maxUser: Int): AllowListConfigUpdateRequest =
    empty.copy(limits = Some(AllowListLimitsUpdateRequest(max = Some(maxUser), timeframe = None)))

  def percentage(percentage: Int): AllowListConfigUpdateRequest =
    empty.copy(percentageLoad = Some(percentage))

  def isEnabled(bool: Boolean): AllowListConfigUpdateRequest =
    empty.copy(isEnabled = Some(bool))

  given OFormat[AllowListConfigUpdateRequest] = Json.format
}
