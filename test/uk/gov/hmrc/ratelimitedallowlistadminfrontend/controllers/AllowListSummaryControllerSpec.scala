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
import org.jsoup.select.Elements
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
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{AllowListReport, FeatureSummary}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AllowListSummaryControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures:
  
  private val stubBehaviour = mock[StubBehaviour]
  private val mockConnector = mock[RateLimitedAllowListConnector]
  private val resources = List(
    Resource.from("rate-limited-allow-list-admin-frontend", "bar"),
    Resource.from("rate-limited-allow-list-admin-frontend", "foo")
  )

  val service = "fake-frontend"
  val allowList = "allow list 1"

  val metadata = FeatureSummary(service, allowList, 10, true)
  val currentUserCount = 100
  val report = AllowListReport(service, allowList, currentUserCount, List.empty)

  override def fakeApplication(): Application =
    val frontendAuthComponents = FrontendAuthComponentsStub(stubBehaviour)(stubControllerComponents(), global)
    new GuiceApplicationBuilder()
      .overrides(
        bind[FrontendAuthComponents].toInstance(frontendAuthComponents),
        bind[RateLimitedAllowListConnector].toInstance(mockConnector)
      )
      .build()

  given messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  lazy val url = routes.AllowListSummaryController.onPageLoad(service, allowList)
  
  override def beforeEach(): Unit =
    super.beforeEach()
    Mockito.reset(stubBehaviour)

  "GET /" should:
    "must display the page when the user is authorised and there are features for the service" in:
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(resources))
      when(mockConnector.getFeatureMetadata(any(), any())(using any())).thenReturn(Future.successful(Some(metadata)))
      when(mockConnector.getFeatureReport(any(), any())(using any())).thenReturn(Future.successful(Some(report)))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      val summaryListHtml: Elements = html.getElementsByClass("govuk-summary-list")
      summaryListHtml.size() mustEqual 1

      val rows = summaryListHtml.get(0).getElementsByClass("govuk-summary-list__row")
      rows.size() mustEqual 4

      val currentUserRow = rows.get(0)
      val tokenRow = rows.get(1)
      val totalUsersRow = rows.get(2)
      val statusRow = rows.get(3)

      // Current user count row
      currentUserRow.getElementsByClass("govuk-summary-list__value").text() must include(currentUserCount.toString)
      currentUserRow.getElementsByClass("govuk-summary-list__actions-list-item").size() mustEqual 0

      // Token row
      tokenRow.getElementsByClass("govuk-summary-list__value").text() must include(metadata.tokens.toString)

      val tokenActions = tokenRow.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
      tokenActions.size() mustEqual 1
      tokenActions.get(0).text() must include("Increase")

      // Total row
      totalUsersRow.getElementsByClass("govuk-summary-list__value").text() must include((currentUserCount + metadata.tokens).toString)

//          val totalUsersActions = totalUsersRow.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
//          totalUsersActions.size() mustEqual 1
//          totalUsersActions.get(0).text() must include("Set user count")

      // status row
      val expectedStatusText = if metadata.canIssueTokens then "Yes" else "No"
      statusRow.getElementsByClass("govuk-summary-list__value").text() must include(expectedStatusText)

      val expectedStatusActionText = if metadata.canIssueTokens then "Pause" else "Resume"
      val statusActions = statusRow.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
        statusActions.size() mustEqual 1
        statusActions.get(0).text() must include(expectedStatusActionText)


    "must display the page when the user is authorised and the feature is not found" in :
      when(stubBehaviour.stubAuth[Set[Resource]](any(), any())).thenReturn(Future.successful(None))
      when(mockConnector.getFeatureMetadata(any(), any())(using any())).thenReturn(Future.successful(None))
      when(mockConnector.getFeatureReport(any(), any())(using any())).thenReturn(Future.successful(None))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByClass("govuk-summary-list__row").size() mustEqual 0
      Option(html.getElementById("not-found")).value.text() must include(messages("rlal.allow_list_summary.not_found"))


    "must fail when the user is not authenticated (no auth token)" in:
      val request = FakeRequest(url)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER

    "must fail when the user is not authorised" in:
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
