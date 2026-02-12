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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, IAAction, Resource, Retrieval}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendBaseController, FrontendController}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.util.RlalPredicate
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.ServiceSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ServiceSummaryController @Inject()(
  mcc: MessagesControllerComponents,
  auth: FrontendAuthComponents,
  connector: RateLimitedAllowListConnector,
  view: ServiceSummaryView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:
 
  private def authorised(service: String) =
    auth.authorizedAction(
      continueUrl = routes.ServiceSummaryController.onPageLoad(service),
      predicate = RlalPredicate.forService(service).asAdmin,
      retrieval = Retrieval.username
    )

  def onPageLoad(service: String): Action[AnyContent] =
    authorised(service).async:
      implicit request =>
        connector
          .getFeatures(service)
          .map:
            summaries =>
              Ok(view(service, summaries))