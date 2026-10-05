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
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.*
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.Timeframe.Daily

import java.time.{Instant, LocalDate}

class RateLimitedAllowListConnectorSpec extends AnyFreeSpec, Matchers, GuiceOneAppPerSuite, WireMockSupport, ScalaFutures, IntegrationPatience, OptionValues {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.rate-limited-allow-list.port" -> server.port(),
      )
      .build()

  private lazy val connector = app.injector.instanceOf[RateLimitedAllowListConnector]
  private lazy val server = wireMockServer

  private val config = AllowListConfiguration(
    service = "service-name",
    feature = "allow-list-1",
    isEnabled = true,
    userLimitPerTimeframe = 10,
    timeframe = Daily,
    userLimit = Some(100),
    percentageLoad = 10,
    acceptedCounter = 125,
    created = Instant.now,
    lastUpdated = Instant.now
  )

  ".getServices" - {
    val url = "/rate-limited-allow-list/v2/services"
    val hc = HeaderCarrier()

    "for admin - return a list of allow lists for a service when the server responds with OK" in {
      val validResponse = List("service-1", "service-2", "service-3")

      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("permission", equalTo("admin"))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getServices("admin")(using hc).futureValue
      result mustEqual validResponse
    }

    "for read - return a list of allow lists for a service when the server responds with OK" in {
      val validResponse = List("service-1", "service-2", "service-3")

      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("permission", equalTo("read"))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getServices("read")(using hc).futureValue
      result mustEqual validResponse
    }

    "for admin or read - return an empty list when the server response with  NoContent" in {
      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("permission", equalTo("read"))
          .willReturn(
            aResponse().withStatus(NO_CONTENT)
          )
      )

      val result = connector.getServices("read")(using hc).futureValue
      result mustEqual List.empty
    }

    "must fail when the server responds with anything else" in {
      server.stubFor(
        get(urlPathEqualTo(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getServices("admin")(using hc).failed.futureValue
    }
  }

  ".createAllowList" - {
    val url = "/rate-limited-allow-list/v2/services/service/allow-lists"
    val hc = HeaderCarrier()
    val request = CreateAllowListRequest(
      feature = "allow-list-name",
      userLimitPerTimeframe = 0,
      timeframe = Timeframe.Weekly,
      userLimit = None,
      percentageLoad = 0
    )

    "must return the number of tokens when the server responds with OK" in {
      server.stubFor(
        post(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(CREATED))
      )

      connector.createAllowList("service", request.allowList)(using hc).futureValue
    }

    "must fail when the server responds with anything else" in {
      server.stubFor(
        post(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.createAllowList("service", request.allowList)(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {
      server.stubFor(
        post(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.createAllowList("service", request.allowList)(using hc).failed.futureValue
    }
  }

  ".getAllowListConfig" - {
    val serviceName = "service-name"
    val allowListName = "test-allow-list-value"
    val url = s"/rate-limited-allow-list/v2/services/$serviceName/allow-lists/$allowListName"
    val hc = HeaderCarrier()

    "must return the allow list when the server responds with OK" in {
      val validResponse = config.copy(feature = "allow-list-1", isEnabled = true)

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getAllowListConfig(serviceName, allowListName)(using hc).futureValue
      result.value mustEqual validResponse
    }

    "must return an None when the server responds with Not found" in {

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      val result = connector.getAllowListConfig(serviceName, allowListName)(using hc).futureValue
      result mustEqual None
    }

    "must fail when the server responds with anything else" in {
      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getAllowListConfig(serviceName, allowListName)(using hc).failed.futureValue
    }
  }

  ".updateAllowListConfig" - {
    val serviceName = "service-name"
    val allowListName = "test-allow-list-value"
    val url = s"/rate-limited-allow-list/v2/services/$serviceName/allow-lists/$allowListName"
    val hc = HeaderCarrier()

    val request = AllowListConfigUpdate(
      userLimitPerTimeframe = Some(10),
      timeframe = Some(Daily),
      userLimit = Some(100),
      percentageLoad = Some(50),
      isEnabled = Some(true)
    )

    "must be successful when updating an allow list and the server responds with NO_CONTENT" in {
      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.updateAllowListConfig(serviceName, allowListName, request)(using hc).futureValue
    }

    "must be successful when updating an allow list and the server responds with OK" in {
      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(OK))
      )

      connector.updateAllowListConfig(serviceName, allowListName, request)(using hc).futureValue
    }

    "must fail when the server responds with anything else" in {
      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.updateAllowListConfig(serviceName, allowListName, request)(using hc).failed.futureValue
    }

    "must fail when the server connection fails" in {
      server.stubFor(
        patch(urlMatching(url))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(request))))
          .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      )

      connector.updateAllowListConfig(serviceName, allowListName, request)(using hc).failed.futureValue
    }
  }

  ".getAllowLists" - {
    val serviceName = "service-name"
    val url = s"/rate-limited-allow-list/v2/services/$serviceName"
    val hc = HeaderCarrier()

    "must return the allow lists for service when the server responds with OK" in {
      val validResponse = List(
        config.copy(feature = "allow-list-1", isEnabled = true),
        config.copy(feature = "allow-list-1", isEnabled = false)
      )

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getAllowLists(serviceName)(using hc).futureValue
      result mustEqual validResponse
    }

    "must return the allow lists for service when the server responds with OK with empty response" in {
      val validResponse = List.empty[AllowListConfiguration]

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(validResponse)))
          )
      )

      val result = connector.getAllowLists(serviceName)(using hc).futureValue
      result mustEqual validResponse
    }

    "must return an empty list for service when the server responds with Not found" in {
      val validResponse = List.empty[AllowListConfiguration]

      server.stubFor(
        get(urlMatching(url))
          .willReturn(
            aResponse().withStatus(NOT_FOUND)
          )
      )

      val result = connector.getAllowLists(serviceName)(using hc).futureValue
      result mustEqual validResponse
    }

    "must fail when the server responds with anything else" in {
      server.stubFor(
        get(urlMatching(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getAllowLists(serviceName)(using hc).failed.futureValue
    }
  }
 
  ".getAllowListReport" - {
    val serviceName = "service-name"
    val allowListName = "test-allow-list-value"
    val url = s"/rate-limited-allow-list/v2/services/$serviceName/allow-lists/$allowListName/report"
    val freqKey = "frequency"
    val freqValue = "daily"
    val hc = HeaderCarrier()

    "must return the metadata for the service's feature when the server responds with OK" in {
      val validResponse = AllowListReport(
        serviceName,
        allowListName,
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

      val result = connector.getAllowListReport(serviceName, allowListName)(using hc).futureValue
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

      val result = connector.getAllowListReport(serviceName, allowListName)(using hc).futureValue
      result must be(empty)
    }

    "must fail when the server responds with anything else" in {

      server.stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam(freqKey, equalTo(freqValue))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      connector.getAllowListReport(serviceName, allowListName)(using hc).failed.futureValue
    }
  }

}
