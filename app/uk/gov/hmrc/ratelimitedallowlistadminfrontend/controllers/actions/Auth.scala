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
import play.api.mvc.*
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes

import javax.inject.Inject

class Auth @Inject() (
  authComponents: FrontendAuthComponents,
  userModeTransformer: UserModeTransformer,
  adminUserFilter: AdminUserFilter
) extends Logging {
  private val resourceType = ResourceType("rate-limited-allow-list-admin-frontend")

  private lazy val index = routes.IndexController.onPageLoad()

  object authenticated {
    def apply(): AuthenticatedActionBuilder[Unit, AnyContent] =
      authComponents.authenticatedAction(continueUrl = index)

    object retrieveLocations {
      def admin(): AuthenticatedActionBuilder[Set[Resource], AnyContent] =
        authComponents.authenticatedAction(
          continueUrl = index,
          retrieval = Retrieval.locations(resourceType = Some(resourceType), action = Some(IAAction("ADMIN")))
        )
        
      def all(): AuthenticatedActionBuilder[Set[Resource], AnyContent] =
        authComponents.authenticatedAction(
          continueUrl = index,
          retrieval = Retrieval.locations(resourceType = Some(resourceType))
        )
    }
  }

  object authorized {
    outer =>

    private def permission(role: "ADMIN" | "READ", service: String) =
      Predicate.Permission(Resource(resourceType, ResourceLocation(service)), IAAction(role))

    def service(service: String): ActionBuilder[AnyUserRequest, AnyContent] =
      authComponents.authorizedAction(
        continueUrl = index,
        predicate = Predicate.or(permission("ADMIN", service), permission("READ", service)),
        retrieval = Retrieval.hasPredicate(permission("ADMIN", service))
      ).andThen(userModeTransformer)

    object admin {
      def service(service: String): ActionBuilder[AdminUserRequest, AnyContent] =
        outer.service(service).andThen(adminUserFilter)
    }
  }
}
