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

import play.api.Configuration
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.config.Service
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector.UnexpectedResponseException
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

@Singleton
class RateLimitedAllowListConnector @Inject()(
                                               configuration: Configuration,
                                               httpClient: HttpClientV2
                                             )(implicit ec: ExecutionContext) {

  private val rateLimitedAllowListService: Service = configuration.get[Service]("microservice.services.rate-limited-allow-list")

  def addTokens(service: String, feature: String, tokens: Int)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient.post(url"$rateLimitedAllowListService/rate-limited-allow-list/services/$service/features/$feature/tokens")
      .withBody(Json.toJson(TokenRequest(tokens)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(Done)
          case status => Future.failed(UnexpectedResponseException(status))
        }
      }

  def setTokens(service: String, feature: String, tokens: Int)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient.put(url"$rateLimitedAllowListService/rate-limited-allow-list/services/$service/features/$feature/tokens")
      .withBody(Json.toJson(TokenRequest(tokens)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(Done)
          case status => Future.failed(UnexpectedResponseException(status))
        }
      }

  def availableTokens(service: String, feature: String)(implicit hc: HeaderCarrier): Future[TokenResponse] =
    httpClient.get(url"$rateLimitedAllowListService/rate-limited-allow-list/services/$service/features/$feature/tokens")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(response.json.as[TokenResponse])
          case status => Future.failed(UnexpectedResponseException(status))
        }
      }

  def issuedTokens(service: String, feature: String)(implicit hc: HeaderCarrier): Future[IssuedTokensResponse] =
    httpClient.get(url"$rateLimitedAllowListService/rate-limited-allow-list/services/$service/features/$feature/issued-tokens")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK     => Future.successful(response.json.as[IssuedTokensResponse])
          case status => Future.failed(UnexpectedResponseException(status))
        }
      }
}

object RateLimitedAllowListConnector {

  final case class UnexpectedResponseException(status: Int) extends Exception with NoStackTrace {
    override def getMessage: String = s"Unexpected status: $status"
  }
}
