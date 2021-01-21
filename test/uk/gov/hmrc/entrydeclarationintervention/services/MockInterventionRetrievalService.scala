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

package uk.gov.hmrc.entrydeclarationintervention.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.entrydeclarationintervention.models._

import scala.concurrent.Future

trait MockInterventionRetrievalService extends MockFactory {
  val mockInterventionRetrievalService: InterventionRetrievalService = mock[InterventionRetrievalService]

  object MockInterventionRetrievalService {
    def retrieveIntervention(eori: String, notificationId: String): CallHandler[Future[Option[InterventionModel]]] =
      mockInterventionRetrievalService.retrieveIntervention _ expects (eori, notificationId)

    def retrieveFullIntervention(eori: String, notificationId: String): CallHandler[Future[Option[InterventionModel]]] =
      mockInterventionRetrievalService.retrieveFullIntervention _ expects (eori, notificationId)

    def acknowledgeIntervention(eori: String, notificationId: String): CallHandler[Future[Option[InterventionModel]]] =
      mockInterventionRetrievalService.acknowledgeIntervention _ expects (eori, notificationId)

    def listInterventions(eori: String): CallHandler[Future[List[InterventionIds]]] =
      mockInterventionRetrievalService.listInterventions _ expects eori
  }

}
