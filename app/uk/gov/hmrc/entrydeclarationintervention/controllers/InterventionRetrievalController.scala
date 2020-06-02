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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, NotificationId, StandardError}
import uk.gov.hmrc.entrydeclarationintervention.services.{AuthService, InterventionRetrievalService}

import scala.concurrent.ExecutionContext

@Singleton
class InterventionRetrievalController @Inject()(
  val authService: AuthService,
  cc: ControllerComponents,
  service: InterventionRetrievalService)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  def listInterventions(): Action[AnyContent] = authorisedAction().async { userRequest =>
    service.listInterventions(userRequest.eori).map {
      case Nil                  => NoContent
      case interventionMetadata => Ok(listXml(interventionMetadata)).as(MimeTypes.XML)
    }
  }

  def getIntervention(notificationId: String): Action[AnyContent] = authorisedAction().async { implicit userRequest =>
    service.retrieveIntervention(userRequest.eori, notificationId) map {
      case None               => NotFound(StandardError.notFound)
      case Some(intervention) => Ok(intervention.interventionXml).as(MimeTypes.XML)
    }
  }

  def acknowledgeIntervention(notificationId: String): Action[AnyContent] = authorisedAction().async {
    implicit userRequest =>
      service.acknowledgeIntervention(userRequest.eori, notificationId) map {
        case None    => NotFound(StandardError.notFound)
        case Some(_) => Ok
      }
  }

  private def listXml(interventionIdList: List[InterventionIds]): String = {
    val prettyPrinter = new scala.xml.PrettyPrinter(100, 4)

    val xml = <advancedNotifications>{interventionIdList.map { interventionIds =>
    <response>
     <correlationId>{interventionIds.correlationId}</correlationId>
      <notificationId>{interventionIds.notificationId}</notificationId>
     <link>/customs/imports/notifications/{interventionIds.notificationId}</link>
    </response>
   }}</advancedNotifications>

    prettyPrinter.format(xml)
  }
}
