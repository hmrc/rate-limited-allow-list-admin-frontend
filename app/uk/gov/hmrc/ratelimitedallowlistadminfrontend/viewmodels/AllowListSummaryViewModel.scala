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
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{AllowListReport, FeatureSummary, UserMode}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.helpers.summarylist.*

case class AllowListSummaryViewModel(
  service: String,
  feature: String,
  userSummary: SummaryList,
  allowListSummary: SummaryList
)

object AllowListSummaryViewModel:

  def apply(featureSummary: FeatureSummary, report: AllowListReport, userMode: UserMode)(using messages: Messages): AllowListSummaryViewModel =
    featureSummary match {
      case FeatureSummary(service, feature, tokens, canIssueTokens) =>
        val (statusMsg, statusActionMsg) = if canIssueTokens then
          (
            "rlal.allow_list_summary.allowList.onboardingStatus.value.Running",
            "rlal.allow_list_summary.allowList.onboardingStatus.action.Running"
          )
        else
          (
            "rlal.allow_list_summary.allowList.onboardingStatus.value.Paused",
            "rlal.allow_list_summary.allowList.onboardingStatus.action.Paused"
          )

        val userActions = userMode match {
          case UserMode.ReadOnly => Seq.empty
          case UserMode.Admin =>
            Seq(
              ActionItemViewModel(
                "rlal.allow_list_summary.users.newUserCount.action.incr",
                routes.IncreaseNewUserLimitController.onPageLoad(service, feature).url

              ).withVisuallyHiddenText(messages("rlal.allow_list_summary.users.newUserCount.action.incr.visuallyHidden")),
              ActionItemViewModel(
                "rlal.allow_list_summary.users.newUserCount.action.set",
                routes.SetNewUserLimitController.onPageLoad(service, feature).url
              ).withVisuallyHiddenText(messages("rlal.allow_list_summary.users.newUserCount.action.set.visuallyHidden"))
            )
        }
        val userSummary = SummaryListViewModel(
          List(
            SummaryListRowViewModel(
              "rlal.allow_list_summary.users.currentUserCount.label",
              ValueViewModel(report.currentUserCount.toString)
            ),
            SummaryListRowViewModel(
              "rlal.allow_list_summary.users.newUserCount.label",
              ValueViewModel(tokens.toString),
              userActions
            )
          )
        )

        val allowListSummaryActions = userMode match {
          case UserMode.ReadOnly => Seq.empty
          case UserMode.Admin =>
            Seq(
              ActionItemViewModel(
                statusActionMsg,
                routes.ToggleNewUserOnboardingController.onPageLoad(service, feature).url
              ).withVisuallyHiddenText(messages(statusActionMsg))
            )
        }
        val allListSummary = SummaryListViewModel(
          List(
            SummaryListRowViewModel(
              "rlal.allow_list_summary.allowList.onboardingStatus.label",
              ValueViewModel(statusMsg),
              allowListSummaryActions
            )
          )
        )

        AllowListSummaryViewModel(report.service, feature, userSummary, allListSummary)
    }
