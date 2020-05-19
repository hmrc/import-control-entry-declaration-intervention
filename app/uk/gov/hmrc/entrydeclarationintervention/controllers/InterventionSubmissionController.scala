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

package uk.gov.hmrc.entrydeclarationintervention.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsResult, JsString, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.models.received.InterventionReceived
import uk.gov.hmrc.entrydeclarationintervention.services.InterventionSubmissionService
import uk.gov.hmrc.entrydeclarationintervention.utils.{EventLogger, SaveError}
import uk.gov.hmrc.entrydeclarationintervention.validators.JsonSchemaValidator
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InterventionSubmissionController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  service: InterventionSubmissionService)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with EventLogger {

  val postIntervention: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val model: JsResult[InterventionReceived] = request.body.validate[InterventionReceived]

    if (model.isSuccess) {
      getValidationErrors(model.get, request.body) match {
        case Some(errorMsg) => Future.successful(BadRequest(errorMsg))
        case None =>
          service.processIntervention(model.get).map {
            case None                      => Created
            case Some(SaveError.Duplicate) => Conflict
            case _                         => InternalServerError
          }
      }
    } else {
      Future.successful(BadRequest)
    }
  }

  private def getValidationErrors(interventionReceived: InterventionReceived, json: JsValue): Option[JsValue] =
    if (appConfig.validateIncomingJson && !JsonSchemaValidator.validateJSONAgainstSchema(json)) {
      Some(JsString("Failed to validate JSON against schema"))
    } else { None }
}
