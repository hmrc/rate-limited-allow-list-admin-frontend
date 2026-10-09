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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.actions.{AdminUserRequest, AnyUserRequest, AuthActions, UserRequest}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.UserMode
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.UserMode.{Admin, ReadOnly}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.viewmodels.AllowListSummaryViewModel
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.AllowListSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AllowListSummaryController @Inject()(
  mcc: MessagesControllerComponents,
  auth: AuthActions,
  connector: RateLimitedAllowListConnector,
  view: AllowListSummaryView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging {

  def root(service: String, allowList: String): Action[AnyContent] =
    auth.authorized.service(service) {
      r =>
        r.userMode match {
          case Admin => Redirect(routes.AllowListSummaryController.manage(service, allowList)).flashing(r.flash)
          case ReadOnly => Redirect(routes.AllowListSummaryController.view(service, allowList))
        }
    }

  def view(service: String, allowList: String): Action[AnyContent] =
    auth.authorized.service(service).async {
      request =>
        given AnyUserRequest[AnyContent] = request
        onPageLoad(service, allowList, ReadOnly)
    }

  def manage(service: String, allowList: String): Action[AnyContent] =
    auth.authorized.admin.service(service).async {
      request =>
        given AdminUserRequest[?] = request
        onPageLoad(service, allowList, Admin)
    }

  private def onPageLoad(service: String, allowList: String, mode: UserMode)(using request: UserRequest[?]): Future[Result] =
    for
      allowListConfigOpt <- connector.getAllowList(service, allowList)
    yield
      allowListConfigOpt match
        case Some(metadata) =>
          val vm = AllowListSummaryViewModel(metadata,  mode)
          Ok(view(service, allowList, Some(vm)))

        case None =>
          logger.error(s"No configuration found for service $service and allow list $allowList")
          Ok(view(service, allowList, None))
}