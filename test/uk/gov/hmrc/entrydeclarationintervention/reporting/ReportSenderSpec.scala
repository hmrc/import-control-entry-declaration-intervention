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

package uk.gov.hmrc.entrydeclarationintervention.reporting

import com.kenshoo.play.metrics.Metrics
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.JsObject
import uk.gov.hmrc.entrydeclarationintervention.reporting.audit.{AuditEvent, MockAuditHandler}
import uk.gov.hmrc.entrydeclarationintervention.utils.MockMetrics
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportSenderSpec extends UnitSpec with MockAuditHandler with ScalaFutures {

  val auditEvent: AuditEvent = AuditEvent("type", "trans", JsObject.empty)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockedMetrics: Metrics = new MockMetrics

  val reportSender = new ReportSender(mockAuditHandler, mockedMetrics)

  "ReportSender" must {
    object Report

    implicit val sources: EventSources[Report.type] = new EventSources[Report.type] {
      override def auditEventFor(report: Report.type): Option[AuditEvent] = Some(auditEvent)
    }

    "audit" in {
      MockAuditHandler.audit(auditEvent) returns Future.successful(())

      reportSender.sendReport(Report).futureValue shouldBe ((): Unit)
    }
    "return success even if the audit fails" in {
      MockAuditHandler.audit(auditEvent) returns Future.failed(new RuntimeException)

      reportSender.sendReport(Report).futureValue shouldBe ((): Unit)
    }
  }

}
