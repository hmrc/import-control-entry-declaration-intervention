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
