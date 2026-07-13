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

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Done

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class SetNewUserLimitControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures:

  private val stubBehaviour = mock[StubBehaviour]
  private val mockConnector = mock[RateLimitedAllowListConnector]
  private val retrievalResult = true

  val validAnswer = 0

  private val service = "fake-frontend"
  private val feature = "fake-feature"

  val onPageLoad = routes.SetNewUserLimitController.onPageLoad(service, feature)
  lazy val onSubmit = routes.SetNewUserLimitController.onSubmit(service, feature)

  override def fakeApplication(): Application =
    val frontendAuthComponents = FrontendAuthComponentsStub(stubBehaviour)(stubControllerComponents(), global)
    new GuiceApplicationBuilder()
      .overrides(
        bind[FrontendAuthComponents].toInstance(frontendAuthComponents),
        bind[RateLimitedAllowListConnector].toInstance(mockConnector)
      )
      .build()

  def messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override def beforeEach(): Unit =
    super.beforeEach()
    Mockito.reset(stubBehaviour, mockConnector)

  "GET" should :

    "return OK and the correct view for a GET" in:
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

      val request = FakeRequest(onPageLoad).withSession("authToken" -> "Token some-token")
      val result = route(app, request).value

      status(result) mustEqual OK

      val html = Jsoup.parse(contentAsString(result))
      val formElems = html.getElementsByTag("form")
      formElems.size() mustEqual 1

      val form = formElems.get(0)
      form.attributes().get("action") mustEqual onSubmit.url

    "must show the user an unauthorized screen when they are authenticated but do not have access" in :
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(false))

      val request = FakeRequest(onPageLoad).withSession("authToken" -> "Token some-token")
      val result = route(app, request).value

      status(result) mustBe UNAUTHORIZED
      val html = Jsoup.parse(contentAsString(result))

      val h1 = html.getElementsByTag("h1")
      h1.size() mustEqual 1
      h1.text() must include(messages("rlal.unauthorised.heading"))

    "must fail when the user is not authenticated (no auth token)" in :
      val request = FakeRequest(onPageLoad)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must include("/internal-auth-frontend/sign-in")

    "must fail when the user is not authorised" in :
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(onPageLoad)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue

  "POST" should:
    "redirect when the value is valid and submission is successful" in:
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))
      when(mockConnector.setTokens(any(), any(), any())(using any())).thenReturn(Future.successful(Done))

      val request = FakeRequest(POST, onSubmit.url)
        .withSession("authToken" -> "Token some-token")
        .withFormUrlEncodedBody("value" -> "100")

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.AllowListSummaryController.root(service, feature).url
      
      val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      flash(result).get("rlal-notification").value mustEqual messages("rlal.set_new.flash.success", feature)


    "return a Bad Request and errors when invalid data is submitted and rerender the form" in:
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

      val request = FakeRequest(onSubmit)
        .withSession("authToken" -> "Token some-token")
        .withFormUrlEncodedBody("value" -> "-100")

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
 
      val html = Jsoup.parse(contentAsString(result))
      html.getElementsByTag("form").size() mustEqual 1

    "must show the user an unauthorized screen when they are authenticated but do not have access" in :
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(false))

      val request = FakeRequest(onSubmit)
        .withSession("authToken" -> "Token some-token")
        .withFormUrlEncodedBody("value" -> "false")

      val result = route(app, request).value

      status(result) mustBe UNAUTHORIZED
      val html = Jsoup.parse(contentAsString(result))

      val h1 = html.getElementsByTag("h1")
      h1.size() mustEqual 1
      h1.text() must include(messages("rlal.unauthorised.heading"))

    "fail when the user is not authenticated (no auth token)" in :
      val request = FakeRequest(onSubmit)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must include("/internal-auth-frontend/sign-in")

    "fail when the user is not authorised" in :
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(onSubmit)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
