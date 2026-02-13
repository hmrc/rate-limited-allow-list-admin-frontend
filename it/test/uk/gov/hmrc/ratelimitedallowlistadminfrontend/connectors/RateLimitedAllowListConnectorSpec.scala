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
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.{IssuedTokensResponse, IssuedTokensSummary, TokenRequest, TokenResponse, FeatureSummary}
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.util.WireMockHelper

import java.time.LocalDate

class RateLimitedAllowListConnectorSpec extends AnyFreeSpec with Matchers with ScalaFutures with IntegrationPatience with WireMockHelper {

  private lazy val app: Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.rate-limited-allow-list.port" -> server.port(),
      )
      .build()

  private lazy val connector = app.injector.instanceOf[RateLimitedAllowListConnector]

  ".getFeatures" - {
    
    val url = "/rate-limited-allow-list/services/service/features"
    val hc = HeaderCarrier()

    "must return the number of tokens when the server responds with OK" in {
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
          .willReturn(aResponse().withStatus(OK))
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

  ".issuedTokens" - {

    val url = "/rate-limited-allow-list/services/service/features/feature/issued-tokens"
    val hc = HeaderCarrier()
    val validResponse = IssuedTokensResponse(List(IssuedTokensSummary(LocalDate.now, 1)))

    "must return the number of tokens when the server responds with OK" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse))))
      )

      connector.issuedTokens("service", "feature")(using hc).futureValue mustBe validResponse
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.issuedTokens("service", "feature")(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.issuedTokens("service", "feature")(using hc).failed.futureValue
    }
  }
}
