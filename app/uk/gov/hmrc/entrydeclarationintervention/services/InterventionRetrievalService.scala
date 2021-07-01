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

import com.kenshoo.play.metrics.Metrics
import play.api.Logging

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, InterventionModel}
import uk.gov.hmrc.entrydeclarationintervention.repositories.InterventionRepo
import uk.gov.hmrc.entrydeclarationintervention.utils.Timer

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InterventionRetrievalService @Inject()(interventionRepo: InterventionRepo, override val metrics: Metrics)(
  implicit ec: ExecutionContext)
    extends Timer
    with Logging {
  def retrieveIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    timeFuture("Service retrieveIntervention", "retrieveIntervention.total") {
      interventionRepo.lookupIntervention(eori, notificationId)
    }

  def retrieveFullIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    timeFuture("Service retrieveIntervention", "retrieveFullIntervention.total") {
      interventionRepo.lookupFullIntervention(eori, notificationId)
    }

  def acknowledgeIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    timeFuture("Service acknowledgeIntervention", "acknowledgeIntervention.total") {
      interventionRepo.acknowledgeIntervention(eori, notificationId)
    }

  def listInterventions(eori: String): Future[List[InterventionIds]] =
    timeFuture("Service listInterventions", "listInterventions.total") {
      interventionRepo.listInterventions(eori)
    }
}
