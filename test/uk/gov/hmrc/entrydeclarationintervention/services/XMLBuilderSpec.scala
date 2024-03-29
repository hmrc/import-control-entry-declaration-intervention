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

package uk.gov.hmrc.entrydeclarationintervention.services

import java.time.Instant
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationintervention.models.received.{ArbitraryIntervention, InterventionResponse, InterventionResponseNew}
import uk.gov.hmrc.entrydeclarationintervention.services.XMLBuilder._
import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils
import uk.gov.hmrc.entrydeclarationintervention.validators.SchemaValidator

import scala.xml.{Utility, XML}

class XMLBuilderSpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaCheckDrivenPropertyChecks with ArbitraryIntervention {
  "XMLBuilder" when {
    "formatting with getDateFromDateTime" must {
      "use the correct format" in {
        getDateFromDateTime(Instant.parse("2019-03-29T23:12:00.000Z")) shouldBe "190329"
      }
    }

    "formatting with getTimeFromDateTime" must {
      "use the correct format" in {
        getTimeFromDateTime(Instant.parse("2019-03-29T23:12:00.000Z")) shouldBe "2312"
      }
    }

    "formatting with getDateTimeInXSDFormat" must {
      "use the correct format" in {
        getDateTimeInXSDFormat(Instant.parse("2019-03-29T23:12:00.000Z")) shouldBe "201903292312"
      }
    }
  }

  val xmlBuilder = new XMLBuilder

  "XMLBuilder" should {
    "return XML formatted correctly" when {
      "an intervention is supplied" when {
        "optionalFieldFeature is false" in {
          val interventionJson = ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse)
          val intervention = interventionJson.as[InterventionResponse]
          val expected = ResourceUtils.withInputStreamFor("xmls/Intervention.xml")(XML.load)

          val xml = xmlBuilder.buildXML(intervention)
          Utility.trim(xml).text shouldBe Utility.trim(expected).text
          xml.namespace shouldBe "http://ics.dgtaxud.ec/CC351A"
          xml.prefix shouldBe "cc3"
        }
        "optionalFieldFeature is true" in {
          val interventionJson = ResourceUtils.withInputStreamFor("jsons/InterventionNew.json")(Json.parse)
          val intervention = interventionJson.as[InterventionResponseNew]
          val expected = ResourceUtils.withInputStreamFor("xmls/InterventionNew.xml")(XML.load)

          val xml = xmlBuilder.buildXMLNew(intervention)
          Utility.trim(xml).text shouldBe Utility.trim(expected).text
          xml.namespace shouldBe "http://ics.dgtaxud.ec/CC351A"
          xml.prefix shouldBe "cc3"
        }
      }

      "an intervention is supplied with all optional fields" in {
        val interventionJson = ResourceUtils.withInputStreamFor("jsons/InterventionAllOptional.json")(Json.parse)
        val intervention     = interventionJson.as[InterventionResponse]
        val expected         = ResourceUtils.withInputStreamFor("xmls/InterventionAllOptional.xml")(XML.load)

        val xml = xmlBuilder.buildXML(intervention)
        Utility.trim(xml).text shouldBe Utility.trim(expected).text
        xml.namespace          shouldBe "http://ics.dgtaxud.ec/CC351A"
        xml.prefix             shouldBe "cc3"
      }

      "generate schema valid XML for all inputs" when {
        "optionalFieldFeature is false" in {
          val schemaValidator = new SchemaValidator

          forAll { intervention: InterventionResponse =>
            val xml = xmlBuilder.buildXML(intervention)

            schemaValidator.validateSchema(xml).allErrors.filterNot { ex =>
              // Ignore type related errors
              val msg = ex.toString
              msg.contains("cvc-pattern-valid") || msg.contains("cvc-type.3.1.3") || msg.contains("cvc-enumeration-valid")
            } shouldBe Nil
          }
        }
        "optionalFieldFeature is true" in {
          val schemaValidator = new SchemaValidator

          forAll { intervention: InterventionResponseNew =>
            val xml = xmlBuilder.buildXMLNew(intervention)

            schemaValidator.validateSchema(xml, true).allErrors.filterNot { ex =>
              // Ignore type related errors
              val msg = ex.toString
              msg.contains("cvc-pattern-valid") || msg.contains("cvc-type.3.1.3") || msg.contains("cvc-enumeration-valid")
            } shouldBe Nil
          }
        }
      }
    }
  }
}
