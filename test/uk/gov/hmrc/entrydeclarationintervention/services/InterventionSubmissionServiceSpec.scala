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

import java.time.Instant

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationintervention.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionModel, NotificationId}
import uk.gov.hmrc.entrydeclarationintervention.models.received.InterventionReceived
import uk.gov.hmrc.entrydeclarationintervention.repositories.MockInterventionRepo
import uk.gov.hmrc.entrydeclarationintervention.utils.{MockIdGenerator, ResourceUtils, SaveError}
import uk.gov.hmrc.entrydeclarationintervention.validators.{MockSchemaValidator, ValidationResult}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, SAXParseException}

class InterventionSubmissionServiceSpec
    extends UnitSpec
    with MockAppConfig
    with MockXMLBuilder
    with MockXMLWrapper
    with MockInterventionRepo
    with MockSchemaValidator
    with MockIdGenerator
    with ScalaFutures {

  val mockedMetrics: Metrics = new MockMetrics

  private class MockMetrics extends Metrics {
    override val defaultRegistry: MetricRegistry = new MetricRegistry()

    override def toJson: String = throw new NotImplementedError
  }

  val service = new InterventionSubmissionService(
    mockAppConfig,
    mockXMLBuilder,
    mockXMLWrapper,
    interventionRepo,
    mockSchemaValidator,
    mockIdGenerator,
    mockedMetrics)

  val correlationId = "12345678901234"

  val notificationId = "notificationId"

  val rawXml: Elem     = <rawXml/>
  val wrappedXml: Elem = <wrapped/>

  val interventionModel: InterventionModel = InterventionModel(
    "ABCDEFGHIJKLMN",
    notificationId,
    correlationId,
    acknowledged = false,
    Instant.parse("2020-12-23T14:46:01.000Z"),
    "c75f40a6-a3df-4429-a697-471eeec46435",
    wrappedXml.toString
  )

  val interventionReceived: InterventionReceived =
    ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse).as[InterventionReceived]

  "InterventionSubmissionService processIntervention" should {
    "return None" when {
      "an intervention is supplied and successfully saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockXMLBuilder.buildXML(interventionReceived).returns(rawXml)
        MockIdGenerator.generateNotificationId.returns(notificationId)
        MockXMLWrapper.wrapXml(notificationId, rawXml) returns wrappedXml
        MockInterventionRepo.saveIntervention(interventionModel).returns(None)
        println(Json.toJson(interventionModel))

        service.processIntervention(interventionReceived).futureValue shouldBe None
      }

      "an intervention successfully processed despite schema validation failing" in {
        MockAppConfig.validateJsonToXMLTransformation returns true
        MockXMLBuilder.buildXML(interventionReceived).returns(rawXml)
        MockSchemaValidator.validateSchema(rawXml) returns failedValidationResult
        MockIdGenerator.generateNotificationId.returns(notificationId)
        MockXMLWrapper.wrapXml(notificationId, rawXml) returns wrappedXml
        MockInterventionRepo.saveIntervention(interventionModel).returns(None)

        service.processIntervention(interventionReceived).futureValue shouldBe None
      }
    }

    "return SaveError" when {
      val someSaveError = SaveError.Duplicate

      "the intervention cannot be saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockXMLBuilder.buildXML(interventionReceived).returns(rawXml)
        MockIdGenerator.generateNotificationId.returns(notificationId)
        MockXMLWrapper.wrapXml(notificationId, rawXml) returns wrappedXml
        MockInterventionRepo.saveIntervention(interventionModel).returns(Some(someSaveError))

        service.processIntervention(interventionReceived).futureValue shouldBe Some(someSaveError)
      }
    }
  }

  private def failedValidationResult =
    new ValidationResult {
      override def isValid                           = false
      override def allErrors: Seq[SAXParseException] = Seq(new SAXParseException("invalid!", null))
    }

}
