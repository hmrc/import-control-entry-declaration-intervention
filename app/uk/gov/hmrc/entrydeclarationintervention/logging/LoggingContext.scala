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

package uk.gov.hmrc.entrydeclarationintervention.logging

import uk.gov.hmrc.entrydeclarationintervention.models.InterventionModel

case class LoggingContext(
  eori: Option[String]           = None,
  correlationId: Option[String]  = None,
  submissionId: Option[String]   = None,
  notificationId: Option[String] = None) {
  private[logging] lazy val context: String = {
    Seq(
      eori.map(v => s"eori=$v"),
      correlationId.map(v => s"correlationId=$v"),
      submissionId.map(v => s"submissionId=$v"),
      notificationId.map(v => s"notificationId=$v")).flatten.mkString(" ")
  }
}
object LoggingContext {
  def apply(eori: String, correlationId: String, submissionId: String, notificationId: String): LoggingContext =
    LoggingContext(Some(eori), Some(correlationId), Some(submissionId), Some(notificationId))

  def apply(model: InterventionModel): LoggingContext =
    LoggingContext(model.eori, model.correlationId, model.submissionId, model.notificationId)
}