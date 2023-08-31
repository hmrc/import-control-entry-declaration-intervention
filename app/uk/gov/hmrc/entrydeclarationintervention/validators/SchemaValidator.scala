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

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.{Schema, SchemaFactory}
import org.xml.sax.{ErrorHandler, InputSource}
import uk.gov.hmrc.entrydeclarationintervention.utils.ResourceUtils

import scala.xml.{Node, SAXParseException}

trait ValidationResult {
  def isValid: Boolean

  def allErrors: Seq[SAXParseException]
}

class SchemaValidator {

  private[SchemaValidator] class ValidationResultImpl extends ErrorHandler with ValidationResult {

    var warnings = Vector.empty[SAXParseException]

    var errors = Vector.empty[SAXParseException]

    var fatalErrors = Vector.empty[SAXParseException]

    override def warning(ex: SAXParseException): Unit =
      warnings = warnings :+ ex

    override def error(ex: SAXParseException): Unit =
      errors = errors :+ ex

    override def fatalError(ex: SAXParseException): Unit =
      fatalErrors = fatalErrors :+ ex

    def isValid: Boolean = errors.isEmpty && fatalErrors.isEmpty

    def allErrors: Seq[SAXParseException] = errors ++ fatalErrors
  }

  val schema: Schema = {
    val schemaLang = XMLConstants.W3C_XML_SCHEMA_NS_URI
    val resource   = ResourceUtils.url("xsds/CC351A-v10-0.xsd")
    SchemaFactory.newInstance(schemaLang).newSchema(resource)
  }

  def validateSchema(xml: Node): ValidationResult = {
    val factory = SAXParserFactory.newInstance() //Parser for XML to check if valid XML

    factory.setNamespaceAware(true)
    factory.setSchema(schema)

    val reader = factory.newSAXParser().getXMLReader

    val validationResult = new ValidationResultImpl
    reader.setErrorHandler(validationResult)

    reader.parse(new InputSource(new ByteArrayInputStream(xml.toString.getBytes(StandardCharsets.UTF_8))))

    validationResult
  }
}
