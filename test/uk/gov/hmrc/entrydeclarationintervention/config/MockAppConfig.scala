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

package uk.gov.hmrc.entrydeclarationintervention.config

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory

import scala.concurrent.duration.FiniteDuration

trait MockAppConfig extends MockFactory {
  val mockAppConfig: AppConfig = mock[AppConfig]

  object MockAppConfig {
    def appName: CallHandler[String] = (() => mockAppConfig.appName).expects()

    def apiSubscriptionFieldsHost: CallHandler[String] = (() => mockAppConfig.apiSubscriptionFieldsHost).expects()

    def apiGatewayContext: CallHandler[String] = (() => mockAppConfig.apiGatewayContext).expects()

    def apiStatus: CallHandler[String] = (() => mockAppConfig.apiStatus).expects()

    def apiEndpointsEnabled: CallHandler[Boolean] = (() => mockAppConfig.apiEndpointsEnabled).expects()

    def eisInboundBearerToken: CallHandler[String] = (() => mockAppConfig.eisInboundBearerToken).expects()

    def listInterventionsLimit: CallHandler[Int] = (() => mockAppConfig.listInterventionsLimit).expects()

    def validateIncomingJson: CallHandler[Boolean] = (() => mockAppConfig.validateIncomingJson).expects()

    def validateJsonToXMLTransformation: CallHandler[Boolean] =
      (() => mockAppConfig.validateJsonToXMLTransformation).expects()

    def defaultTtl: CallHandler[FiniteDuration] = (() => mockAppConfig.defaultTtl).expects()

    def optionalFieldsFeature: CallHandler[Boolean] = (() => mockAppConfig.optionalFieldsFeature).expects()
  }
}
