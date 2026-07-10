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
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.actions.{Auth, RequireRetrievals}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.StringFormProvider
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.SelectCreateAllowListView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class SelectCreateAllowListController @Inject()(
                                                 mcc: MessagesControllerComponents,
                                                 auth: Auth,
                                                 formProvider: StringFormProvider,
                                                 requireRetrievals: RequireRetrievals,
                                                 view: SelectCreateAllowListView
                                               ) extends FrontendController(mcc), I18nSupport, Logging:

  def onPageLoad(): Action[AnyContent] =
    (auth.authenticated.retrieveLocations.admin() andThen requireRetrievals).async { request =>
      given AuthenticatedRequest[AnyContent, Set[Resource]] = request
      if request.retrieval.isEmpty then {
        logger.info("No services returned for user on load. Check if the user has been added to a team")
      }

      Future.successful(Ok(view(formProvider("service", 100), request.retrieval.toSelectVM)))
    }

  def onSubmit(): Action[AnyContent] =
    (auth.authenticated.retrieveLocations.admin() andThen requireRetrievals).async { request =>
      given AuthenticatedRequest[AnyContent, Set[Resource]] = request
      if request.retrieval.isEmpty then {
        logger.info("No services returned for user on submit. Check if the user has been added to a team")
      }

      val submittedForm = formProvider("service", 100).bindFromRequest()
      submittedForm.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, request.retrieval.toSelectVM))),
          selection =>
            if request.retrieval.containsResource(selection) then
              Future.successful(Redirect(routes.CreateAllowListController.onPageLoad(selection)))
            else {
              val formWithErrors = submittedForm.withError("value", "rlal.selectcreate.heading")
              Future.successful(BadRequest(view(formWithErrors, request.retrieval.toSelectVM)))
            }

      )
    }
    

object SelectCreateAllowListController {
  extension (resources: Set[Resource])
    def containsResource(name: String): Boolean = resources.exists(_.resourceLocation.value == name)

    def toSelectVM(using Messages): Select = {
      val items =
        resources.toList.sortBy(_.resourceLocation.value)
          .map(resource => SelectItem(text = resource.resourceLocation.value))

      Select(
        name = "value",
        items = SelectItem(text = "", selected = true, disabled = true) +: items
      ).asAccessibleAutocomplete(Some(
        AccessibleAutocomplete(showAllValues = true)
      ))
    }
}
