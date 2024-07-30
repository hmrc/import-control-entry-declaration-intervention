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
import sbt.*

object AppDependencies {
  val bootstrapVersion = "8.6.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % "2.2.0",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.github.fge"    %  "json-schema-validator"     % "2.2.14",
    "org.typelevel"     %% "cats-core"                 % "2.12.0",
    "com.chuusai"       %% "shapeless"                 % "2.3.12"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.1",
    "org.scalamock"          %% "scalamock"              % "5.2.0",
    "org.scalatestplus"      %% "scalacheck-1-18"        % "3.2.19.0"
  ).map(_ % Test)

  val itDependencies: Seq[ModuleID] = Seq()

}
