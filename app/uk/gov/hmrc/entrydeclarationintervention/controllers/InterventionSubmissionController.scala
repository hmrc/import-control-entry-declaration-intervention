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

package uk.gov.hmrc.entrydeclarationintervention.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationintervention.models.received.{InterventionResponse, InterventionResponseNew}
import uk.gov.hmrc.entrydeclarationintervention.reporting.{InterventionReceived, ReportSender}
import uk.gov.hmrc.entrydeclarationintervention.services.InterventionSubmissionService
import uk.gov.hmrc.entrydeclarationintervention.validators.JsonSchemaValidator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InterventionSubmissionController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  service: InterventionSubmissionService,
  reportSender: ReportSender)(using ec: ExecutionContext)
    extends EisInboundAuthorisedController(cc, appConfig) with Logging {

  val postIntervention: Action[JsValue] = authorisedAction.async(parse.json) { request =>
    given Request[JsValue] = request
    if(appConfig.optionalFieldsFeature) {
      request.body.validate[InterventionResponseNew] match {
        case JsSuccess(intervention, _) =>
          given loggingContext: LoggingContext = LoggingContext(intervention)
          getValidationErrors(request.body, true) match {
            case Some(errorMsg) =>
              Future.successful(BadRequest(errorMsg))
            case None =>
              service.processInterventionNew(intervention).map { response =>
                reportSender.sendReport(
                  InterventionReceived(
                    eori          = intervention.metadata.senderEORI,
                    correlationId = intervention.metadata.correlationId,
                    submissionId  = intervention.submissionId,
                    intervention.metadata.messageType,
                    request.body
                  ))

                response match {
                  case None                      => Created
                  case _                         => InternalServerError
                }
              }
          }
        case JsError(errs) =>
          logger.error(s"Unable to parse intervention payload: $errs")
          Future.successful(BadRequest)
      }
    } else {
      request.body.validate[InterventionResponse] match {
        case JsSuccess(intervention, _) =>
          given loggingContext: LoggingContext = LoggingContext(intervention)
          getValidationErrors(request.body) match {
            case Some(errorMsg) => Future.successful(BadRequest(errorMsg))
            case None =>
              service.processIntervention(intervention).map { response =>
                reportSender.sendReport(
                  InterventionReceived(
                    eori          = intervention.metadata.senderEORI,
                    correlationId = intervention.metadata.correlationId,
                    submissionId  = intervention.submissionId,
                    intervention.metadata.messageType,
                    request.body
                  ))

                response match {
                  case None                      => Created
                  case _                         => InternalServerError
                }
              }
          }
        case JsError(errs) =>
          logger.error(s"Unable to parse intervention payload: $errs")
          Future.successful(BadRequest)
      }
    }
  }

  private def getValidationErrors(json: JsValue, useNew: Boolean = false)
                                 (using lc: LoggingContext): Option[JsValue] = {
    if (!appConfig.validateIncomingJson) None
    else {
      val schemaDoc =
        if (useNew) "conf/jsonSchemas/AdvancedInterventionNew.json"
        else "conf/jsonSchemas/AdvancedIntervention.json"

      JsonSchemaValidator.validateJSONAgainstSchema(json, schemaDoc) match {
        case Right(_) => None
        case Left(errors) =>
          Some(
            Json.obj(
              "message" -> "Failed to validate JSON against schema",
              "errors"  -> errors
            ))
      }
    }
  }
}
