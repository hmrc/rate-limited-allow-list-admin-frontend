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

import play.api.{Configuration, Logging}
import play.api.http.Status.{CREATED, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.config.Service
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector.UnexpectedResponseException
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.models.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

@Singleton
class RateLimitedAllowListConnector @Inject()(configuration: Configuration,
                                              httpClient: HttpClientV2
                                             )(implicit ec: ExecutionContext) extends Logging {

  private val rateLimitedAllowListService: Service = configuration.get[Service]("microservice.services.rate-limited-allow-list")

  def getServices(permission: "read" | "admin")(using HeaderCarrier): Future[Seq[String]] =
    httpClient.get(url"$rateLimitedAllowListService/rate-limited-allow-list/v2/services?permission=$permission")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(response.json.as[List[String]])
          case NO_CONTENT => Future.successful(List.empty)
          case status    => Future.failed(UnexpectedResponseException(status))
        }
      }

  def createAllowList(service: String, allowList: String)(using HeaderCarrier): Future[Done] =
    httpClient.post(url"$rateLimitedAllowListService/rate-limited-allow-list/v2/services/$service/allow-lists")
      .withBody(Json.toJson(CreateAllowListRequest.default(allowList)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case CREATED   =>  Future.successful(Done)
          case status    =>
            logger.error(response.body)
            Future.failed(UnexpectedResponseException(status))
        }
      }

  def updateAllowListConfig(service: String, allowList: String, update: AllowListConfigUpdate)(using HeaderCarrier): Future[Done] = {
    httpClient.patch(url"$rateLimitedAllowListService/rate-limited-allow-list/v2/services/$service/allow-lists/$allowList")
      .withBody(Json.toJson(update))
      .execute[HttpResponse]
      .flatMap { 
        _.status match {
          case OK | NO_CONTENT   =>  Future.successful(Done)
          case status    => Future.failed(UnexpectedResponseException(status))
        }
      }
  }

  def getAllowLists(service: String)(using HeaderCarrier): Future[Seq[AllowListConfiguration]] =
    httpClient.get(url"$rateLimitedAllowListService/rate-limited-allow-list/v2/services/$service")
      .execute[Option[List[AllowListConfiguration]]]
      .map(_.getOrElse(List.empty))

  def getAllowList(service: String, allowList: String)(using HeaderCarrier): Future[Option[AllowListConfiguration]] =
    httpClient.get(url"$rateLimitedAllowListService/rate-limited-allow-list/v2/services/$service/allow-lists/$allowList")
      .execute[Option[AllowListConfiguration]]

  def getAllowListReport(service: String, allowList: String)(using HeaderCarrier): Future[Option[AllowListReport]] =
    httpClient.get(url"$rateLimitedAllowListService/rate-limited-allow-list/v2/services/$service/allow-lists/$allowList/report?frequency=daily")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(Some(response.json.as[AllowListReport]))
          case NOT_FOUND => Future.successful(Option.empty)
          case status    => Future.failed(UnexpectedResponseException(status))
        }
      }

}

object RateLimitedAllowListConnector {

  final case class UnexpectedResponseException(status: Int) extends Exception with NoStackTrace {
    override def getMessage: String = s"Unexpected status: $status"
  }

}
