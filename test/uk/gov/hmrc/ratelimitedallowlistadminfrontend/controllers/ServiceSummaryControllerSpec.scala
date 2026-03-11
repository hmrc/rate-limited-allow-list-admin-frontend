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
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, Resource}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.FeatureSummary

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceSummaryControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures:

  private val stubBehaviour = mock[StubBehaviour]
  private val mockConnector = mock[RateLimitedAllowListConnector]
  private val resources = List(
    Resource.from("rate-limited-allow-list-admin-frontend", "bar"),
    Resource.from("rate-limited-allow-list-admin-frontend", "foo")
  )

  val service = "fake-frontend"
  val feature1 = "feature 1"
  val feature2 = "feature 2"
  val feature3 = "feature 3"

  val summary1 = FeatureSummary(service, feature1, 10, true)
  val summary2 = FeatureSummary(service, feature2, 20, false)
  val summary3 = FeatureSummary(service, feature3, 30, true)

  override def fakeApplication(): Application =
    val frontendAuthComponents = FrontendAuthComponentsStub(stubBehaviour)(stubControllerComponents(), global)
    new GuiceApplicationBuilder()
      .overrides(
        bind[FrontendAuthComponents].toInstance(frontendAuthComponents),
        bind[RateLimitedAllowListConnector].toInstance(mockConnector)
      )
      .build()

  given messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override def beforeEach(): Unit =
    super.beforeEach()
    Mockito.reset(stubBehaviour)

  private def url: Call = routes.ServiceSummaryController.onPageLoad(service)

  "GET /" should {
    "must display the page when the user is authorised and there are allow lists for the service" in {
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatures(any())(using any())).thenReturn(Future.successful(List(summary3, summary2, summary1)))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      val runningSection = Option(html.getElementById("allow-lists-running")).value
      val pausedSection = Option(html.getElementById("allow-lists-paused")).value

      val runningAllowListsList = runningSection.getElementsByTag("li")
      runningAllowListsList.size() mustEqual 2
      runningAllowListsList.get(0).text() must include(feature1)
      runningAllowListsList.get(1).text() must include(feature3)

      val pausedAllowListsList = pausedSection.getElementsByTag("li")
      pausedAllowListsList.size() mustEqual 1
      pausedAllowListsList.get(0).text() must include(feature2)
    }

    "must display the page when the user is authorised and all allow lists are running" in {
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatures(any())(using any())).thenReturn(Future.successful(List(summary3, summary1)))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      val runningSection = Option(html.getElementById("allow-lists-running")).value
      val pausedSection = Option(html.getElementById("allow-lists-paused")).value

      val runningAllowListsList = runningSection.getElementsByTag("li")
      runningAllowListsList.size() mustEqual 2
      runningAllowListsList.get(0).text() must include(feature1)
      runningAllowListsList.get(1).text() must include(feature3)

      val pausedAllowListsList = pausedSection.getElementsByTag("li")
      pausedAllowListsList.size() mustEqual 0
      Option(pausedSection.getElementById("allow-list-paused-empty")).value.text() must include(
        messages("rlal.service_summary.allow_list_paused_empty")
      )
    }

    "must display the page when the user is authorised and all allow lists are paused" in {
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatures(any())(using any())).thenReturn(Future.successful(List(summary2)))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      val runningSection = Option(html.getElementById("allow-lists-running")).value
      val pausedSection = Option(html.getElementById("allow-lists-paused")).value

      val runningAllowListsList = runningSection.getElementsByTag("li")
      runningAllowListsList.size() mustEqual 0
      Option(runningSection.getElementById("allow-list-running-empty")).value.text() must include(
        messages("rlal.service_summary.allow_list_running_empty")
      )

      val pausedAllowListsList = pausedSection.getElementsByTag("li")
      pausedAllowListsList.size() mustEqual 1
      pausedAllowListsList.get(0).text() must include(feature2)
    }

    "must display the page when the user is authorised and there are no allow lists for the service" in {
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatures(any())(using any())).thenReturn(Future.successful(List.empty))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe 404

      val html = Jsoup.parse(contentAsString(result))

      val runningSection = Option(html.getElementById("allow-lists-running")).value
      val pausedSection = Option(html.getElementById("allow-lists-paused")).value

      Option(runningSection.getElementById("allow-list-running-empty")).value.text() must include(
        messages("rlal.service_summary.allow_list_running_empty")
      )
      Option(pausedSection.getElementById("allow-list-paused-empty")).value.text() must include(
        messages("rlal.service_summary.allow_list_paused_empty")
      )
    }

    "must fail when the user is not authenticated (no auth token)" in {
      val request = FakeRequest(url)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER
    }

    "must fail when the user is not authorised" in {
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(url)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
    }
  }
