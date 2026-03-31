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
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.ratelimitedallowlistadminfrontend.connectors.RateLimitedAllowListConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IndexControllerSpec extends AnyWordSpec, Matchers, GuiceOneAppPerSuite, OptionValues, MockitoSugar, BeforeAndAfterEach, ScalaFutures:
  
  private val stubBehaviour = mock[StubBehaviour]
  private val connectorMock = mock[RateLimitedAllowListConnector]

  override def fakeApplication(): Application =
    val frontendAuthComponents = FrontendAuthComponentsStub(stubBehaviour)(stubControllerComponents(), global)
    new GuiceApplicationBuilder()
      .overrides(
        bind[FrontendAuthComponents].toInstance(frontendAuthComponents),
        bind[RateLimitedAllowListConnector].toInstance(connectorMock),
      )
      .build()

  given Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override def beforeEach(): Unit =
    super.beforeEach()
    Mockito.reset(stubBehaviour, connectorMock)

  "GET /" should:
    "must display the page when the user is authorised" in:
      val serviceNames = List("service-1", "service-2")
      when(stubBehaviour.stubAuth(any(), any())).thenReturn(Future.unit)
      when(connectorMock.getServices()(using any())).thenReturn(Future.successful(serviceNames))

      val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
        .withSession("authToken" -> "Token some-token")

      val result = route(app, request).value

      status(result) mustBe OK
      val html = Jsoup.parse(contentAsString(result))
      val services = Option(html.getElementById("services-section")).value

      val list = Option(services.getElementById("services-list")).value
      list.nodeName() mustEqual "ul"

      import scala.jdk.CollectionConverters.*

      val elements = list.getElementsByTag("li").assertNotNull.asScala.toList
                       .flatMap(_.getElementsByTag("a").assertNotNull.asScala.toList)

      elements.size mustEqual serviceNames.size
      elements.zip(serviceNames).foreach {
        case (elem, expectedServiceName) =>
          elem.text() must include(expectedServiceName)
      }

    "must fail when the user is not authenticated (no auth token)" in:
      val request = FakeRequest(GET, routes.IndexController.onPageLoad().url) 
      val result = route(app, request).value
      status(result) mustBe SEE_OTHER

    "must fail when the user is not authorised" in:
      when(stubBehaviour.stubAuth[String](any(), any())).thenReturn(Future.failed(new RuntimeException()))
      val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)
        .withSession("authToken" -> "Token some-token")

      route(app, request).value.failed.futureValue
