/*
 * Copyright 2025 HM Revenue & Customs
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
import sbt.*

object AppDependencies {
  val bootstrapVersion = "10.8.0"

  private val jacksonVersion = "2.15.3"

  val jacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core"       % "jackson-core"            % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-databind"        % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-annotations"     % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
    "com.fasterxml.jackson.datatype"   % "jackson-datatype-jsr310" % jacksonVersion
  )

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"        % "2.13.0",
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.networknt"                %  "json-schema-validator"     % "2.0.7",
    "org.typelevel"                %% "cats-core"                 % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.2",
    "org.scalamock"          %% "scalamock"              % "7.5.5",
    "org.scalatestplus"      %% "scalacheck-1-18"        % "3.2.19.0"
  ).map(_ % Test)

  val itDependencies: Seq[ModuleID] = Seq()

}
