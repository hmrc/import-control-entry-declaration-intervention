/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.entrydeclarationintervention.services

import javax.inject.Singleton
import uk.gov.hmrc.entrydeclarationintervention.models.NotificationId

import scala.xml.Elem

@Singleton
class XMLWrapper {
  def wrapXml(notificationId: NotificationId, xml: Elem): Elem =
    //@formatter:off
    <notificationResponse xmlns:cc3="http://ics.dgtaxud.ec/CC351A">
      <response>
        {xml}
      </response>
      <acknowledgement method='DELETE' href={s"/customs/imports/notifications/${notificationId.value}"}/>
    </notificationResponse>
  //@formatter:on
}
