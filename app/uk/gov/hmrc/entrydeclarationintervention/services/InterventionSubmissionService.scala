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

import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionModel, NotificationId}
import uk.gov.hmrc.entrydeclarationintervention.models.received.InterventionReceived
import uk.gov.hmrc.entrydeclarationintervention.repositories.InterventionRepo
import uk.gov.hmrc.entrydeclarationintervention.utils.{EventLogger, SaveError, Timer}
import uk.gov.hmrc.entrydeclarationintervention.validators.SchemaValidator

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, Utility}

@Singleton
class InterventionSubmissionService @Inject()(
  appConfig: AppConfig,
  xmlBuilder: XMLBuilder,
  xmlWrapper: XMLWrapper,
  interventionRepo: InterventionRepo,
  schemaValidator: SchemaValidator,
  override val metrics: Metrics)(implicit ec: ExecutionContext)
    extends Timer
    with EventLogger {

  def processIntervention(intervention: InterventionReceived): Future[Option[SaveError]] =
    timeFuture("Service processIntervention", "processIntervention.total") {
      val rawXml = buildXML(intervention)
      validateSchema(rawXml)
      val wrappedXml = xmlWrapper.wrapXml(NotificationId(intervention.metadata.correlationId), rawXml)
      saveIntervention(intervention, wrappedXml)
    }

  private def saveIntervention(intervention: InterventionReceived, interventionXml: Node): Future[Option[SaveError]] =
    timeFuture("Service saveIntervention", "saveIntervention.total") {
      import intervention._
      val xmlString = Utility.trim(interventionXml).toString

      interventionRepo.save(
        InterventionModel(
          eori             = metadata.senderEORI,
          correlationId    = metadata.correlationId,
          receivedDateTime = metadata.receivedDateTime,
          submissionId     = submissionId,
          interventionXml  = xmlString
        ))
    }

  private def buildXML(intervention: InterventionReceived) =
    time("Build XML", s"buildInterventionXML") {
      xmlBuilder.buildXML(intervention)
    }

  private def validateSchema(xml: Node): Unit =
    if (appConfig.validateJsonToXMLTransformation) {
      val result = schemaValidator.validateSchema(xml)
      if (!result.isValid) {
        logger.warn(
          s"\n$xml\n is not valid against CC351A schema:\n ${result.allErrors.map(_.getMessage).mkString("\n")}")
      }
    }

}