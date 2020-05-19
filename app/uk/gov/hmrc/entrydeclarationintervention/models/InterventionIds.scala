/*
 * Copyright 2020 HM Revenue & Customs
 *
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
