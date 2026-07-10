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

import play.api.mvc.ActionTransformer
import uk.gov.hmrc.internalauth.client.AuthenticatedRequest
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.UserMode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserModeTransformer @Inject() (ec: ExecutionContext)
  extends ActionTransformer[[A] =>> AuthenticatedRequest[A, Boolean], AnyUserRequest] {

  override protected def transform[A](request: AuthenticatedRequest[A, Boolean]): Future[AnyUserRequest[A]] = {
    Future.successful(
      AnyUserRequest[A](request, request.headerCarrier, request.authorizationToken, UserMode(request.retrieval))
    )
  }

  override protected def executionContext: ExecutionContext = ec
}
