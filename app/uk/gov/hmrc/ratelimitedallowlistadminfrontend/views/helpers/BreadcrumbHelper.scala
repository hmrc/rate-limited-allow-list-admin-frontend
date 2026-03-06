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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.views.helpers

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.breadcrumbs.BreadcrumbsItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.breadcrumbs.Breadcrumbs
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers
import play.api.mvc.Call

extension (left: BreadcrumbsItem)
  def >(right: BreadcrumbsItem): Breadcrumbs = Breadcrumbs(List(left, right))

extension (left: Breadcrumbs)
  def >(right: BreadcrumbsItem): Breadcrumbs = left.copy(items = left.items :+ right)

given Conversion[BreadcrumbsItem, Breadcrumbs] with
  def apply(x: BreadcrumbsItem): Breadcrumbs = Breadcrumbs(List(x))

object crumb:
  def home(using Messages): BreadcrumbsItem = 
    page("rlal.index.breadcrumb")(controllers.routes.IndexController.onPageLoad())

  def current(messageKey: String, params: String*)(using messages: Messages): BreadcrumbsItem =
    BreadcrumbsItem(
      content = Text(messages(messageKey, params*)),
      href = None
    )

  def page(messageKey: String, params: String*)(call: Call)(using messages: Messages): BreadcrumbsItem =
    BreadcrumbsItem(
      content = Text(messages(messageKey, params*)),
      href = Some(call.url)
    )

