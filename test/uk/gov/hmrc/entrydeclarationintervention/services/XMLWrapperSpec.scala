/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.entrydeclarationintervention.services

import org.apache.commons.lang3.StringUtils
import uk.gov.hmrc.entrydeclarationintervention.models.NotificationId
import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.{Utility, XML}

class XMLWrapperSpec extends UnitSpec {

  val xmlWrapper = new XMLWrapper

  "XMLWrapper" must {
    "wrap xml and include a link with the notification id" in {
      val namespace = "http://ics.dgtaxud.ec/CC351A"
      val wrapped = Utility.trim(
        xmlWrapper.wrapXml(
          NotificationId("someNotificationId"),
          <cc3:parent>
            <a>
              <b/>
            </a>
          </cc3:parent>))

      wrapped shouldBe
        Utility.trim(
          //@formatter:off
          <notificationResponse xmlns:cc3={namespace}>
          <response>
              <cc3:parent>
                <a>
                  <b/>
                </a>
              </cc3:parent>
          </response>
          <acknowledgement method='DELETE' href="/customs/imports/notifications/someNotificationId"/>
        </notificationResponse>
          //@formatter:on
        )

      wrapped.getNamespace("cc3") shouldBe namespace
    }

    "only include the namespace once in the string representation" in {
      val xml     = ResourceUtils.withInputStreamFor("xmls/Intervention.xml")(XML.load)
      val wrapped = xmlWrapper.wrapXml(NotificationId("someNotificationId"), xml)

      StringUtils.countMatches(wrapped.toString(), xml.namespace) shouldBe 1
    }
  }
}
