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

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IntFormProviderSpec extends AnyWordSpec, Matchers, OptionValues:

  val form = IntFormProvider()()

  "must bind when given valid data" in {
    val data = Map("value" -> "100")

    val expected = 100

    val boundForm = form.bind(data)

    boundForm.errors mustBe empty
    boundForm.value.value mustEqual expected
  }
  
  "must fail to bind when value is negative a number" in {
    val data = Map("value" -> "-100")
    val boundForm = form.bind(data)

    val field = boundForm("value")
    field.errors.length mustBe 1

    val error = field.error.value
    error.message mustEqual "error.nonNegative"
    error.key mustEqual "value"
  }

  "must fail to bind when value is not a number" in {
    val data = Map("value" -> "foo")
    val boundForm = form.bind(data)

    val field = boundForm("value")
    field.errors.length mustBe 1

    val error = field.error.value
    error.message mustEqual "error.nonNumeric"
    error.key mustEqual "value"
  }

  "must fail to bind when the feature is missing" in {

    val boundForm = form.bind(Map.empty[String, String])
    val field = boundForm("value")

    field.errors.length mustBe 1

    val error = field.error.value

    error.message mustEqual "error.required"
    error.key mustEqual "value"
  }

  "must fail when feature is blank" in {
    val boundForm = form.bind(Map("value" -> "  "))
    val field = boundForm("value")

    field.errors.length mustBe 1

    val error = field.error.value

    error.message mustEqual "error.required"
    error.key mustEqual "value"
  }

