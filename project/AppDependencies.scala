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
  val bootstrapVersion = "5.25.0"

  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"      % "0.68.0",
    "uk.gov.hmrc"    %% "bootstrap-backend-play-28" % bootstrapVersion,
    "com.github.fge" %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"  %% "cats-core"                 % "2.8.0",
    "com.chuusai"    %% "shapeless"                 % "2.3.9"
  )

  val test = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % bootstrapVersion % "test, it",
    "com.typesafe.play"            %% "play-test"              % current          % "test",
    "org.pegdown"                  %  "pegdown"                % "1.6.0"          % "test, it",
    "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"          % "test, it",
    "org.scalamock"                %% "scalamock"              % "5.2.0"          % "test, it",
    "org.scalatestplus"            %% "scalacheck-1-15"        % "3.2.11.0"       % "test, it",
    "com.github.tomakehurst"       %  "wiremock"               % "2.33.2"         % "test, it",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.3"         % "test, it"
  )

}
