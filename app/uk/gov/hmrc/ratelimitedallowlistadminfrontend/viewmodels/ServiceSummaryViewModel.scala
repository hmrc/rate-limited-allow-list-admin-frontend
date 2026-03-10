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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.FeatureSummary
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.helpers.summarylist.*

case class ServiceSummaryViewModel(feature: String, summaryList: SummaryList)

object ServiceSummaryViewModel:

  def apply(featureSummary: FeatureSummary)(using messages: Messages): ServiceSummaryViewModel =
    featureSummary match {
      case FeatureSummary(service, feature, tokens, canIssueTokens) =>
        val (statusMsg, statusActionMsg) = if canIssueTokens then
          ("rlal.service_summary.active.value.onboardingStatusRunning", "rlal.toggle.title.on")
        else
          ("rlal.service_summary.active.value.onboardingStatusPaused", "rlal.toggle.title.off")

        val vm = SummaryListViewModel(
          List(
            SummaryListRowViewModel(
              "rlal.service_summary.active.label.newUserCount",
              ValueViewModel(tokens.toString),
              Seq(
                ActionItemViewModel(
                  "rlal.service_summary.action.increase",
                  routes.IncreaseNewUserLimitController.onPageLoad(service, feature).url
                ).withVisuallyHiddenText(messages("rlal.service_summary.action.increase.visuallyHidden")),
                ActionItemViewModel(
                  "rlal.service_summary.action.set",
                  routes.SetNewUserLimitController.onPageLoad(service, feature).url
                ).withVisuallyHiddenText(messages("rlal.service_summary.action.set.visuallyHidden"))
              )
            ),
            SummaryListRowViewModel(
              "rlal.service_summary.active.label.onboardingStatus",
              ValueViewModel(statusMsg),
              Seq(
                ActionItemViewModel(
                  statusActionMsg,
                  routes.ToggleNewUserOnboardingController.onPageLoad(service, feature).url
                ).withVisuallyHiddenText(messages(statusActionMsg))
              )
            )
          )
        )
        ServiceSummaryViewModel(feature, vm)
    }
