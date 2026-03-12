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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, Retrieval}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.util.PredicateBuilder
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.AllowListSummaryViewModel
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.AllowListSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AllowListSummaryController @Inject()(
                                          mcc: MessagesControllerComponents,
                                          auth: FrontendAuthComponents,
                                          connector: RateLimitedAllowListConnector,
                                          view: AllowListSummaryView
                                        )(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:

  private def authorised(service: String) =
    auth.authorizedAction(
      continueUrl = routes.ServiceSummaryController.onPageLoad(service),
      predicate = PredicateBuilder.forService(service).asAdmin,
      retrieval = Retrieval.username
    )

  def root(service: String, feature: String) = Action(Redirect(routes.AllowListSummaryController.onPageLoad(service, feature)))
    
  def onPageLoad(service: String, feature: String): Action[AnyContent] =
    authorised(service).async {
      request =>
        given Request[?] = request

        val metadataF = connector.getFeatureMetadata(service, feature)
        val reportF = connector.getFeatureReport(service, feature)

        for
          metadataOpt <- metadataF
          reportOpt <- reportF
        yield
          (metadataOpt, reportOpt) match
            case (Some(metadata), Some(report)) =>
              val vm = AllowListSummaryViewModel(metadata, report)
              Ok(view(service, feature, Some(vm)))
            case (mOpt, rOpt) =>
              logger.error(s"For service $service and feature $feature, metadata was ${mOpt.showStatus} and report was ${rOpt.showStatus}")
              Ok(view(service, feature, None))
    }

extension (opt: Option[?])
  def showStatus: String = opt.fold("undefined")(_ => "defined")