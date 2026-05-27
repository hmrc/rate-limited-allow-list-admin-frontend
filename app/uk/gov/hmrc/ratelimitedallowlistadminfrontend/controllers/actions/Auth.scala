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
import play.api.mvc.AnyContent
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes

import javax.inject.Inject

class Auth @Inject() (authComponents: FrontendAuthComponents) extends Logging {
  private val resourceType = ResourceType("rate-limited-allow-list-admin-frontend")

  private lazy val index = routes.IndexController.onPageLoad()

  object authenticated {
    def apply(): AuthenticatedActionBuilder[Unit, AnyContent] =
      authComponents.authenticatedAction(continueUrl = index)

    object retrieval {
      def locations(): AuthenticatedActionBuilder[Set[Resource], AnyContent] =
        authComponents.authenticatedAction(
          continueUrl = index,
          retrieval = Retrieval.locations(resourceType = Some(resourceType), action = Some(IAAction("ADMIN")))
        )
    }
  }
  
  object authorized {
    object admin {
      def service(service: String): AuthenticatedActionBuilder[Unit, AnyContent] =
        authComponents.authorizedAction(
          continueUrl = index,
          predicate = Predicate.Permission(
            Resource(
              ResourceType("rate-limited-allow-list-admin-frontend"),
              ResourceLocation(service),
            ),
            IAAction("ADMIN")
          )
        )
    }
    
    object readOnly {
      def serviceAdmin(service: String): AuthenticatedActionBuilder[Unit, AnyContent] =
        authComponents.authorizedAction(
          continueUrl = index,
          predicate =
            Predicate.Permission(
              Resource(
                ResourceType("rate-limited-allow-list-admin-frontend"),
                ResourceLocation(service),
              ),
              IAAction("READ")
            )
        )
    }
  }
}
