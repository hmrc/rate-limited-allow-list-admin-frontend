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
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, Resource}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{Done, FeatureSummary}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class ToggleNewUserOnboardingControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures:

  private val stubBehaviour = mock[StubBehaviour]
  private val mockConnector = mock[RateLimitedAllowListConnector]
  private val resources = List(
    Resource.from("rate-limited-allow-list-admin-frontend", "bar"),
    Resource.from("rate-limited-allow-list-admin-frontend", "foo")
  )

  private val service = "fake-frontend"
  private val feature = "fake-feature"

  val featureSummary = FeatureSummary(service, feature, 20, false)

  val validAnswer = 0

  lazy val onPageLoad = routes.ToggleNewUserOnboardingController.onPageLoad(service, feature)
  lazy val onSubmit = routes.ToggleNewUserOnboardingController.onSubmit(service, feature)

  override def fakeApplication(): Application =
    val frontendAuthComponents = FrontendAuthComponentsStub(stubBehaviour)(stubControllerComponents(), global)
    new GuiceApplicationBuilder()
      .overrides(
        bind[FrontendAuthComponents].toInstance(frontendAuthComponents),
        bind[RateLimitedAllowListConnector].toInstance(mockConnector)
      )
      .build()

  override def beforeEach(): Unit =
    super.beforeEach()
    Mockito.reset(stubBehaviour, mockConnector)

  "GET" should :

    "return OK and the correct view for a GET" in:
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatureMetadata(any(), any())(using any()))
        .thenReturn(Future.successful(Some(featureSummary)))

      val request = FakeRequest(onPageLoad).withSession("authToken" -> "Token some-token")
      val result = route(app, request).value

      status(result) mustEqual OK

      val html = Jsoup.parse(contentAsString(result))
      val formElems = html.getElementsByTag("form")
      formElems.size() mustEqual 1

      val form = formElems.get(0)
      form.attributes().get("action") mustEqual onSubmit.url

    "redirect the user and with flash error to when there is not data" in :
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatureMetadata(any(), any())(using any()))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(onPageLoad).withSession("authToken" -> "Token some-token")
      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.ServiceSummaryController.onPageLoad(service).url

      val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      flash(result).get("rlal-notification").value mustEqual messages("error.feature_not_found", service, feature)

    "must fail when the user is not authenticated (no auth token)" in :
      val request = FakeRequest(onPageLoad)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER

    "must fail when the user is not authorised" in :
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(onPageLoad)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue

  "POST" should:
    "redirect with flash success when the value is valid and submission is successful" in:
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.setCanIssueTokens(any(), any(), any())(using any())).thenReturn(Future.successful(Done))

      val request = FakeRequest(POST, onSubmit.url)
        .withSession("authToken" -> "Token some-token")
        .withFormUrlEncodedBody("value" -> "false")

      val result = route(app, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.ServiceSummaryController.onPageLoad(service).url
      
      val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      flash(result).get("rlal-notification").value mustEqual messages("rlal.toggle.success.paused", feature)

    "return a Bad Request and errors when invalid data is submitted and rerender the form" in:
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatureMetadata(any(), any())(using any()))
        .thenReturn(Future.successful(Some(featureSummary)))

      val request = FakeRequest(onSubmit)
        .withSession("authToken" -> "Token some-token")
        .withFormUrlEncodedBody("value" -> "1")

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
 
      val html = Jsoup.parse(contentAsString(result))
      html.getElementsByTag("form").size() mustEqual 1


    "fail when the user is not authenticated (no auth token)" in:
      val request = FakeRequest(onSubmit)
        .withFormUrlEncodedBody("value" -> "false")
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER

    "fail when the user is not authorised" in:
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(onSubmit)
        .withSession("authToken" -> "Token some-token")
        .withFormUrlEncodedBody("value" -> "false")

      route(app, request).value.failed.futureValue
