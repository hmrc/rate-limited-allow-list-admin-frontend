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
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.controllers.routes
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Timeframe.Daily
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.AllowListConfiguration

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class AllowListSummaryControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures {

  private val stubBehaviour = mock[StubBehaviour]
  private val mockConnector = mock[RateLimitedAllowListConnector]
  val service = "fake-frontend"
  val allowList = "allow list 1"

  val allowListConfig = AllowListConfiguration(
    service = service,
    feature = allowList,
    isEnabled = true,
    userLimitPerTimeframe = 10,
    timeframe = Daily,
    userLimit = Some(100),
    percentageLoad = 50,
    acceptedCounter = 125,
    created = Instant.now,
    lastUpdated = Instant.now
  )

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
    Mockito.reset(stubBehaviour, mockConnector)

  "GET /" should {
    "when user is an admin for the service, redirect user to the manage endpoint" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(true))

      val request = FakeRequest(routes.AllowListSummaryController.root(service, allowList))
        .withSession("authToken" -> "Token some-token")

      val result = route(app, request).value
      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.AllowListSummaryController.manage(service, allowList).url
    }

    "when user is not an admin for the service, redirect user to the view endpoint" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(false))

      val request = FakeRequest(routes.AllowListSummaryController.root(service, allowList))
        .withSession("authToken" -> "Token some-token")

      val result = route(app, request).value
      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.AllowListSummaryController.view(service, allowList).url
    }
  }

  "GET /manage when user is an admin" should {
    def url = routes.AllowListSummaryController.manage(service, allowList)

    "must display the page when the user is authorised and there are features for the service" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(true))
      when(mockConnector.getAllowListConfig(any(), any())(using any())).thenReturn(Future.successful(Some(allowListConfig)))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      // return the correct status
      status(result) mustBe OK

      // return 2 summary lists
      val summaryListHtml: Elements = html.getElementsByClass("govuk-summary-list")
      summaryListHtml.size() mustEqual 3

      // summary list 1 should display details on the user onboarding
      val userSummaryRows = html.getElementsByClass("govuk-summary-list").get(0).getElementsByClass("govuk-summary-list__row")
      userSummaryRows.size() mustEqual 4

      val currentUserRow :: userPercentage :: rollingUser :: totalUsers :: Nil = userSummaryRows.asScala.toList

      currentUserRow.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.acceptedCounter.toString)
      currentUserRow.getElementsByClass("govuk-summary-list__actions-list-item").size() mustEqual 0

      userPercentage.getElementsByClass("govuk-summary-list__value").text() must include("50%")
      val userPercentageActions = userPercentage.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
      userPercentageActions.size() mustEqual 1
      userPercentageActions.get(0).text() must include("Change")

      rollingUser.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.userLimitPerTimeframe.toString)
      rollingUser.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.timeframe.toString)
      val rollingUserActions = rollingUser.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
      rollingUserActions.size() mustEqual 1
      rollingUserActions.get(0).text() must include("Change")

      totalUsers.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.userLimit.get.toString)
      val totalUsersActions = totalUsers.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
      totalUsersActions.size() mustEqual 1
      totalUsersActions.get(0).text() must include("Change")

      // summary list 2 should display times
      val detailsRows = html.getElementsByClass("govuk-summary-list").get(1).getElementsByClass("govuk-summary-list__row")
      detailsRows.size() mustEqual 3

      val lastUpdatedRow :: createdRow :: validUntilRow :: Nil = detailsRows.asScala.toList

      lastUpdatedRow.getElementsByClass("govuk-summary-list__value").text() must include("GMT")
      createdRow.getElementsByClass("govuk-summary-list__value").text() must include("GMT")
      validUntilRow.getElementsByClass("govuk-summary-list__value").text() must include("GMT")

      // summary list 3 should display
      val manageRows = html.getElementsByClass("govuk-summary-list").get(2).getElementsByClass("govuk-summary-list__row")
      manageRows.size() mustEqual 1

      val onboardingRow = manageRows.get(0)

      onboardingRow.getElementsByClass("govuk-summary-list__value").text() must include("Active")
      val manageActions = onboardingRow.getElementsByClass("govuk-summary-list__actions").get(0).getElementsByTag("a")
      manageActions.size() mustEqual 1
      manageActions.get(0).text() must include("Pause")
    }

    "must display the page when the user is authorised and the feature is not found" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(true))
      when(mockConnector.getAllowListConfig(any(), any())(using any())).thenReturn(Future.successful(None))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByClass("govuk-summary-list__row").size() mustEqual 0
      Option(html.getElementById("not-found")).value.text() must include(messages("rlal.allow_list_summary.not_found"))
    }

    "must redirect a user to sign in when they are are not authenticated (no auth token)" in {
      val request = FakeRequest(url)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must include("/internal-auth-frontend/sign-in")
    }

    "must fail when the user is not authorised" in {
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
    }
  }

  "GET /view when user is not an admin" should {
    def url = routes.AllowListSummaryController.view(service, allowList)

    "must display the page when the user is authorised and there are features for the service" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(false))
      when(mockConnector.getAllowListConfig(any(), any())(using any())).thenReturn(Future.successful(Some(allowListConfig)))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      // return the correct status
      status(result) mustBe OK

      // return 2 summary lists
      val summaryListHtml: Elements = html.getElementsByClass("govuk-summary-list")
      summaryListHtml.size() mustEqual 3

      // summary list 1 should display details on the user onboarding
      val userSummaryRows = html.getElementsByClass("govuk-summary-list").get(0).getElementsByClass("govuk-summary-list__row")
      userSummaryRows.size() mustEqual 4

      val currentUserRow :: userPercentage :: rollingUser :: totalUsers :: Nil = userSummaryRows.asScala.toList

      currentUserRow.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.acceptedCounter.toString)
      currentUserRow.getElementsByClass("govuk-summary-list__actions-list-item").size() mustEqual 0

      userPercentage.getElementsByClass("govuk-summary-list__value").text() must include("50%")
      userPercentage.getElementsByClass("govuk-summary-list__actions").size() mustEqual 0

      rollingUser.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.userLimitPerTimeframe.toString)
      rollingUser.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.timeframe.toString)
      rollingUser.getElementsByClass("govuk-summary-list__actions").size() mustEqual 0

      totalUsers.getElementsByClass("govuk-summary-list__value").text() must include(allowListConfig.userLimit.get.toString)
      totalUsers.getElementsByClass("govuk-summary-list__actions").size() mustEqual 0

      // summary list 2 should display times
      val detailsRows = html.getElementsByClass("govuk-summary-list").get(1).getElementsByClass("govuk-summary-list__row")
      detailsRows.size() mustEqual 3

      val lastUpdatedRow :: createdRow :: validUntilRow :: Nil = detailsRows.asScala.toList

      lastUpdatedRow.getElementsByClass("govuk-summary-list__value").text() must include("GMT")
      createdRow.getElementsByClass("govuk-summary-list__value").text() must include("GMT")
      validUntilRow.getElementsByClass("govuk-summary-list__value").text() must include("GMT")

      // summary list 3 should display
      val manageRows = html.getElementsByClass("govuk-summary-list").get(2).getElementsByClass("govuk-summary-list__row")
      manageRows.size() mustEqual 1

      val onboardingRow = manageRows.get(0)

      onboardingRow.getElementsByClass("govuk-summary-list__value").text() must include("Active")
      onboardingRow.getElementsByClass("govuk-summary-list__actions").size() mustEqual 0
    }

    "must display the page when the user is authorised and the feature is not found" in {
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.successful(false))
      when(mockConnector.getAllowListConfig(any(), any())(using any())).thenReturn(Future.successful(None))

      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByClass("govuk-summary-list__row").size() mustEqual 0
      Option(html.getElementById("not-found")).value.text() must include(messages("rlal.allow_list_summary.not_found"))
    }

    "must redirect a user to sign in when they are are not authenticated (no auth token)" in {
      val request = FakeRequest(url)
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result).value must include("/internal-auth-frontend/sign-in")
    }

    "must fail when the user is not authorised" in {
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(url).withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
    }
  }
}

extension (a: AllowListConfiguration) {
  def expectedDisplayStatus(): String = if a.isEnabled then "Pause" else "Resume"
}

