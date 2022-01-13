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
import play.core.PlayVersion.current
import sbt._

object AppDependencies {
  val bootstrapVersion = "5.19.0"

  val compile = Seq(
    "uk.gov.hmrc"    %% "simple-reactivemongo"      % "8.0.0-play-28",
    "uk.gov.hmrc"    %% "bootstrap-backend-play-28" % bootstrapVersion,
    "com.github.fge" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"  %% "cats-core"                 % "2.7.0",
    "com.chuusai"    %% "shapeless"                 % "2.3.7"
  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % bootstrapVersion % "test, it",
    "com.typesafe.play"            %% "play-test"              % current          % "test",
    "org.pegdown"                  %  "pegdown"                % "1.6.0"          % "test, it",
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"          % "test, it",
    "org.scalamock"                %% "scalamock"              % "5.2.0"          % "test, it",
    "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.10.0"       % "test, it",
    "com.github.tomakehurst"       %  "wiremock"               % "2.32.0"         % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.1"         % "test, it"
  )
  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Seq[ModuleID] = {
    val jettyFromWiremockVersion = "9.4.44.v20210927"
    Seq(
      "com.typesafe.akka"           %% "akka-actor"                 % "2.6.17",
      "com.typesafe.akka"           %% "akka-stream"                % "2.6.17",
      "com.typesafe.akka"           %% "akka-protobuf"              % "2.6.17",
      "com.typesafe.akka"           %% "akka-slf4j"                 % "2.6.17",
      "com.typesafe.akka"           %% "akka-serialization-jackson" % "2.6.17",
      "com.typesafe.akka"           %% "akka-actor-typed"           % "2.6.17",
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }
}
