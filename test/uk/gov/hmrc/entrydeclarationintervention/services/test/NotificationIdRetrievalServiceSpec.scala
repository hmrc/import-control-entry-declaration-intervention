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

package uk.gov.hmrc.entrydeclarationintervention.services.test

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.entrydeclarationintervention.repositories.MockInterventionRepo

import scala.concurrent.Future

class NotificationIdRetrievalServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with MockInterventionRepo with ScalaFutures {
  val notificationIdRetrievalService = new NotificationIdRetrievalService(interventionRepo)

  val submissionId: String = "submissionId"
  val notificationId       = "notificationId"

  "NotificationIdRetrievalService" when {
    "retrieving notificationIds from submissionId" must {
      "return it what is found" in {
        MockInterventionRepo.lookupNotificationIds(submissionId) returns Future.successful(Seq(notificationId))

        notificationIdRetrievalService.retrieveNotificationIds(submissionId).futureValue shouldBe Seq(notificationId)
      }
    }
  }
}
