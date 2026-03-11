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

package uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.*

import java.time.LocalDate

class RateLimitedAllowListConnectorSpec extends AnyFreeSpec, Matchers, GuiceOneAppPerSuite, WireMockSupport, ScalaFutures, IntegrationPatience {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.rate-limited-allow-list.port" -> server.port(),
      )
      .build()

  private lazy val connector = app.injector.instanceOf[RateLimitedAllowListConnector]
  private lazy val server = wireMockServer

  ".getFeatures" - {
    
    val url = "/rate-limited-allow-list/services/service/features"
    val hc = HeaderCarrier()

    "must return the metadata for all the service's features when the server responds with OK" in {
      val validResponse = List(
        FeatureSummary("service", "feature-1", 10, true),
        FeatureSummary("service", "feature-2", 20, false)
      )

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result: Seq[FeatureSummary] = connector.getFeatures("service")(using hc).futureValue
      result mustEqual validResponse
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getFeatures("service")(using hc).failed.futureValue
    }
  }
 
  ".getFeatureMetadata" - {
    val feature = "test-feature-value"
    val url = "/rate-limited-allow-list/services/service/features/test-feature-value/metadata"
    val hc = HeaderCarrier()

    "must return the metadata for the service's feature when the server responds with OK" in {
      val validResponse = FeatureSummary("service", "feature-1", 10, true)

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getFeatureMetadata("service", feature)(using hc).futureValue
      result mustEqual Some(validResponse)
    }

    "must return a None when the server responds with 404" in {
      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(404)
          )
      )

      val result = connector.getFeatureMetadata("service", feature)(using hc).futureValue
      result must be(empty)
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getFeatureMetadata("service", feature)(using hc).failed.futureValue
    }
  }

  ".getFeatureReport" - {
    val feature = "test-feature-value"
    val url = "/rate-limited-allow-list/services/service/features/test-feature-value/report"
    val freqKey = "frequency"
    val freqValue = "daily"
    val hc = HeaderCarrier()

    "must return the metadata for the service's feature when the server responds with OK" in {
      val validResponse = AllowListReport(
        "service",
        "feature-1",
        100, 
        List(
          DailyReport(LocalDate.now(), 11),
          DailyReport(LocalDate.now(), 12)
        )
      )

      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(freqKey, equalTo(freqValue))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getFeatureReport("service", feature)(using hc).futureValue
      result mustEqual Some(validResponse)
    }

    "must return a None when the server responds with 404" in {
      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(freqKey, equalTo(freqValue))
          .willReturn(
            aResponse().withStatus(404)
          )
      )

      val result = connector.getFeatureReport("service", feature)(using hc).futureValue
      result must be(empty)
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(freqKey, equalTo(freqValue))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getFeatureReport("service", feature)(using hc).failed.futureValue
    }
  }
    
  ".addTokens" - {

    val url = "/rate-limited-allow-list/services/service/features/feature/metadata/tokens"
    val hc = HeaderCarrier()
    val request = TokenRequest(123)

    "must return the number of tokens when the server responds with OK" in {

      server.stubFor(
        post(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(OK))
      )

      connector.addTokens("service", "feature", 123)(using hc).futureValue
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        post(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.addTokens("service", "feature", 123)(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {

      server.stubFor(
        post(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.addTokens("service", "feature", 123)(using hc).failed.futureValue
    }
  }

  ".setTokens" - {

    val url = "/rate-limited-allow-list/services/service/features/feature/metadata"
    val hc = HeaderCarrier()
    val request = TokenRequest(123)

    "must return the number of tokens when the server responds with OK" in {

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.setTokens("service", "feature", 123)(using hc).futureValue
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.setTokens("service", "feature", 123)(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.setTokens("service", "feature", 123)(using hc).failed.futureValue
    }
  }

  ".setCanIssueTokens" - {

    val url = "/rate-limited-allow-list/services/service/features/feature/metadata"
    val hc = HeaderCarrier()
    val request = IssueTokenStatusUpdateRequest(true)

    "must be succesful when enabling when the server responds with OK" in {

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.setCanIssueTokens("service", "feature", request.canIssueTokens)(using hc).futureValue
    }

    "must be succesful when disabling when the server responds with OK" in {
      val request = IssueTokenStatusUpdateRequest(false)

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.setCanIssueTokens("service", "feature", request.canIssueTokens)(using hc).futureValue
    }


    "must fail when the server responds with anything else" in {

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.setCanIssueTokens("service", "feature", request.canIssueTokens)(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {

      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.setCanIssueTokens("service", "feature", request.canIssueTokens)(using hc).failed.futureValue
    }
  }

  ".availableTokens" - {

    val url = "/rate-limited-allow-list/services/service/features/feature/tokens"
    val hc = HeaderCarrier()
    val validResponse = TokenResponse(123)

    "must return the number of tokens when the server responds with OK" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse))))
      )

      connector.availableTokens("service", "feature")(using hc).futureValue mustBe validResponse
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.availableTokens("service", "feature")(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.availableTokens("service", "feature")(using hc).failed.futureValue
    }
  }

}
