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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms

import play.api.data.Form
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.forms.mappings.Mappings

import scala.compiletime.summonAll
import scala.deriving.Mirror

sealed trait RadioFormProvider

object RadioFormProvider extends RadioFormProvider, Mappings {
  inline def apply[A](using m: Mirror.SumOf[A], conversion: Conversion[A, String]): Form[A] =
    Form(
      "value" -> radio[A]()
  )

  inline def options[A](using m: Mirror.SumOf[A], conversion: Conversion[A, String]): Seq[RadioItem] =
    summonAll[Tuple.Map[m.MirroredElemTypes, ValueOf]]
      .toList
      .asInstanceOf[List[ValueOf[A]]]
      .map(_.value)
      .zipWithIndex
      .map{
        case (value, index) =>
          RadioItem(
            content = Text(conversion(value)),
            value   = Some(conversion(value)),
            id      = Some(s"value_$index")
          )
      }
}
