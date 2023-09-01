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

package uk.gov.hmrc.entrydeclarationintervention.logging

import uk.gov.hmrc.entrydeclarationintervention.models.InterventionModel
import uk.gov.hmrc.entrydeclarationintervention.models.received.InterventionResponse

case class LoggingContext(
  eori: Option[String]                    = None,
  correlationId: Option[String]           = None,
  submissionId: Option[String]            = None,
  notificationId: Option[String]          = None,
  movementReferenceNumber: Option[String] = None) {
  private[logging] lazy val context: String = {
    Seq(
      eori.map(v => s"eori=$v"),
      correlationId.map(v => s"correlationId=$v"),
      submissionId.map(v => s"submissionId=$v"),
      notificationId.map(v => s"notificationId=$v"),
      movementReferenceNumber.map(v => s"movementReferenceNumber=$v")
    ).flatten.mkString(" ")
  }
}
object LoggingContext {
  def apply(
    eori: String,
    correlationId: String,
    submissionId: String,
    notificationId: String,
    movementReferenceNumber: String): LoggingContext =
    LoggingContext(
      eori                    = Some(eori),
      correlationId           = Some(correlationId),
      submissionId            = Some(submissionId),
      notificationId          = Some(notificationId),
      movementReferenceNumber = Some(movementReferenceNumber)
    )

  def apply(model: InterventionModel): LoggingContext =
    LoggingContext(
      eori           = Some(model.eori),
      correlationId  = Some(model.correlationId),
      submissionId   = Some(model.submissionId),
      notificationId = Some(model.notificationId)
    )

  def apply(model: InterventionResponse): LoggingContext =
    LoggingContext(
      eori                    = Some(model.metadata.senderEORI),
      correlationId           = Some(model.metadata.correlationId),
      submissionId            = Some(model.submissionId),
      movementReferenceNumber = Some(model.declaration.movementReferenceNumber)
    )

  def apply(model: InterventionResponse, notificationId: String): LoggingContext =
    LoggingContext(
      eori                    = model.metadata.senderEORI,
      correlationId           = model.metadata.correlationId,
      submissionId            = model.submissionId,
      notificationId          = notificationId,
      movementReferenceNumber = model.declaration.movementReferenceNumber
    )
}
