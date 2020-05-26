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

  val compile = Seq(
    "uk.gov.hmrc"    %% "simple-reactivemongo" % "7.23.0-play-26",
    "uk.gov.hmrc"    %% "bootstrap-play-26"    % "1.5.0",
    "uk.gov.hmrc"    %% "play-hmrc-api"        % "4.1.0-play-26",
    "com.github.fge" % "json-schema-validator" % "2.2.6",
    "org.typelevel"  %% "cats-core"            % "2.0.0",
    "com.chuusai"    %% "shapeless"            % "2.3.3"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-play-26"  % "1.5.0"         % Test classifier "tests",
    "org.scalatest"          %% "scalatest"          % "3.0.8"         % "test, it",
    "com.typesafe.play"      %% "play-test"          % current         % "test",
    "org.pegdown"            % "pegdown"             % "1.6.0"         % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"         % "test, it",
    "org.scalamock"          %% "scalamock"          % "4.4.0"         % "test, it",
    "org.scalacheck"         %% "scalacheck"         % "1.14.1"        % "test, it",
    "com.github.tomakehurst" % "wiremock"            % "2.25.1"        % "test, it",
    "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26" % "test, it"
  )
  // Fixes a transitive dependency clash after upgrading to sbt 1.3.4
  val overrides: Seq[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Seq(
      "com.typesafe.akka" %% "akka-actor"    % "2.5.23",
      "com.typesafe.akka" %% "akka-stream"   % "2.5.23",
      "com.typesafe.akka" %% "akka-protobuf" % "2.5.23",
      "com.typesafe.akka" %% "akka-slf4j"    % "2.5.23",
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