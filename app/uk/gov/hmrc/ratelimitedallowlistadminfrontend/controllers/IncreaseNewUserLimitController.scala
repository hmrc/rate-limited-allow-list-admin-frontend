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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, Retrieval}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.IntFormProvider
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.util.RlalPredicate
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.IncreaseNewUserLimitView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class IncreaseNewUserLimitController @Inject()(
  mcc: MessagesControllerComponents,
  auth: FrontendAuthComponents,
  connector: RateLimitedAllowListConnector,
  formProvider: IntFormProvider,
  view: IncreaseNewUserLimitView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:

  private def form: Form[Int] = formProvider()

  private def authorised(service: String) =
    auth.authorizedAction(
      continueUrl = routes.ServiceSummaryController.onPageLoad(service),
      predicate = RlalPredicate.forService(service).asAdmin,
      retrieval = Retrieval.username
    )

  def onPageLoad(service: String, feature: String): Action[AnyContent] =
    authorised(service):
      implicit request =>
        Ok(view(form, service, feature))

  def onSubmit(service: String, feature: String): Action[AnyContent] =
    authorised(service).async:
      implicit request =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(view(formWithErrors, service, feature)))
          },
          userIncrement => connector.addTokens(service, feature, userIncrement).map(
            _ => Redirect(routes.ServiceSummaryController.onPageLoad(service))
              .flashing("rlal-notification" -> summon[Messages]("rlal.increase.success", feature))
          )
        )