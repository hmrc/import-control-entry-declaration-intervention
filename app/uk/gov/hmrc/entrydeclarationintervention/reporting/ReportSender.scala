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

import com.google.inject.Inject
import com.codahale.metrics.MetricRegistry
import play.api.Logging
import uk.gov.hmrc.entrydeclarationintervention.reporting.audit.{AuditEvent, AuditHandler}
import uk.gov.hmrc.entrydeclarationintervention.utils.Timer
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReportSender @Inject()(auditHandler: AuditHandler, override val metrics: MetricRegistry)(implicit ec: ExecutionContext)
    extends Timer
    with Logging {
  def sendReport[R: EventSources](report: R)(implicit hc: HeaderCarrier): Future[Unit] = {

    val eventSources: EventSources[R] = implicitly

    eventSources.auditEventFor(report).foreach(event => audit(event))

    Future.successful(())
  }

  private def audit[R: EventSources](event: AuditEvent)(implicit hc: HeaderCarrier) =
    timeFuture("ReportSender audit", "reporting.audit") {
      auditHandler.audit(event)
    }
}
