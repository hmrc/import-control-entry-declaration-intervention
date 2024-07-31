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

package uk.gov.hmrc.entrydeclarationintervention.repositories

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.entrydeclarationintervention.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, InterventionModel}
import uk.gov.hmrc.entrydeclarationintervention.utils.SaveError

import scala.concurrent.Future

trait MockInterventionRepo extends TestSuite with MockFactory {
  val interventionRepo: InterventionRepo = mock[InterventionRepo]

  object MockInterventionRepo {
    def saveIntervention(intervention: InterventionModel): CallHandler[Future[Option[SaveError]]] =
      (interventionRepo.save(_:InterventionModel)(_: LoggingContext)).expects(intervention, *)

    def lookupNotificationIds(submissionId: String): CallHandler[Future[Seq[String]]] =
      interventionRepo.lookupNotificationIds _ expects submissionId

    def lookupIntervention(eori: String, notificationId: String): CallHandler[Future[Option[InterventionModel]]] =
      (interventionRepo.lookupIntervention(_: String, _: String)).expects(eori, notificationId)

    def lookupFullIntervention(eori: String, notificationId: String): CallHandler[Future[Option[InterventionModel]]] =
      (interventionRepo.lookupFullIntervention(_: String, _: String)).expects(eori, notificationId)

    def acknowledgeIntervention(eori: String, notificationId: String): CallHandler[Future[Option[InterventionModel]]] =
      (interventionRepo.acknowledgeIntervention(_: String, _: String)).expects(eori, notificationId)

    def listInterventions(eori: String): CallHandler[Future[List[InterventionIds]]] =
      interventionRepo.listInterventions _ expects eori
  }
}
