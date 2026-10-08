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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers

import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.actions.AuthActions
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.{IntFormProvider, RadioFormProvider}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Timeframe.Unbounded
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{AllowListConfigUpdate, Timeframe}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.{SetRollingLimitTimeView, SetRollingLimitValueView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SetRollingLimitController @Inject()(
                                           mcc: MessagesControllerComponents,
                                           auth: AuthActions,
                                           connector: RateLimitedAllowListConnector,
                                           formProviderInt: IntFormProvider,
                                           viewTime: SetRollingLimitTimeView,
                                           viewValue: SetRollingLimitValueView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:

  private val timeframeForm = RadioFormProvider[Timeframe]
  private def userValueForm(using Messages): Form[Int] = formProviderInt(min = Some(0))
  private val options = RadioFormProvider.options[Timeframe]

  def onPageLoad(service: String, allowList: String, timeframe: Option[Timeframe] = None): Action[AnyContent] =
    auth.authorized.admin.service(service):
      request =>
        given Request[?] = request

        timeframe match {
          case None => Ok(viewTime(timeframeForm, options, service, allowList))
          case Some(Unbounded) => BadRequest(viewTime(timeframeForm, options, service, allowList))
          case Some(value) => Ok(viewValue(userValueForm, service, allowList, value))
        }

  def onSubmit(service: String, allowList: String, timeFrame: Option[Timeframe]): Action[AnyContent] =
    auth.authorized.admin.service(service).async:
      request =>
        given Request[?] = request

        timeFrame match {
          case None =>
            RadioFormProvider[Timeframe].bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(viewTime(formWithErrors, options, service, allowList))),
              {
                case tf @ Unbounded =>
                  val update = AllowListConfigUpdate(timeframe = Some(tf))
                  connector.updateAllowListConfig(service, allowList, update)
                    .map(
                      _ => 
                        Redirect(routes.AllowListSummaryController.root(service, allowList))
                          .flashing(
                            "rlal-notification" -> summon[Messages]("rlal.set_rolling_limit_value.flash.success.unbound")
                        )
                    )
                  
                case tf =>
                  Future.successful(Redirect(routes.SetRollingLimitController.onPageLoad(service, allowList, Some(tf))))

              }
            )

          case Some(Unbounded) =>
            Future.successful(BadRequest(viewTime(timeframeForm, options, service, allowList)))

          case Some(timeframe) =>
            userValueForm.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(viewValue(formWithErrors, service, allowList, timeframe))),

              userLimit =>
                val update = AllowListConfigUpdate(timeframe = Some(timeframe), userLimitPerTimeframe = Some(userLimit))
                connector.updateAllowListConfig(service, allowList, update)
                  .map(
                    _ => {
                      val msg = 
                        if userLimit == 1 then "rlal.set_rolling_limit_value.flash.success.singular"
                        else "rlal.set_rolling_limit_value.flash.success.plural"
                      Redirect(routes.AllowListSummaryController.root(service, allowList))
                        .flashing("rlal-notification" -> summon[Messages](msg, userLimit, timeframe))
                    }
                  )
            )
        }

