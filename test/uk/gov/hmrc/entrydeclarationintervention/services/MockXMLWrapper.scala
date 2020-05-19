/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.entrydeclarationintervention.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.entrydeclarationintervention.models.NotificationId

import scala.xml.Elem

trait MockXMLWrapper extends MockFactory {
  val mockXMLWrapper: XMLWrapper = mock[XMLWrapper]

  object MockXMLWrapper {
    def wrapXml(notificationId: NotificationId, xml: Elem): CallHandler[Elem] =
      mockXMLWrapper.wrapXml _ expects (notificationId, xml)
  }

}
