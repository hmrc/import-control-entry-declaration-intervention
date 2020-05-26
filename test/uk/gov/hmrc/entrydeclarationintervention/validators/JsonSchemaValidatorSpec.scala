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

package uk.gov.hmrc.entrydeclarationintervention.validators

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils
import uk.gov.hmrc.play.test.UnitSpec

class JsonSchemaValidatorSpec extends UnitSpec {

  "JsonSchemaValidator" should {
    "return true " when {
      "a valid message is supplied" in {
        val intervention: JsValue = ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse)

        JsonSchemaValidator.validateJSONAgainstSchema(intervention) shouldBe true
      }
    }

    "return false" when {
      "an invalid message is supplied" in {
        val invalidIntervention: JsValue =
          Json.parse("""|{
                        |"submissionId": "c75f40a6-a3df-4429-a697-471eeec46435",
                        |  "metadata": {
                        |    "senderEORI": "ABCDEFGHIJKLMN",
                        |    "senderBranch": "ABCDEFGHIJKLMNO",
                        |    "preparationDateTime": "2020-12-23T14:46:01.000Z",
                        |    "messageType": "IE351",
                        |    "messageIdentification": "ABCDE",
                        |    "receivedDateTime": "2020-12-23T14:46:01.000Z",
                        |    "correlationId": "12345678901234"
                        |    }
                        |  }""".stripMargin)

        JsonSchemaValidator.validateJSONAgainstSchema(invalidIntervention) shouldBe false
      }
    }
  }
}