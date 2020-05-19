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

import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class SchemaValidatorSpec extends UnitSpec {
  val schemaValidator = new SchemaValidator

  "SchemaValidator" when {
    "XML is valid" should {
      "validate a sample valid xml correctly" in {
        val xml = ResourceUtils.withInputStreamFor("xmls/Intervention.xml")(XML.load)

        schemaValidator.validateSchema(xml).isValid shouldBe true
      }

      "fail to validate a invalid xml" in {
        val xml = ResourceUtils.withInputStreamFor("xmls/BadExample.xml")(XML.load)

        schemaValidator.validateSchema(xml).isValid shouldBe false
      }
    }
  }
}
