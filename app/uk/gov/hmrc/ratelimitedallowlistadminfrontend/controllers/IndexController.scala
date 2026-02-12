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
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, Resource, ResourceType, Retrieval}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.IndexView

import javax.inject.{Inject, Singleton}

@Singleton
class IndexController @Inject()(
  mcc: MessagesControllerComponents,
  auth: FrontendAuthComponents,
  view: IndexView
) extends FrontendController(mcc), I18nSupport, Logging:

  private val authenticated =
    auth.authenticatedAction(
      continueUrl = routes.IndexController.onPageLoad(),
      retrieval = Retrieval.locations(Some(ResourceType("rate-limited-allow-list-admin-frontend")))
    )

  def onPageLoad(): Action[AnyContent] =
    authenticated { implicit request =>
      val resources: Seq[Resource] = request.retrieval.toList.sortBy(_.resourceLocation.value)
      Ok(view(resources))
    }

  def stopOnboardingUsers(service: String, feature: String): Action[AnyContent] = Action(Ok)
  
  def startOnboardingUser(service: String, feature: String): Action[AnyContent] = Action(Ok)
    
  
  def setNewUserLimit(service: String, feature: String): Action[AnyContent] = Action(Ok)
