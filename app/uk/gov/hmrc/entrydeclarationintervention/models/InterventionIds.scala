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

package uk.gov.hmrc.entrydeclarationintervention.models

import play.api.libs.json.{Reads, __}

case class InterventionIds(correlationId: String, notificationId: String)

object InterventionIds {
  implicit val reads: Reads[InterventionIds] = for {
    correlationId <- (__ \ "correlationId").read[String]
  } yield InterventionIds(correlationId, toNotificationId(correlationId).value)

  // TODO eventually have notificationId as a separately persisted field and remove this
  def toNotificationId(correlationId: String): NotificationId = NotificationId(correlationId)

  // TODO eventually have notificationId as a separately persisted field and remove this
  def toCorrelationId(notificationId: NotificationId): String = notificationId.value
}
