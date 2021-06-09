/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.entrydeclarationintervention.controllers

import controllers.Assets
import org.scalatest.{Assertion, OptionValues, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import org.scalamock.matchers.Matchers
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.http.MimeTypes
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.entrydeclarationintervention.config.MockAppConfig

class DocumentationControllerSpec extends WordSpecLike with Matchers with OptionValues with MockAppConfig with Injecting with GuiceOneAppPerSuite {
  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure("metrics.enabled" -> "false")
    .build()

  val assets: Assets = inject[Assets]

  val documentationController =
    new DocumentationController(Helpers.stubControllerComponents(), assets, mockAppConfig)

  "api definition" must {
    def checkDefinition(apiStatus: String, enabled: Boolean): Assertion = {
      MockAppConfig.apiStatus returns apiStatus
      MockAppConfig.apiEndpointsEnabled returns enabled
      MockAppConfig.apiGatewayContext returns "customs/imports/notifications"

      val result = documentationController.definition()(FakeRequest())

      status(result)      shouldBe OK
      contentType(result) shouldBe Some(MimeTypes.JSON)
      val definitionJson = contentAsJson(result)

      val versions = (definitionJson \ "api" \ "versions").as[Seq[JsValue]]
//      versions should have size 1
      val version = versions.head

      (version \ "status").as[String]            shouldBe apiStatus
      (version \ "endpointsEnabled").as[Boolean] shouldBe enabled
      (version \ "access").toOption              shouldBe None
    }

    "include the correct status and enabled flags disabled" in {
      checkDefinition("ALPHA", enabled = false)
    }

    "include the correct status and enabled flags enabled" in {
      checkDefinition("BETA", enabled = true)
    }
  }
}
