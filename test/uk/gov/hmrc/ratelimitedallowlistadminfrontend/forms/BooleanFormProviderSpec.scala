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

class BooleanFormProviderSpec extends AnyWordSpec, Matchers, OptionValues:

  val form = BooleanFormProvider()()

  "must bind when given valid data" in {
    val boundForm = form.bind(Map("value" -> "true"))

    boundForm.errors mustBe empty
    boundForm.value.value mustEqual true

    val boundForm2 = form.bind(Map("value" -> "false"))

    boundForm2.errors mustBe empty
    boundForm2.value.value mustEqual false
  }
  
  "must fail to bind when value is not a boolean" in {
    val boundForm = form.bind(Map("value" -> "foo"))
    val field = boundForm("value")
    field.errors.length mustBe 1

    val error = field.error.value
    error.message mustEqual "error.boolean"
    error.key mustEqual "value"

    val boundForm2 = form.bind(Map("value" -> "1"))
    val field2 = boundForm2("value")
    field2.errors.length mustBe 1

    val error2 = field2.error.value
    error2.message mustEqual "error.boolean"
    error2.key mustEqual "value"
  }

  "must fail to bind when the value is missing" in {

    val boundForm = form.bind(Map.empty[String, String])
    val field = boundForm("value")

    field.errors.length mustBe 1

    val error = field.error.value

    error.message mustEqual "error.required"
    error.key mustEqual "value"
  }

  "must fail when value is blank" in {
    val boundForm = form.bind(Map("value" -> "  "))
    val field = boundForm("value")

    field.errors.length mustBe 1

    val error = field.error.value

    error.message mustEqual "error.required"
    error.key mustEqual "value"
  }

