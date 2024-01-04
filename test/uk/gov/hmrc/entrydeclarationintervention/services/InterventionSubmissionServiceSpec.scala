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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalamock.matchers.Matchers
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import uk.gov.hmrc.entrydeclarationintervention.config.MockAppConfig
import uk.gov.hmrc.entrydeclarationintervention.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationintervention.models.InterventionModel
import uk.gov.hmrc.entrydeclarationintervention.models.received.InterventionResponse
import uk.gov.hmrc.entrydeclarationintervention.repositories.MockInterventionRepo
import uk.gov.hmrc.entrydeclarationintervention.utils.{MockIdGenerator, ResourceUtils, SaveError}
import uk.gov.hmrc.entrydeclarationintervention.validators.{MockSchemaValidator, ValidationResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.xml.{Elem, SAXParseException}

class InterventionSubmissionServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
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

  val submissionId   = "c75f40a6-a3df-4429-a697-471eeec46435"
  val eori           = "ABCDEFGHIJKLMN"
  val correlationId  = "12345678901234"
  val notificationId = "notificationId"

  val rawXml: Elem     = <rawXml/>
  val wrappedXml: Elem = <wrapped/>

  val defaultTtl: FiniteDuration = 3.seconds
  val receivedDateTime: Instant  = Instant.parse("2020-12-23T14:46:01.000Z")
  val housekeepingAt: Instant    = receivedDateTime.plusSeconds(defaultTtl.toSeconds)

  val interventionModel: InterventionModel = InterventionModel(
    eori,
    notificationId,
    correlationId,
    acknowledged = false,
    receivedDateTime,
    housekeepingAt,
    submissionId,
    wrappedXml.toString
  )
  implicit val loggingContext: LoggingContext = LoggingContext(eori, correlationId, submissionId, notificationId, "mrn")

  val interventionReceived: InterventionResponse =
    ResourceUtils.withInputStreamFor("jsons/Intervention.json")(Json.parse).as[InterventionResponse]

  "InterventionSubmissionService processIntervention" must {
    "return None" when {
      "an intervention is supplied and successfully saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockXMLBuilder.buildXML(interventionReceived) returns rawXml
        MockIdGenerator.generateNotificationId returns notificationId
        MockXMLWrapper.wrapXml(notificationId, rawXml) returns wrappedXml
        MockAppConfig.defaultTtl returns defaultTtl
        MockInterventionRepo.saveIntervention(interventionModel) returns Future.successful(None)

        service.processIntervention(interventionReceived).futureValue shouldBe None
      }

      "an intervention successfully processed despite schema validation failing" in {
        MockAppConfig.validateJsonToXMLTransformation returns true
        MockXMLBuilder.buildXML(interventionReceived) returns rawXml
        MockSchemaValidator.validateSchema(rawXml, false) returns failedValidationResult
        MockIdGenerator.generateNotificationId returns notificationId
        MockXMLWrapper.wrapXml(notificationId, rawXml) returns wrappedXml
        MockAppConfig.defaultTtl returns defaultTtl
        MockInterventionRepo.saveIntervention(interventionModel) returns Future.successful(None)

        service.processIntervention(interventionReceived).futureValue shouldBe None
      }
    }

    "return SaveError" when {
      val someSaveError = SaveError.ServerError

      "the intervention cannot be saved" in {
        MockAppConfig.validateJsonToXMLTransformation returns false
        MockXMLBuilder.buildXML(interventionReceived) returns rawXml
        MockIdGenerator.generateNotificationId returns notificationId
        MockXMLWrapper.wrapXml(notificationId, rawXml) returns wrappedXml
        MockAppConfig.defaultTtl returns defaultTtl
        MockInterventionRepo.saveIntervention(interventionModel) returns Future.successful(Some(SaveError.ServerError))

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
