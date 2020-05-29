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

package uk.gov.hmrc.entrydeclarationintervention.controllers.test

import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.entrydeclarationintervention.models.NotificationId
import uk.gov.hmrc.entrydeclarationintervention.services.test.MockNotificationIdRetrievalService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationIdControllerSpec extends UnitSpec with MockNotificationIdRetrievalService {

  val controller = new NotificationIdController(Helpers.stubControllerComponents(), mockNotificationIdRetrievalService)

  val submissionId: String           = "submissionId"
  val notificationId: NotificationId = NotificationId("notificationId")

  "EntryDeclarationRetrievalController" when {
    "getting submissionId from eori and correlationId" when {
      "id exists" must {
        "return OK with the submissionId in a JSON object" in {
          MockNotificationIdRetrievalService
            .retrieveNotificationId(submissionId)
            .returns(Future.successful(Some(notificationId)))

          val result: Future[Result] = controller.getNotificationId(submissionId)(FakeRequest())

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(notificationId)
          contentType(result)   shouldBe Some(MimeTypes.JSON)
        }
      }

      "id does not exist" must {
        "return NOT_FOUND" in {
          MockNotificationIdRetrievalService
            .retrieveNotificationId(submissionId)
            .returns(Future.successful(None))

          val result: Future[Result] = controller.getNotificationId(submissionId)(FakeRequest())

          status(result) shouldBe NOT_FOUND
        }
      }
    }
  }

}
