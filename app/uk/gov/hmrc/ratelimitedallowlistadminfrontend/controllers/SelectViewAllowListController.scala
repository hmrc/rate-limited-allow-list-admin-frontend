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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Implicits.RichSelect
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.{Select, SelectItem}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.accessibleautocomplete.AccessibleAutocomplete
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.actions.AuthActions
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.StringFormProvider
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.SelectViewAllowListView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class SelectViewAllowListController @Inject()(
                                                 mcc: MessagesControllerComponents,
                                                 auth: AuthActions,
                                                 formProvider: StringFormProvider,
                                                 connector: RateLimitedAllowListConnector,
                                                 view: SelectViewAllowListView
                                               )(using ExecutionContext) extends FrontendController(mcc), I18nSupport, Logging {
  def onPageLoad(): Action[AnyContent] =
    auth.authenticated().async { request =>
      given AuthenticatedRequest[AnyContent, Unit] = request

      connector.getServices("read").map {
        case services if (services.isEmpty) =>
          logger.info("No services returned, no active lists")
          
          Redirect(routes.IndexController.onPageLoad())
            .flashing(
              "rlal-notification" -> summon[Messages]("error.flash.retrievals_empty"),
              "rlal-notification-type" -> summon[Messages]("site.error")
            )
        case services =>
          val items = services.map(service => SelectItem(text = service))

          val vm = Select(
            name = "value",
            items = SelectItem(text = "", selected = true, disabled = true) +: items
          ).asAccessibleAutocomplete(Some(AccessibleAutocomplete(showAllValues = true)))


          Ok(view(formProvider("service", 100), vm))
      }
    }

  def onSubmit(): Action[AnyContent] =
    auth.authenticated().async { request =>
      given AuthenticatedRequest[AnyContent, Unit] = request

      connector.getServices("read").map {
        case services if (services.isEmpty) =>
          logger.info("No services returned, no active lists")

          Redirect(routes.IndexController.onPageLoad())
            .flashing(
              "rlal-notification" -> summon[Messages]("error.flash.retrievals_empty"),
              "rlal-notification-type" -> summon[Messages]("site.error")
            )
 
        case services =>
          val items = services.map(service => SelectItem(text = service))

          val vm = Select(
            name = "value",
            items = SelectItem(text = "", selected = true, disabled = true) +: items
          ).asAccessibleAutocomplete(Some(AccessibleAutocomplete(showAllValues = true)))

          val submittedForm = formProvider("service", 100).bindFromRequest()

          submittedForm.fold(
            formWithErrors => BadRequest(view(formWithErrors, vm)),
            selection =>
              if services.contains(selection) then
                  Redirect(routes.ServiceSummaryController.onPageLoad(selection))
              else
                val formWithErrors = submittedForm.withError("value", "rlal.selectcreate.heading")
                BadRequest(view(formWithErrors, vm))
        )


      }


    }
}
