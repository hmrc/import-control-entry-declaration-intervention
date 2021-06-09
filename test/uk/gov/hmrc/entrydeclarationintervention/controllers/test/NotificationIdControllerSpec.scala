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

package uk.gov.hmrc.entrydeclarationintervention.controllers.test

import play.api.http.MimeTypes
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.entrydeclarationintervention.services.test.MockNotificationIdRetrievalService
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationIdControllerSpec
  extends WordSpecLike
    with Matchers
    with OptionValues
    with MockNotificationIdRetrievalService {

  val controller = new NotificationIdController(Helpers.stubControllerComponents(), mockNotificationIdRetrievalService)

  val submissionId: String = "submissionId"
  val notificationId1      = "notificationId1"
  val notificationId2      = "notificationId2"

  "EntryDeclarationRetrievalController" when {
    "getting notificationIds from submissionId" when {
      "interventions exist" must {
        "return OK with a JSON array of notificationIds" in {
          MockNotificationIdRetrievalService
            .retrieveNotificationIds(submissionId) returns Future.successful(Seq(notificationId1, notificationId2))

          val result: Future[Result] = controller.getNotificationIds(submissionId)(FakeRequest())

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.arr(notificationId1, notificationId2)
          contentType(result)   shouldBe Some(MimeTypes.JSON)
        }
      }

      "no interventions exist" must {
        "return OK with an empty JSON array off notificationIds" in {
          MockNotificationIdRetrievalService
            .retrieveNotificationIds(submissionId) returns Future.successful(Nil)

          val result: Future[Result] = controller.getNotificationIds(submissionId)(FakeRequest())

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe JsArray.empty
          contentType(result)   shouldBe Some(MimeTypes.JSON)
        }
      }
    }
  }

}
