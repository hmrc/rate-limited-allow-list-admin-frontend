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
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{AllowListConfiguration, UserMode}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.helpers.summarylist.*

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}
import java.util.Locale

case class AllowListSummaryViewModel(
  service: String,
  feature: String,
  userSummary: SummaryList,
  allowListSummary: SummaryList,
  settings: SummaryList
)

object AllowListSummaryViewModel:

  private val formatterDate = DateTimeFormatter
    .ofPattern("h:mm a z, EEEE dd MMMM, yyyy", Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

  extension (i: Instant) {
    def dateTimeStr: String = formatterDate.format(i)
  }

  def apply(allowListConfig: AllowListConfiguration, userMode: UserMode)(using messages: Messages): AllowListSummaryViewModel =
    val service = allowListConfig.service
    val allowList = allowListConfig.feature

    val (statusMsg, statusActionMsg) = if allowListConfig.isEnabled then
      (
        "rlal.allow_list_summary.allowList.onboardingStatus.value.Running",
        "rlal.allow_list_summary.allowList.onboardingStatus.action.Running"
      )
    else
      (
        "rlal.allow_list_summary.allowList.onboardingStatus.value.Paused",
        "rlal.allow_list_summary.allowList.onboardingStatus.action.Paused"
      )

    val userSummary = SummaryListViewModel(
      List(
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.currentUserCount.label",
          ValueViewModel(allowListConfig.acceptedCounter.toString)
        ),
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.percentageOnboarding.label",
          ValueViewModel(s"${allowListConfig.percentageLoad}%"),
          Option.when(userMode.isAdmin)(
            ActionItemViewModel(
              "rlal.allow_list_summary.users.percentageOnboarding.action",
              routes.SetPercentageLimitController.onPageLoad(service, allowList).url
            ).withVisuallyHiddenText(
              messages("rlal.allow_list_summary.users.percentageOnboarding.action.visuallyHidden")
            )
          ).toList
        ),
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.userTimeLimit.label",
          ValueViewModel(messages("rlal.allow_list_summary.users.userTimeLimit.value", allowListConfig.userLimitPerTimeframe, allowListConfig.timeframe)),
          Option.when(userMode.isAdmin)(
            ActionItemViewModel(
              "rlal.allow_list_summary.users.userTimeLimit.action",
              "" // TODO
            ).withVisuallyHiddenText(
              messages("rlal.allow_list_summary.users.userTimeLimit.action.visuallyHidden", allowListConfig.timeframe)
            )
          ).toList
        ),
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.totalUserLimit.label",
          ValueViewModel(
            allowListConfig.userLimit match {
              case Some(value) => messages("rlal.allow_list_summary.users.totalUserLimit.value", value)
              case None => messages("rlal.allow_list_summary.users.totalUserLimit.empty.value")
            }
          ),
          Option.when(userMode.isAdmin)(
            ActionItemViewModel(
              "rlal.allow_list_summary.users.totalUserLimit.action",
              routes.SetUserLimitController.onPageLoad(service, allowList).url
            ).withVisuallyHiddenText(
              messages("rlal.allow_list_summary.users.totalUserLimit.action.visuallyHidden", allowListConfig.timeframe)
            )
          ).toList
        )
      )
    )

    val summary = SummaryListViewModel(
      List(
        SummaryListRowViewModel(
          "rlal.allow_list_summary.allowList.lastUpdate.label",
          ValueViewModel(allowListConfig.lastUpdated.dateTimeStr),
          List.empty
        ),
        SummaryListRowViewModel(
          "rlal.allow_list_summary.allowList.created.label",
          ValueViewModel(allowListConfig.created.dateTimeStr),
          List.empty
        ),
        SummaryListRowViewModel( // TODO: Get from API
          "rlal.allow_list_summary.allowList.endDate.label",
          ValueViewModel(allowListConfig.created.plus(90, ChronoUnit.DAYS).atZone(ZoneId.of("UTC")).toInstant.dateTimeStr),
          List.empty
        )
      )
    )


    val manage = SummaryListViewModel(
      List(
        SummaryListRowViewModel(
          "rlal.allow_list_summary.allowList.onboardingStatus.label",
          ValueViewModel(statusMsg),
          Option.when(userMode.isAdmin)(
            ActionItemViewModel(
              statusActionMsg,
              routes.ToggleNewUserOnboardingController.onPageLoad(service, allowList).url
            )
          ).toList
        ),
//        SummaryListRowViewModel(
//          "rlal.allow_list_summary.manage.delete.label",
//          ValueViewModel(" "),
//          Option.when(userMode.isAdmin)(
//            ActionItemViewModel(
//              "rlal.allow_list_summary.manage.delete.action",
//              ""
//            ).withVisuallyHiddenText(
//              messages("rlal.allow_list_summary.manage.delete.action.visuallyHidden")
//            )
//          ).toList
//        )
      )
    )

    AllowListSummaryViewModel(service, allowList, userSummary, summary, manage)