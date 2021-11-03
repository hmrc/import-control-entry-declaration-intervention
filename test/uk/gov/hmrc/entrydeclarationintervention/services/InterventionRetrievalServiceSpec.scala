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

import java.time.Instant

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalamock.matchers.Matchers
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.entrydeclarationintervention.models._
import uk.gov.hmrc.entrydeclarationintervention.repositories.MockInterventionRepo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InterventionRetrievalServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with MockInterventionRepo with ScalaFutures {

  val mockedMetrics: Metrics = new MockMetrics

  private class MockMetrics extends Metrics {
    override val defaultRegistry: MetricRegistry = new MetricRegistry()

    override def toJson: String = throw new NotImplementedError
  }

  val service = new InterventionRetrievalService(interventionRepo, mockedMetrics)

  val submissionId   = "submissionId"
  val eori           = "eori"
  val correlationId  = "correlationId"
  val notificationId = "notificationId"
  val xml            = "somexml"

  val intervention: InterventionModel = InterventionModel(
    eori,
    notificationId,
    correlationId,
    acknowledged = false,
    Instant.parse("2020-12-31T23:59:00Z"),
    Instant.parse("2020-12-31T23:59:00Z"),
    submissionId,
    xml
  )

  "InterventionRetrievalService" when {
    "retrieving intervention by eori and notificationId" must {
      "return the intervention if an intervention exists in the database" in {
        MockInterventionRepo.lookupIntervention(eori, notificationId) returns Future.successful(Some(intervention))

        service.retrieveIntervention(eori, notificationId).futureValue shouldBe Some(intervention)
      }

      "return None if no intervention exists in the database" in {
        MockInterventionRepo.lookupIntervention(eori, notificationId) returns Future.successful(None)

        service.retrieveIntervention(eori, notificationId).futureValue shouldBe None
      }
    }

    "retrieving full intervention by eori and notificationId" must {
      "return the intervention if an intervention exists in the database" in {
        MockInterventionRepo.lookupFullIntervention(eori, notificationId) returns Future.successful(Some(intervention))

        service.retrieveFullIntervention(eori, notificationId).futureValue shouldBe Some(intervention)
      }

      "return None if no intervention exists in the database" in {
        MockInterventionRepo.lookupFullIntervention(eori, notificationId) returns Future.successful(None)

        service.retrieveFullIntervention(eori, notificationId).futureValue shouldBe None
      }
    }

    "acknowledging intervention xml" must {
      "return true if an intervention exists in the database" in {
        MockInterventionRepo.acknowledgeIntervention(eori, notificationId) returns Future.successful(Some(intervention))

        service.acknowledgeIntervention(eori, notificationId).futureValue shouldBe Some(intervention)
      }

      "return false if no intervention exists in the database" in {
        MockInterventionRepo.acknowledgeIntervention(eori, notificationId) returns Future.successful(None)

        service.acknowledgeIntervention(eori, notificationId).futureValue shouldBe None
      }
    }

    "listing interventions xml" must {
      "return List(Interventions) if an intervention exists in the database" in {
        val interventionIds = List(InterventionIds("corId", "notId"))
        MockInterventionRepo.listInterventions(eori) returns Future.successful(interventionIds)

        service.listInterventions(eori).futureValue shouldBe interventionIds
      }

      "return Empty list if no intervention exists in the database" in {
        MockInterventionRepo.listInterventions(eori) returns Future.successful(List.empty[InterventionIds])

        service.listInterventions(eori).futureValue shouldBe List.empty[InterventionIds]
      }
    }
  }
}
