/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils

import scala.xml.XML

class SchemaValidatorSpec extends AnyWordSpecLike with Matchers with OptionValues {
  val schemaValidator = new SchemaValidator

  "SchemaValidator" when {
    "XML is valid" should {
      "validate a sample valid xml correctly" when {
        "optionalFieldFeature is false" in {
          val xml = ResourceUtils.withInputStreamFor("xmls/Intervention.xml")(XML.load)

          schemaValidator.validateSchema(xml).isValid shouldBe true
        }
        "optionalFieldFeature is true" in {
          val xml = ResourceUtils.withInputStreamFor("xmls/InterventionNew.xml")(XML.load)

          schemaValidator.validateSchema(xml, true).isValid shouldBe true
        }
      }

      "fail to validate a invalid xml" in {
        val xml = ResourceUtils.withInputStreamFor("xmls/BadExample.xml")(XML.load)

        schemaValidator.validateSchema(xml).isValid shouldBe false
      }
    }
  }
}
