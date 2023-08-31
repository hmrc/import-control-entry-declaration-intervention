/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.entrydeclarationintervention.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationintervention.models.received.{InterventionResponse, MessageType}
import uk.gov.hmrc.entrydeclarationintervention.reporting.{InterventionReceived, MockReportSender}
import uk.gov.hmrc.entrydeclarationintervention.services.MockInterventionSubmissionService
import uk.gov.hmrc.entrydeclarationintervention.utils.{ResourceUtils, SaveError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InterventionSubmissionControllerSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MockInterventionSubmissionService
    with MockAppConfig
    with MockReportSender {

  val validIntervention: JsValue = ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse)
  val bearerToken: String        = "bearerToken"
  val request: FakeRequest[JsValue] =
    FakeRequest().withBody(validIntervention).withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $bearerToken")

  val interventionReceivedReport: InterventionReceived = InterventionReceived(
    "ABCDEFGHIJKLMN",
    "12345678901234",
    "c75f40a6-a3df-4429-a697-471eeec46435",
    MessageType.IE351,
    validIntervention) //Could parse the values from the validIntervention

  private val controller =
    new InterventionSubmissionController(
      mockAppConfig,
      Helpers.stubControllerComponents(),
      mockInterventionSubmissionService,
      mockReportSender)

  private val submissionId = "submissionId"

  "Post a valid intervention message" should {
    "return a 201 Created " in {
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(false)
      MockInterventionSubmissionService
        .processIntervention(validIntervention.as[InterventionResponse])
        .returns(Future.successful(None))
      MockReportSender.sendReport(interventionReceivedReport)

      val result = controller.postIntervention(request)

      status(result) shouldBe Status.CREATED
    }
  }

  "Post an intervention that cannot be processed" should {
    "convert error codes from the service to the appropriate responses" when {
      def run(saveError: SaveError, expectedStatus: Int): Unit =
        s"a $saveError error is returned from the service" in {
          MockAppConfig.eisInboundBearerToken.returns(bearerToken)
          MockAppConfig.validateIncomingJson.returns(false)
          MockInterventionSubmissionService
            .processIntervention(validIntervention.as[InterventionResponse])
            .returns(Future.successful(Some(saveError)))
          MockReportSender.sendReport(interventionReceivedReport)

          val result = controller.postIntervention(request)

          status(result) shouldBe expectedStatus
        }

      val input = Seq(
        (SaveError.ServerError, INTERNAL_SERVER_ERROR)
      )
      input.foreach(args => (run _).tupled(args))
    }
  }

  "Post a valid message when schema validation is enabled" must {
    "return a 201 Created" in {
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      MockAppConfig.validateIncomingJson.returns(true)
      MockInterventionSubmissionService
        .processIntervention(validIntervention.as[InterventionResponse])
        .returns(Future.successful(None))
      MockReportSender.sendReport(interventionReceivedReport)

      val result = controller.postIntervention(request)

      status(result) shouldBe Status.CREATED
    }
  }

  "Post an invalid message" should {
    "return a 400 Bad Request when schema validation is enabled" in {
      val intervention = Json.parse(s"""
                                       |{
                                       |  "submissionId": "$submissionId",
                                       |  "metadata": {
                                       |    "senderEORI": "ABCDEFGHIJKLMN",
                                       |    "senderBranch": "ABCDEFGHIJKLMNO",
                                       |    "preparationDateTime": "2020-12-23T14:46:01.000Z",
                                       |    "messageType": "IE351",
                                       |    "messageIdentification": "ABCDE",
                                       |    "receivedDateTime": "2020-12-23T14:46:01.000Z",
                                       |    "correlationId": "BAD_CORRELATION_ID"
                                       |  },
                                       |  "parties": {
                                       |    "declarant": {"eori": "GB00123456789"}
                                       |  },
                                       |  "goods": {
                                       |    "numberOfItems": 0
                                       |  },
                                       |  "declaration": {
                                       |    "localReferenceNumber": "ABCDEFGHIJKLMNOPQRST",
                                       |    "movementReferenceNumber": "ABCDEFGHIJKLMNOPQR",
                                       |    "registeredDateTime": "2020-12-23T14:46:01.000Z",
                                       |    "submittedDateTime": "2020-12-23T14:46:01.000Z"
                                       |  },
                                       |  "itinerary": {
                                       |    "officeOfFirstEntry": {
                                       |      "reference": "ABCDEFGH",
                                       |      "expectedDateTimeOfArrival": "2020-12-23T14:46:01.000Z"
                                       |    }
                                       |  },
                                       |  "customsIntervention": {
                                       |    "notificationDateTime": "2020-12-23T14:46:01.000Z",
                                       |    "interventions": [
                                       |      {
                                       |        "code": "A001",
                                       |        "itemNumber": "1",
                                       |        "text": "GOODS NOT TO BE LOADED",
                                       |        "language": "EN"
                                       |      }
                                       |    ]
                                       |  }
                                       |}
                                       |""".stripMargin)

      MockAppConfig.validateIncomingJson.returns(true)
      val request =
        FakeRequest().withBody(intervention).withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $bearerToken")
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      val result = controller.postIntervention(request)

      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 400 Bad Request for un-parsable json" in {
      val intervention = Json.parse(s"""
                                       |{
                                       |  "submissionId": "$submissionId",
                                       |  "metadata": {
                                       |    "senderEORI": "volup",
                                       |    "senderBranch": "pariat",
                                       |    "preparationDateTime": "2020-12-31T23:59:00.000Z",
                                       |    "messageType": "IE305",
                                       |    "messageIdentification": "msgId",
                                       |    "correlationId": "correlationId",
                                       |    "receivedDateTime": "2020-12-31T23:59:00.000Z"
                                       |  },
                                       |  "response": "beans"
                                       |}
                                       |""".stripMargin)

      val request =
        FakeRequest().withBody(intervention).withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $bearerToken")
      MockAppConfig.eisInboundBearerToken.returns(bearerToken)
      val result = controller.postIntervention(request)

      status(result) shouldBe Status.BAD_REQUEST
    }
    "return 403" when {
      "no authentication fails" in {
        MockAppConfig.eisInboundBearerToken returns "XXXX"

        val result = controller.postIntervention(request)

        status(result) shouldBe FORBIDDEN
      }
    }
  }
}
