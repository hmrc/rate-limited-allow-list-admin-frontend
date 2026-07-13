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
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.StringFormProvider
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.AllowList
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.CreateAllowListView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CreateAllowListController @Inject()(
  mcc: MessagesControllerComponents,
  auth: AuthActions,
  connector: RateLimitedAllowListConnector,
  formProvider: StringFormProvider,
  view: CreateAllowListView
)(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging:

  private def form = formProvider("allow list name", 100, AllowList.REGEX_PATTERN, "rlal.create_allow_list.values.hint")

  def onPageLoad(service: String): Action[AnyContent] =
    auth.authorized.admin.service(service):
      request =>
        given Request[?] = request
        Ok(view(form, service))

  def onSubmit(service: String): Action[AnyContent] =
    auth.authorized.admin.service(service).async:
      request =>
        given Request[?] = request
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(view(formWithErrors, service)))
          },
          feature => connector.createAllowList(service, feature).map(
            _ => Redirect(routes.AllowListSummaryController.root(service, feature))
              .flashing("rlal-notification" -> summon[Messages]("rlal.create_allow_list.flash.success", service, feature))
          )
        )
