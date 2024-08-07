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

package uk.gov.hmrc.entrydeclarationintervention.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.entrydeclarationintervention.models.received.{InterventionResponse, InterventionResponseNew}
import uk.gov.hmrc.entrydeclarationintervention.utils.SaveError

import scala.concurrent.Future

trait MockInterventionSubmissionService extends TestSuite with MockFactory {
  val mockInterventionSubmissionService: InterventionSubmissionService = mock[InterventionSubmissionService]

  object MockInterventionSubmissionService {
    def processIntervention(interventionReceived: InterventionResponse): CallHandler[Future[Option[SaveError]]] =
      mockInterventionSubmissionService.processIntervention _ expects interventionReceived

    def processInterventionNew(interventionReceived: InterventionResponseNew): CallHandler[Future[Option[SaveError]]] =
      mockInterventionSubmissionService.processInterventionNew _ expects interventionReceived
  }
}
