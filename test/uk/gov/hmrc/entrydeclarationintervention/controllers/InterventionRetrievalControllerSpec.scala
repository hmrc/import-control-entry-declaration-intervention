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

import java.time.Instant

import play.api.test.Helpers.{contentAsString, contentType, _}
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, InterventionModel, NotificationId}
import uk.gov.hmrc.entrydeclarationintervention.services.{MockAuthService, MockInterventionRetrievalService}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Node

class InterventionRetrievalControllerSpec extends UnitSpec with MockInterventionRetrievalService with MockAuthService {

  val controller =
    new InterventionRetrievalController(
      mockAuthService,
      Helpers.stubControllerComponents(),
      mockInterventionRetrievalService)

  val payloadXml     = "payloadXml"
  val submissionId   = "someSubmissionId"
  val eori           = "someEori"
  val correlationId  = "someCorrelationId"
  val notificationId = "someNotificationId"

  val intervention: InterventionModel = InterventionModel(
    eori,
    notificationId,
    correlationId,
    acknowledged = false,
    Instant.parse("2020-12-31T23:59:00Z"),
    submissionId,
    payloadXml
  )

  val notFoundXml: Node =
    <error>
      <code>NOTIFICATION_NOT_FOUND</code>
      <message>No unacknowledged notification found</message>
    </error>

  "InterventionRetrievalController getIntervention" should {
    "return 200 OK with the XML" when {
      "the user is authenticated and the intervention XML could be found" in {
        MockAuthService.authenticate() returns Future.successful(Some(eori))
        MockInterventionRetrievalService.retrieveIntervention(eori, notificationId) returns Future.successful(
          Some(intervention))

        val result = controller.getIntervention(notificationId)(FakeRequest())

        status(result)          shouldBe OK
        contentAsString(result) shouldBe payloadXml
        contentType(result)     shouldBe Some(MimeTypes.XML)
      }
    }
    "return 404 NOT_FOUND" when {
      "the user is authenticated and the intervention XML could not be found" in {
        MockAuthService.authenticate() returns Future.successful(Some(eori))
        MockInterventionRetrievalService.retrieveIntervention(eori, notificationId) returns Future.successful(None)

        val result = controller.getIntervention(notificationId)(FakeRequest())

        status(result)                              shouldBe NOT_FOUND
        xml.XML.loadString(contentAsString(result)) shouldBe notFoundXml
        contentType(result)                         shouldBe Some(MimeTypes.XML)
      }
    }
    "return 401 UNAUTHORIZED" when {
      "the user is not-authenticated" in {
        MockAuthService.authenticate() returns Future.successful(None)

        val result = controller.getIntervention(notificationId)(FakeRequest())

        status(result) shouldBe UNAUTHORIZED
      }
    }
  }

  "InterventionRetrievalController acknowledgeIntervention" should {
    "return 200 OK" when {
      "the user is authenticated and the intervention XML could be found" in {
        MockAuthService.authenticate() returns Future.successful(Some(eori))
        MockInterventionRetrievalService.acknowledgeIntervention(eori, notificationId) returns Future.successful(
          Some(intervention))

        val result = controller.acknowledgeIntervention(notificationId)(FakeRequest())

        status(result) shouldBe OK
      }
    }
    "return 404 NOT_FOUND" when {
      "the user is authenticated and the intervention XML could not be found" in {
        MockAuthService.authenticate() returns Future.successful(Some(eori))
        MockInterventionRetrievalService.acknowledgeIntervention(eori, notificationId) returns Future.successful(None)

        val result = controller.acknowledgeIntervention(notificationId)(FakeRequest())

        status(result)                              shouldBe NOT_FOUND
        xml.XML.loadString(contentAsString(result)) shouldBe notFoundXml
        contentType(result)                         shouldBe Some(MimeTypes.XML)
      }
    }
    "return 401 UNAUTHORIZED" when {
      "the user is not-authenticated" in {
        MockAuthService.authenticate() returns Future.successful(None)

        val result = controller.acknowledgeIntervention(notificationId)(FakeRequest())

        status(result) shouldBe UNAUTHORIZED
      }
    }
  }

  "InterventionRetrievalController listInterventions" should {
    "return 200 OK" when {
      "the user is authenticated and an unacknowledged intervention could be found" in {
        val corId1 = "corId1"
        val corId2 = "corId2"
        val notId1 = "notId1"
        val notId2 = "notId2"
        MockAuthService.authenticate() returns Future.successful(Some(eori))
        MockInterventionRetrievalService.listInterventions(eori) returns Future.successful(
          List(InterventionIds(corId1, notId1), InterventionIds(corId2, notId2)))

        val listXml =
          <advancedNotifications>
           <response>
            <correlationId>{corId1}</correlationId>
             <notificationId>{notId1}</notificationId>
            <link>/customs/imports/notifications/{notId1}</link>
           </response>
           <response>
            <correlationId>{corId2}</correlationId>
             <notificationId>{notId2}</notificationId>
            <link>/customs/imports/notifications/{notId2}</link>
           </response>
          </advancedNotifications>

        val result        = controller.listInterventions()(FakeRequest())
        val prettyPrinter = new scala.xml.PrettyPrinter(80, 4)

        status(result)                                                    shouldBe OK
        prettyPrinter.format(xml.XML.loadString(contentAsString(result))) shouldBe prettyPrinter.format(listXml)
        contentType(result)                                               shouldBe Some(MimeTypes.XML)
      }
    }
    "return 204 NO_CONTENT" when {
      "the user is authenticated and no unacknowledged intervention XML could be found" in {
        MockAuthService.authenticate() returns Future.successful(Some(eori))
        MockInterventionRetrievalService.listInterventions(eori) returns
          Future.successful(List.empty[InterventionIds])

        val result = controller.listInterventions()(FakeRequest())

        status(result) shouldBe NO_CONTENT
      }
    }
    "return 401 UNAUTHORIZED" when {
      "the user is not-authenticated" in {
        MockAuthService.authenticate() returns Future.successful(None)

        val result = controller.listInterventions()(FakeRequest())

        status(result) shouldBe UNAUTHORIZED
      }
    }
  }
}
