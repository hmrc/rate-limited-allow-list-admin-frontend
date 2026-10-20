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

import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{AllowListConfiguration, AllowListReport, UserMode}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.AllowListConfiguration.*
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Timeframe.Unbounded
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.helpers.summarylist.*
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.helpers.given

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

object AllowListSummaryViewModel extends Logging:

  private val formatterDate = DateTimeFormatter
    .ofPattern("h:mm a z, EEEE dd MMMM, yyyy", Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

  extension (i: Instant) {
    def dateTimeStr: String = formatterDate.format(i)
  }

  def apply(allowListConfig: AllowListConfiguration, report: AllowListReport, userMode: UserMode)(using messages: Messages): AllowListSummaryViewModel = {
    val AllowListConfiguration(
      service,
      allowList,
      isEnabled,
      limits,
      percentageLoad,
      _,
      created,
      lastUpdated
    ) = allowListConfig
    
    val acceptedCounter = report.currentUserCount

    val AllowListLimits(userLimit, timeframe) = limits

    val (statusMsg, statusActionMsg) = if isEnabled then
      (
        "rlal.allow_list_summary.allowList.onboardingStatus.value.Running",
        "rlal.allow_list_summary.allowList.onboardingStatus.action.Running"
      )
    else
      (
        "rlal.allow_list_summary.allowList.onboardingStatus.value.Paused",
        "rlal.allow_list_summary.allowList.onboardingStatus.action.Paused"
      )

    val userLimitRow = { 
      val value = timeframe match {
        case TimeframeLimit(Unbounded, _) =>
          ValueViewModel(messages("rlal.allow_list_summary.users.userTimeLimit.unbounded"))
          
        case TimeframeLimit(bound, Some(1)) =>
          ValueViewModel(messages("rlal.allow_list_summary.users.userTimeLimit.value.singular", 1, bound.value))
          
        case TimeframeLimit(bound, Some(max)) =>
          ValueViewModel(messages("rlal.allow_list_summary.users.userTimeLimit.value.plural", max, bound.value))
 
        case TimeframeLimit(bound, None) =>
          val msg = s"Timeframe $bound has not max value"
          logger.error(msg)
          throw new IllegalStateException(msg)
      }
      
      SummaryListRowViewModel(
        "rlal.allow_list_summary.users.userTimeLimit.label",
        value,
        Option.when(userMode.isAdmin)(
          ActionItemViewModel(
            "rlal.allow_list_summary.users.userTimeLimit.action",
            routes.SetRollingLimitController.onPageLoad(service, allowList).url
          ).withVisuallyHiddenText(
            messages("rlal.allow_list_summary.users.userTimeLimit.action.visuallyHidden", timeframe)
          )
        ).toList
      )
    }

    val userSummary = SummaryListViewModel(
      List(
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.currentUserCount.label",
          ValueViewModel(acceptedCounter.toString)
        ),
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.percentageOnboarding.label",
          ValueViewModel(s"$percentageLoad%"),
          Option.when(userMode.isAdmin)(
            ActionItemViewModel(
              "rlal.allow_list_summary.users.percentageOnboarding.action",
              routes.SetPercentageLimitController.onPageLoad(service, allowList).url
            ).withVisuallyHiddenText(
              messages("rlal.allow_list_summary.users.percentageOnboarding.action.visuallyHidden")
            )
          ).toList
        ),
        userLimitRow,
        SummaryListRowViewModel(
          "rlal.allow_list_summary.users.totalUserLimit.label",
          ValueViewModel(
            userLimit match {
              case Some(value) => messages("rlal.allow_list_summary.users.totalUserLimit.value", value)
              case None => messages("rlal.allow_list_summary.users.totalUserLimit.empty.value")
            }
          ),
          Option.when(userMode.isAdmin)(
            ActionItemViewModel(
              "rlal.allow_list_summary.users.totalUserLimit.action",
              routes.SetUserLimitController.onPageLoad(service, allowList).url
            ).withVisuallyHiddenText(
              messages("rlal.allow_list_summary.users.totalUserLimit.action.visuallyHidden", timeframe)
            )
          ).toList
        )
      )
    )

    val summary = SummaryListViewModel(
      List(
        SummaryListRowViewModel(
          "rlal.allow_list_summary.allowList.lastUpdate.label",
          ValueViewModel(lastUpdated.dateTimeStr),
          List.empty
        ),
        SummaryListRowViewModel(
          "rlal.allow_list_summary.allowList.created.label",
          ValueViewModel(created.dateTimeStr),
          List.empty
        ),
        SummaryListRowViewModel( // TODO: Get from API
          "rlal.allow_list_summary.allowList.endDate.label",
          ValueViewModel(created.plus(90, ChronoUnit.DAYS).atZone(ZoneId.of("UTC")).toInstant.dateTimeStr),
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
        )
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
  }