/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationintervention.models.received.{ArbitraryIntervention, InterventionResponse}
import uk.gov.hmrc.entrydeclarationintervention.services.XMLBuilder._
import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils
import uk.gov.hmrc.entrydeclarationintervention.validators.SchemaValidator

import java.time.Instant
import scala.xml.{Utility, XML}

class XMLBuilderSpec extends WordSpecLike with Matchers with OptionValues with ScalaCheckDrivenPropertyChecks with ArbitraryIntervention {
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
      "an intervention is supplied" in {
        val interventionJson = ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse)
        val intervention     = interventionJson.as[InterventionResponse]
        val expected         = ResourceUtils.withInputStreamFor("xmls/Intervention.xml")(XML.load)

        val xml = xmlBuilder.buildXML(intervention)
        Utility.trim(xml).text shouldBe Utility.trim(expected).text
        xml.namespace          shouldBe "http://ics.dgtaxud.ec/CC351A"
        xml.prefix             shouldBe "cc3"
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

      "generate schema valid XML for all inputs" in {
//        pending
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
    }
  }
}
