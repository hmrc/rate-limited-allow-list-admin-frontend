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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.actions

import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionFunction, ControllerComponents, Result}
import play.api.mvc.Results.Unauthorized
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.UserMode
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.html.UnauthorizedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AdminUserFilter @Inject() (
                                  view: UnauthorizedView,
                                  cc: ControllerComponents)(
                                  using ec: ExecutionContext
                                ) extends ActionFunction[AnyUserRequest, AdminUserRequest], I18nSupport, Logging {

  override def invokeBlock[A](request: AnyUserRequest[A], block: AdminUserRequest[A] => Future[Result]): Future[Result] = {
    given UserRequest[A] = request
    request.userMode match {
      case UserMode.Admin    =>
        block(AdminUserRequest(request.request, request.headerCarrier, request.authorizationToken))
      case UserMode.ReadOnly =>
        logger.info("User attempted to access a resource without ADMIN permission")
        Future.successful(Unauthorized(view()))
    }
  }

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def executionContext: ExecutionContext = ec
}
