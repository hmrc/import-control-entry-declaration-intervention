/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.entrydeclarationintervention.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationintervention.models.received.InterventionReceived
import uk.gov.hmrc.entrydeclarationintervention.services.MockInterventionSubmissionService
import uk.gov.hmrc.entrydeclarationintervention.utils.{ResourceUtils, SaveError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InterventionSubmissionControllerSpec
    extends WordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MockInterventionSubmissionService
    with MockAppConfig {

  val validIntervention: JsValue = ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse)
  val accessToken: String        = "accessToken"
  val bearerToken: String        = s"Bearer $accessToken"
  val request: FakeRequest[JsValue] =
    FakeRequest().withBody(validIntervention).withHeaders(HeaderNames.AUTHORIZATION -> bearerToken)

  private val controller =
    new InterventionSubmissionController(
      mockAppConfig,
      Helpers.stubControllerComponents(),
      mockInterventionSubmissionService)

  private val submissionId = "submissionId"

  "Post a valid intervention message" should {
    "return a 201 Created " in {
      MockAppConfig.eisInboundBearerToken.returns(accessToken)
      MockAppConfig.validateIncomingJson.returns(false)
      MockInterventionSubmissionService
        .processIntervention(validIntervention.as[InterventionReceived])
        .returns(Future.successful(None))

      val result = controller.postIntervention(request)

      status(result) shouldBe Status.CREATED
    }
  }

  "Post an intervention that cannot be processed" should {
    "convert error codes from the service to the appropriate responses" when {
      def run(saveError: SaveError, expectedStatus: Int): Unit =
        s"a $saveError error is returned from the service" in {
          MockAppConfig.eisInboundBearerToken.returns(accessToken)
          MockAppConfig.validateIncomingJson.returns(false)
          MockInterventionSubmissionService
            .processIntervention(validIntervention.as[InterventionReceived])
            .returns(Future.successful(Some(saveError)))

          val result = controller.postIntervention(request)

          status(result) shouldBe expectedStatus
        }

      val input = Seq(
        (SaveError.Duplicate, CONFLICT),
        (SaveError.ServerError, INTERNAL_SERVER_ERROR)
      )
      input.foreach(args => (run _).tupled(args))
    }
  }

  "Post a valid message when schema validation is enabled" must {
    "return a 201 Created" in {
      MockAppConfig.eisInboundBearerToken.returns(accessToken)
      MockAppConfig.validateIncomingJson.returns(true)
      MockInterventionSubmissionService
        .processIntervention(validIntervention.as[InterventionReceived])
        .returns(Future.successful(None))

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
      val request = FakeRequest().withBody(intervention).withHeaders(HeaderNames.AUTHORIZATION -> bearerToken)
      MockAppConfig.eisInboundBearerToken.returns(accessToken)
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

      val request = FakeRequest().withBody(intervention).withHeaders(HeaderNames.AUTHORIZATION -> bearerToken)
      MockAppConfig.eisInboundBearerToken.returns(accessToken)
      val result = controller.postIntervention(request)

      status(result) shouldBe Status.BAD_REQUEST
    }
    "return 401" when {
      "no authentication fails" in {
        MockAppConfig.eisInboundBearerToken returns "XXXX"

        val result = controller.postIntervention(request)

        status(result) shouldBe UNAUTHORIZED
      }
    }
  }
}
