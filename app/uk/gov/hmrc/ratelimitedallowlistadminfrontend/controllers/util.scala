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

import play.api.i18n.Messages
import uk.gov.hmrc.internalauth.client.Resource
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.{Select, SelectItem}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.accessibleautocomplete.AccessibleAutocomplete
import uk.gov.hmrc.govukfrontend.views.Implicits.RichSelect

extension (resources: Set[Resource]) {
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
