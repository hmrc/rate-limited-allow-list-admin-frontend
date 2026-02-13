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
  
  val summaryList: Seq[FeatureSummary] = List(
    FeatureSummary(service, feature1, 10, true),
    FeatureSummary(service, feature2, 20, false)
  )

  override def fakeApplication(): Application =
    val frontendAuthComponents = FrontendAuthComponentsStub(stubBehaviour)(stubControllerComponents(), global)
    new GuiceApplicationBuilder()
      .overrides(
        bind[FrontendAuthComponents].toInstance(frontendAuthComponents),
        bind[RateLimitedAllowListConnector].toInstance(mockConnector)
      )
      .build()

  given Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  
  override def beforeEach(): Unit =
    super.beforeEach()
    Mockito.reset(stubBehaviour)

  "GET /" should:
    "must display the page when the user is authorised and there are features for the service" in:
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatures(any())(using any())).thenReturn(Future.successful(summaryList))

      val request = FakeRequest(GET, routes.ServiceSummaryController.onPageLoad(service).url)
        .withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByAttributeValue("data-test-role","service-summary-row").size() mustEqual summaryList.size
      html.getElementsByAttributeValue("data-test-role", "admin-actions-list").size()  mustEqual summaryList.size

      summaryList.foreach:
        case FeatureSummary(feature, _) =>
          // Option(html.getElementById(s"$service-$feature-stop-onboarding")).value.attributes().get("href") mustEqual routes.IndexController.stopOnboardingUsers(service, feature).url
          // Option(html.getElementById(s"$service-$feature-start-onboarding")).value.attributes().get("href") mustEqual routes.IndexController.startOnboardingUser(service, feature).url
          Option(html.getElementById(s"$service-$feature-increase-new-user-limit")).value.attributes().get("href") mustEqual routes.IncreaseNewUserLimitController.onPageLoad(service, feature).url
          Option(html.getElementById(s"$service-$feature-set-new-user-limit")).value.attributes().get("href") mustEqual routes.IndexController.setNewUserLimit(service, feature).url

    "must display the page when the user is authorised and there are no features for the service" in :
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatures(any())(using any())).thenReturn(Future.successful(List.empty))

      val request = FakeRequest(GET, routes.ServiceSummaryController.onPageLoad(service).url)
        .withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByAttributeValue("data-test-role", "service-summary-row").size() mustEqual 0
      html.getElementsByAttributeValue("data-test-role", "admin-actions-list").size() mustEqual 0
      Option(html.getElementById("no-features")) must not be(empty)
    

    "must fail when the user is not authenticated (no auth token)" in:
      val request = FakeRequest(GET, routes.ServiceSummaryController.onPageLoad(service).url)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER

    "must fail when the user is not authorised" in:
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(GET, routes.ServiceSummaryController.onPageLoad(service).url)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
