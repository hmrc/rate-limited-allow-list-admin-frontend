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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.util

import uk.gov.hmrc.internalauth.client.{IAAction, Predicate, Resource, ResourceLocation, ResourceType}

object PredicateBuilder:
  case class RLALForService private[util] (service: String):
    def asAdmin: Predicate.Permission = forAction("ADMIN")

    def forAction(action: String): Predicate.Permission =
      Predicate.Permission(
        Resource(
          ResourceType("rate-limited-allow-list-admin-frontend"),
          ResourceLocation(service),
        ),
        IAAction(action)
      )

  def forService(service: String): RLALForService = RLALForService(service)

