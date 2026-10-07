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
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.actions.AuthActions
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.BooleanFormProvider
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.AllowListConfigUpdate
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.ToggleNewUserOnboardingView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class ToggleNewUserOnboardingController @Inject()(
  mcc: MessagesControllerComponents,
  auth: AuthActions,
  connector: RateLimitedAllowListConnector,
  formProvider: BooleanFormProvider,
  view: ToggleNewUserOnboardingView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:

  def onPageLoad(service: String, feature: String): Action[AnyContent] =
    auth.authorized.admin.service(service).async:
      request =>
        given Request[?] = request
        connector
          .getAllowList(service, feature)
          .map:
            case Some(allowListConfig) =>
              Ok(view(formProvider().fill(!allowListConfig.isEnabled), allowListConfig))
            case None =>
              Redirect(routes.AllowListSummaryController.root(service, feature))
                .flashing("rlal-notification" -> summon[Messages]("error.flash.feature_not_found", service, feature))


  def onSubmit(service: String, feature: String): Action[AnyContent] =
    auth.authorized.admin.service(service).async:
      request =>
        given Request[?] = request
        formProvider().bindFromRequest().fold(
          formWithErrors => {
            connector.getAllowList(service, feature).map {
              case Some(allowListConfig) =>
                BadRequest(view(formWithErrors.fill(!allowListConfig.isEnabled), allowListConfig))
              case None =>
                Redirect(routes.AllowListSummaryController.root(service, feature))
                .flashing("rlal-notification" -> summon[Messages]("error.flash.feature_not_found", service, feature))
            }
          },
          b =>
            connector
              .updateAllowListConfig(service, feature, AllowListConfigUpdate(isEnabled = Some(b)))
              .map(
                _ =>
                  val successMsg = if b then "rlal.toggle.flash.success.resumed" else "rlal.toggle.flash.success.paused"
                  Redirect(routes.AllowListSummaryController.root(service, feature))
                    .flashing("rlal-notification" -> summon[Messages](successMsg, feature))
              )
        )
