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

import play.api.libs.json._
import uk.gov.hmrc.entrydeclarationintervention.models.InterventionModel
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.jatInstantFormat

import java.time.Instant

private[repositories] case class InterventionPersisted(
  eori: String,
  notificationId: String,
  correlationId: String,
  acknowledged: Boolean = false,
  receivedDateTime: Instant,
  housekeepingAt: Instant,
  submissionId: String,
  interventionXml: String) {
  def toIntervention: InterventionModel =
    InterventionModel(
      eori             = eori,
      notificationId   = notificationId,
      correlationId    = correlationId,
      acknowledged     = acknowledged,
      receivedDateTime = receivedDateTime,
      housekeepingAt   = housekeepingAt,
      submissionId     = submissionId,
      interventionXml  = interventionXml
    )
}

private[repositories] object InterventionPersisted {
  def from(intervention: InterventionModel): InterventionPersisted = {
    import intervention._

    InterventionPersisted(
      eori             = eori,
      notificationId   = notificationId,
      correlationId    = correlationId,
      acknowledged     = acknowledged,
      receivedDateTime = receivedDateTime,
      housekeepingAt   = housekeepingAt,
      submissionId     = submissionId,
      interventionXml  = interventionXml
    )
  }
  implicit val format: Format[InterventionPersisted] = Json.format[InterventionPersisted]
}
