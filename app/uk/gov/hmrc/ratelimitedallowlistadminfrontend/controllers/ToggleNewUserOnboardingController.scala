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
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, Retrieval}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.BooleanFormProvider
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.util.PredicateBuilder
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.ToggleNewUserOnboardingView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class ToggleNewUserOnboardingController @Inject()(
  mcc: MessagesControllerComponents,
  auth: FrontendAuthComponents,
  connector: RateLimitedAllowListConnector,
  formProvider: BooleanFormProvider,
  view: ToggleNewUserOnboardingView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:

  private def authorised(service: String) =
    auth.authorizedAction(
      continueUrl = routes.ServiceSummaryController.onPageLoad(service),
      predicate = PredicateBuilder.forService(service).asAdmin,
      retrieval = Retrieval.username
    )

  def onPageLoad(service: String, feature: String): Action[AnyContent] =
    authorised(service).async:
      request =>
        given Request[?] = request
        connector
          .getFeatureMetadata(service, feature)
          .map:
            case Some(metadata) =>
              Ok(view(formProvider().fill(!metadata.canIssueTokens), metadata))
            case None =>
              Redirect(routes.ServiceSummaryController.onPageLoad(service))
                .flashing("rlal-notification" -> summon[Messages]("error.feature_not_found", service, feature))


  def onSubmit(service: String, feature: String): Action[AnyContent] =
    authorised(service).async:
      request =>
        given Request[?] = request
        formProvider().bindFromRequest().fold(
          formWithErrors => {
            connector.getFeatureMetadata(service, feature).map {
              case Some(metadata) =>
                BadRequest(view(formWithErrors.fill(!metadata.canIssueTokens), metadata))
              case None =>
                Redirect(routes.ServiceSummaryController.onPageLoad(service))
                .flashing("rlal-notification" -> summon[Messages]("error.feature_not_found", service, feature))
            }
          },
          bool => connector.setCanIssueTokens(service, feature, bool).map(
            _ =>
              val successMessageKey = if bool then "rlal.toggle.success.resumed" else "rlal.toggle.success.paused"
              Redirect(routes.ServiceSummaryController.onPageLoad(service))
                .flashing("rlal-notification" -> summon[Messages](successMessageKey, feature))
          )
        )
