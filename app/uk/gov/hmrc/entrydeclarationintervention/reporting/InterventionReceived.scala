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

package uk.gov.hmrc.entrydeclarationintervention.reporting

import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.entrydeclarationintervention.models.received.MessageType
import uk.gov.hmrc.entrydeclarationintervention.reporting.audit.AuditEvent

case class InterventionReceived(
  eori: String,
  correlationId: String,
  submissionId: String,
  messageType: MessageType,
  body: JsValue
) extends Report

object InterventionReceived {
  implicit val eventSources: EventSources[InterventionReceived] = new EventSources[InterventionReceived] {
    override def auditEventFor(report: InterventionReceived): Option[AuditEvent] = {
      import report._
      val auditEvent = AuditEvent(
        auditType       = "InterventionReceived",
        transactionName = "ENS intervention received from EIS",
        JsObject(Seq("eori" -> JsString(eori), "correlationId" -> JsString(correlationId), "interventionBody" -> body))
      )

      Some(auditEvent)
    }
  }
}
