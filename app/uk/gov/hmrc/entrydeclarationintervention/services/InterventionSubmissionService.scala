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

import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationintervention.models.InterventionModel
import uk.gov.hmrc.entrydeclarationintervention.models.received.{InterventionResponse, InterventionResponseNew}
import uk.gov.hmrc.entrydeclarationintervention.repositories.InterventionRepo
import uk.gov.hmrc.entrydeclarationintervention.utils.{IdGenerator, SaveError, Timer}
import uk.gov.hmrc.entrydeclarationintervention.validators.SchemaValidator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, Utility}

@Singleton
class InterventionSubmissionService @Inject()(
  appConfig: AppConfig,
  xmlBuilder: XMLBuilder,
  xmlWrapper: XMLWrapper,
  interventionRepo: InterventionRepo,
  schemaValidator: SchemaValidator,
  idGenerator: IdGenerator,
  override val metrics: Metrics)(implicit ec: ExecutionContext)
    extends Timer
    with Logging {

  def processIntervention(intervention: InterventionResponse): Future[Option[SaveError]] =
    timeFuture("Service processIntervention", "processIntervention.total") {
      val rawXml = buildXML(intervention)
      validateSchema(rawXml)
      val notificationId = idGenerator.generateNotificationId

      implicit val loggingContext: LoggingContext = LoggingContext(intervention, notificationId)
      ContextLogger.info("notification received")

      val wrappedXml = xmlWrapper.wrapXml(notificationId, rawXml)
      saveIntervention(notificationId, intervention, wrappedXml)
    }

  def processInterventionNew(intervention: InterventionResponseNew): Future[Option[SaveError]] =
    timeFuture("Service processIntervention", "processIntervention.total") {
      val rawXml = buildXMLNew(intervention)
      validateSchema(rawXml, true)
      val notificationId = idGenerator.generateNotificationId

      implicit val loggingContext: LoggingContext = LoggingContext(intervention, notificationId)
      ContextLogger.info("notification received")

      val wrappedXml = xmlWrapper.wrapXml(notificationId, rawXml)
      saveInterventionNew(notificationId, intervention, wrappedXml)
    }

  private def saveIntervention(notificationId: String, intervention: InterventionResponse, interventionXml: Node)(
    implicit lc: LoggingContext): Future[Option[SaveError]] =
    timeFuture("Service saveIntervention", "saveIntervention.total") {
      import intervention._
      val xmlString = Utility.trim(interventionXml).toString

      interventionRepo
        .save(
          InterventionModel(
            eori             = metadata.senderEORI,
            correlationId    = metadata.correlationId,
            notificationId   = notificationId,
            receivedDateTime = metadata.receivedDateTime,
            housekeepingAt   = metadata.receivedDateTime.plusMillis(appConfig.defaultTtl.toMillis),
            submissionId     = submissionId,
            interventionXml  = xmlString
          ))
        .map { error =>
          if (error.isEmpty) ContextLogger.info("notification available")
          error
        }
    }

  private def saveInterventionNew(notificationId: String, intervention: InterventionResponseNew, interventionXml: Node)(
    implicit lc: LoggingContext): Future[Option[SaveError]] =
    timeFuture("Service saveIntervention", "saveIntervention.total") {
      import intervention._
      val xmlString = Utility.trim(interventionXml).toString

      interventionRepo
        .save(
          InterventionModel(
            eori             = metadata.senderEORI,
            correlationId    = metadata.correlationId,
            notificationId   = notificationId,
            receivedDateTime = metadata.receivedDateTime,
            housekeepingAt   = metadata.receivedDateTime.plusMillis(appConfig.defaultTtl.toMillis),
            submissionId     = submissionId,
            interventionXml  = xmlString
          ))
        .map { error =>
          if (error.isEmpty) ContextLogger.info("notification available")
          error
        }
    }

  private def buildXML(intervention: InterventionResponse) =
    time("Build XML", s"buildInterventionXML") {
      xmlBuilder.buildXML(intervention)
    }

  private def buildXMLNew(intervention: InterventionResponseNew) =
    time("Build XML", s"buildInterventionXML") {
      xmlBuilder.buildXMLNew(intervention)
    }

  private def validateSchema(xml: Node, useNew: Boolean = false): Unit =
    if (appConfig.validateJsonToXMLTransformation) {
      val result = schemaValidator.validateSchema(xml, useNew)
      if (!result.isValid) {
        logger.warn(
          s"\n$xml\n is not valid against CC351A schema:\n ${result.allErrors.map(_.getMessage).mkString("\n")}")
      }
    }
}
