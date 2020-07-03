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

import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.entrydeclarationintervention.models.received.MessageType
import uk.gov.hmrc.play.test.UnitSpec

class InterventionReceivedSpec extends UnitSpec {
  val report: InterventionReceived = InterventionReceived(
    eori          = "eori",
    correlationId = "correlationId",
    submissionId  = "submissionId",
    messageType   = MessageType.IE351,
    body          = JsObject(Seq("body1" -> JsString("value")))
  )
  "InterventionReceived" must {
    "have the correct associated JSON audit event" in {
      val event = implicitly[EventSources[InterventionReceived]].auditEventFor(report).get

      event.auditType       shouldBe "interventionReceived"
      event.transactionName shouldBe "ENS intervention received from EIS"

      Json.toJson(event.detail) shouldBe
        Json.parse("""
                     |{
                     |  "eori": "eori",
                     |  "correlationId": "correlationId",
                     |  "interventionBody": {
                     |    "body1": "value"
                     |  }
                     |}
                     |""".stripMargin)

    }
  }
}
