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

package uk.gov.hmrc.entrydeclarationintervention.validators

import java.net.URL
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{Error as SchemaError, Schema, SchemaRegistry, SpecificationVersion}
import play.api.libs.json.JsValue
import uk.gov.hmrc.entrydeclarationintervention.logging.{ContextLogger, LoggingContext}

import java.io.FileInputStream
import scala.jdk.CollectionConverters.*
import scala.util.Using

object JsonSchemaValidator {

  private val mapper: ObjectMapper = new ObjectMapper()

  private val registry: SchemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4)

  val basePath: String = System.getProperty("user.dir")

  def validateJSONAgainstSchema(inputDoc: JsValue, schemaDoc: String = "conf/jsonSchemas/AdvancedIntervention.json")(
    using lc: LoggingContext): Boolean =
    try {
      val inputJson: JsonNode = mapper.readTree(inputDoc.toString())

      val schema: Schema =
        Using.resource(new FileInputStream(s"$basePath/$schemaDoc"))(in => registry.getSchema(in))

      val errors: Seq[SchemaError] = schema.validate(inputJson).asScala.toSeq
      if (errors.nonEmpty) ContextLogger.error(s"Failed to validate $inputDoc and $errors")

      errors.isEmpty
    } catch {
      case e: Exception =>
        ContextLogger.error(s"Failed to validate $inputDoc", e)
        false
    }

  def url(resourceName: String): URL = Thread.currentThread().getContextClassLoader.getResource(resourceName)
}
