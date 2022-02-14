/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.entrydeclarationintervention.controllers.test

import java.time.Instant

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.entrydeclarationintervention.models.InterventionModel
import uk.gov.hmrc.entrydeclarationintervention.services.MockInterventionRetrievalService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestInterventionRetrievalControllerSpec extends AnyWordSpecLike with Matchers with OptionValues with MockInterventionRetrievalService {

  val controller =
    new TestInterventionRetrievalController(Helpers.stubControllerComponents(), mockInterventionRetrievalService)

  val eori            = "eori"
  val notificationId  = "notificationId"
  val submissionId    = "submissionId"
  val correlationId   = "correlationId"
  val interventionXml = "<someXml/>"

  val intervention: InterventionModel = InterventionModel(
    eori,
    notificationId,
    correlationId,
    acknowledged = true,
    Instant.now,
    Instant.now.plusSeconds(1),
    submissionId,
    interventionXml
  )
  "TestInterventionRetrievalController" when {
    "getting full intervention by eori and notificationId" should {
      "return 200 with the JSON" when {
        "the intervention could be found" in {
          MockInterventionRetrievalService.retrieveFullIntervention(eori, notificationId) returns Future.successful(
            Some(intervention))

          val result = controller.getFullIntervention(eori, notificationId)(FakeRequest())

          status(result)        shouldBe 200
          contentAsJson(result) shouldBe Json.toJson(intervention)
          contentType(result)   shouldBe Some(MimeTypes.JSON)
        }
      }
      "return 404" when {
        "the intervention could not be found" in {
          MockInterventionRetrievalService.retrieveFullIntervention(eori, notificationId) returns Future.successful(None)

          val result = controller.getFullIntervention(eori, notificationId)(FakeRequest())

          status(result) shouldBe 404
        }
      }
    }
  }
}
