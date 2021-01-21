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

package uk.gov.hmrc.entrydeclarationintervention.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.{AppName, ServicesConfig}

import scala.concurrent.duration.{Duration, FiniteDuration}

trait AppConfig {

  def authBaseUrl: String

  def appName: String

  def apiGatewayContext: String

  def apiEndpointsEnabled: Boolean

  def apiStatus: String

  def allowListEnabled: Boolean

  def allowListApplicationIds: Seq[String]

  def eisInboundBearerToken: String

  def listInterventionsLimit: Int

  def auditingEnabled: Boolean

  def graphiteHost: String

  def apiSubscriptionFieldsHost: String

  def validateIncomingJson: Boolean

  def validateJsonToXMLTransformation: Boolean

  def defaultTtl: FiniteDuration

  def newSSEnrolmentEnabled: Boolean
}

@Singleton
class AppConfigImpl @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig {

  private final def getFiniteDuration(config: Configuration, path: String): FiniteDuration = {
    val string = config.get[String](path)

    Duration.create(string) match {
      case f: FiniteDuration => f
      case _                 => throw new RuntimeException(s"Not a finite duration '$string' for $path")
    }
  }

  val authBaseUrl: String  = servicesConfig.baseUrl("auth")
  lazy val appName: String = AppName.fromConfiguration(config)

  lazy val apiGatewayContext: String    = config.get[String]("api.gateway.context")
  lazy val apiEndpointsEnabled: Boolean = config.get[Boolean]("api.endpoints.enabled")
  lazy val apiStatus: String            = config.get[String]("api.status")

  lazy val allowListEnabled: Boolean = config.get[Boolean]("api.allowList.enabled")

  lazy val allowListApplicationIds: Seq[String] = config.get[Seq[String]]("api.allowList.applicationIds")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String     = config.get[String]("microservice.metrics.graphite.host")

  val apiSubscriptionFieldsHost: String = servicesConfig.baseUrl("api-subscription-fields")

  lazy val eisInboundBearerToken: String =
    config.get[String]("microservice.services.import-control-entry-declaration-eis.inboundBearerToken")

  lazy val listInterventionsLimit: Int   = config.get[Int]("response.max.list")
  lazy val validateIncomingJson: Boolean = config.getOptional[Boolean]("validateIncomingJson").getOrElse(false)
  lazy val validateJsonToXMLTransformation: Boolean =
    config.getOptional[Boolean]("validateJsonToXMLTransformation").getOrElse(false)

  lazy val defaultTtl: FiniteDuration = getFiniteDuration(config.get[Configuration](s"mongodb"), "defaultTtl")

  lazy val newSSEnrolmentEnabled: Boolean = config.get[Boolean]("feature-switch.new-ss-enrolment")
}
