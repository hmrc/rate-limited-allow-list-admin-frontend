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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, verify, when}
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
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Timeframe.{Daily, Unbounded}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{AllowListConfigUpdate, Done}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class SetRollingLimitControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures:

  private val stubBehaviour = mock[StubBehaviour]
  private val mockConnector = mock[RateLimitedAllowListConnector]
  private val retrievalResult = true

  val validAnswer = 0

  private val service = "fake-frontend"
  private val feature = "fake-feature"

  private def onPageLoadNone = routes.SetRollingLimitController.onPageLoad(service, feature, None)
  private def onPageLoadDaily = routes.SetRollingLimitController.onPageLoad(service, feature, Some(Daily))
  private def onPageLoadUnbounded = routes.SetRollingLimitController.onPageLoad(service, feature, Some(Unbounded))
  private def onSubmitNone = routes.SetRollingLimitController.onSubmit(service, feature, None)
  private def onSubmitDaily = routes.SetRollingLimitController.onSubmit(service, feature, Some(Daily))
  private def onSubmitUnbound = routes.SetRollingLimitController.onSubmit(service, feature, Some(Unbounded))

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

  def routeWithAuthorizationChecks(endpoint: Call) = {
    "must show the user an unauthorized screen when they are authenticated but do not have access" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(false))

      val request = FakeRequest(endpoint).withSession("authToken" -> "Token some-token")
      val result = route(app, request).value

      status(result) mustBe UNAUTHORIZED
      val html = Jsoup.parse(contentAsString(result))

      val h1 = html.getElementsByTag("h1")
      h1.size() mustEqual 1
      h1.text() must include(messages("rlal.unauthorised.heading"))
    }
  }

  def routeWithAuthenticationChecks(endpoint: Call) = {
    "must fail when the user is not authenticated (no auth token)" in {
      val request = FakeRequest(endpoint)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must include("/internal-auth-frontend/sign-in")
    }

    "must fail when authentication checks fail" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(endpoint)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
    }
  }

  "when there is no timeframe parameter" should {
    "GET" should {
      "return OK and the correct view for a GET" in {
        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

        val request = FakeRequest(onPageLoadNone).withSession("authToken" -> "Token some-token")
        val result = route(app, request).value

        status(result) mustEqual OK

        val html = Jsoup.parse(contentAsString(result))
        val formElems = html.getElementsByTag("form")
        formElems.size() mustEqual 1

        val form = formElems.get(0)
        form.attributes().get("action") mustEqual onSubmitNone.url
      }

      behave like routeWithAuthenticationChecks(onPageLoadNone)
      behave like routeWithAuthorizationChecks(onPageLoadNone)
    }

    "POST" should {
      "redirect when the value is `Unbounded` and submission is successful" in {
        val value = Unbounded

        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))
        when(
          mockConnector.updateAllowListConfig(any(), any(), eqTo(AllowListConfigUpdate(timeframe = Some(value))
          ))(using any())).thenReturn(Future.successful(Done))

        val request = FakeRequest(onSubmitNone)
          .withSession("authToken" -> "Token some-token")
          .withFormUrlEncodedBody("value" -> value.toString)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AllowListSummaryController.root(service, feature).url

        val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
        flash(result).get("rlal-notification").value mustEqual messages("rlal.set_rolling_limit_value.flash.success.unbound", feature)
      }

      "redirect to additional pages when the value is not `Unbounded` and submission is successful" in {
        val value = Daily

        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))
        when(
          mockConnector.updateAllowListConfig(any(), any(), eqTo(AllowListConfigUpdate(timeframe = Some(value))
          ))(using any())).thenReturn(Future.successful(Done))

        val request = FakeRequest(onSubmitNone)
          .withSession("authToken" -> "Token some-token")
          .withFormUrlEncodedBody("value" -> value.toString)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onPageLoadDaily.url

        flash(result).get("rlal-notification") must be(empty)
      }

      "return a Bad Request and errors when invalid data is submitted and rerender the form" in {
        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

        val request = FakeRequest(onSubmitNone)
          .withSession("authToken" -> "Token some-token")
          .withFormUrlEncodedBody("value" -> "-100")

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST

        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByTag("form").size() mustEqual 1
      }

      behave like routeWithAuthenticationChecks(onSubmitNone)
      behave like routeWithAuthorizationChecks(onSubmitNone)
    }
  }

  "when there is a timeframe parameter" should {
    "GET" should {
      "return a redirect to main page when the parameter is Unbounded with error" in {
        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

        val request = FakeRequest(onPageLoadUnbounded).withSession("authToken" -> "Token some-token")
        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST

        val html = Jsoup.parse(contentAsString(result))
        val formElems = html.getElementsByTag("form")
        formElems.size() mustEqual 1

        val form = formElems.get(0)
        form.attributes().get("action") mustEqual onSubmitNone.url
      }

      "return OK and the correct view for a GET" in {
        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

        val request = FakeRequest(onPageLoadDaily).withSession("authToken" -> "Token some-token")
        val result = route(app, request).value

        status(result) mustEqual OK

        val html = Jsoup.parse(contentAsString(result))
        val formElems = html.getElementsByTag("form")
        formElems.size() mustEqual 1

        val form = formElems.get(0)
        form.attributes().get("action") mustEqual onSubmitDaily.url
      }

      behave like routeWithAuthenticationChecks(onPageLoadDaily)
      behave like routeWithAuthorizationChecks(onPageLoadDaily)
    }

    "POST" should {
      "redirect when submission is successful" in {
        val value = 10
        val update = AllowListConfigUpdate(timeframe = Some(Daily), userLimitPerTimeframe = Some(value))

        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))
        when(
          mockConnector.updateAllowListConfig(any(), any(), eqTo(update))(using any())
        ).thenReturn(Future.successful(Done))

        val request = FakeRequest(onSubmitDaily)
          .withSession("authToken" -> "Token some-token")
          .withFormUrlEncodedBody("value" -> value.toString)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AllowListSummaryController.root(service, feature).url

        val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
        flash(result).get("rlal-notification").value mustEqual messages("rlal.set_rolling_limit_value.flash.success.plural", value, Daily.value)
      }

      "redirect to fist page when the timeframe is unbounded" in {
        val value = 10

        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))
        verify(mockConnector, never()).updateAllowListConfig(any(), any(), any())(using any())

        val request = FakeRequest(onSubmitUnbound)
          .withSession("authToken" -> "Token some-token")
          .withFormUrlEncodedBody("value" -> value.toString)

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST

        val html = Jsoup.parse(contentAsString(result))
        val formElems = html.getElementsByTag("form")
        formElems.size() mustEqual 1

        val form = formElems.get(0)
        form.attributes().get("action") mustEqual onSubmitNone.url
      }

      "return a Bad Request and errors when invalid data is submitted and rerender the form" in {
        when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(retrievalResult))

        val request = FakeRequest(onSubmitDaily)
          .withSession("authToken" -> "Token some-token")
          .withFormUrlEncodedBody("value" -> "-100")

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST

        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByTag("form").size() mustEqual 1
      }

      behave like routeWithAuthenticationChecks(onSubmitDaily)
      behave like routeWithAuthorizationChecks(onSubmitDaily)
    }
  }
