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

package uk.gov.hmrc.entrydeclarationintervention.services.test

import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.entrydeclarationintervention.models.NotificationId
import uk.gov.hmrc.entrydeclarationintervention.repositories.MockInterventionRepo
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class NotificationIdRetrievalServiceSpec extends UnitSpec with MockInterventionRepo with ScalaFutures {
  val notificationIdRetrievalService = new NotificationIdRetrievalService(interventionRepo)

  val submissionId: String           = "submissionId"
  val notificationId: NotificationId = NotificationId("notificationId")

  "NotificationIdRetrievalService" when {
    "retrieving notificationId from submissionId" when {
      "successfully found" must {
        "return it" in {
          MockInterventionRepo.lookupNotificationId(submissionId).returns(Future.successful(Some(notificationId)))

          notificationIdRetrievalService.retrieveNotificationId(submissionId).futureValue shouldBe Some(notificationId)
        }
      }
      "not found" must {
        "return None" in {
          MockInterventionRepo.lookupNotificationId(submissionId).returns(Future.successful(None))

          notificationIdRetrievalService.retrieveNotificationId(submissionId).futureValue shouldBe None
        }
      }
    }
  }
}
