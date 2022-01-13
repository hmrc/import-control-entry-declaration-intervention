/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.entrydeclarationintervention.services

import org.scalamock.handlers.CallHandler
import org.scalamock.matchers.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Inside, OptionValues}
import play.api.mvc.Headers
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.entrydeclarationintervention.connectors.{MockApiSubscriptionFieldsConnector, MockAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

class AuthServiceSpec
  extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with MockAuthConnector
    with MockApiSubscriptionFieldsConnector
    with ScalaFutures
    with Inside {

  val X_CLIENT_ID      = "X-Client-Id"


  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(500, Millis))

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  val service          = new AuthService(mockAuthConnector, mockApiSubscriptionFieldsConnector)
  val eori             = "GB123"
  val clientId         = "someClientId"

  // WLOG - any AuthorisationException will do
  val authException = new InsufficientEnrolments with NoStackTrace
  val enrolmentKey = "HMRC-SS-ORG"
  val identifier   = "EORINumber"
  def validICSEnrolment(eori: String): Enrolment =
    Enrolment(
      key               = enrolmentKey,
      identifiers       = Seq(EnrolmentIdentifier(identifier, eori)),
      state             = "Activated",
      delegatedAuthRule = None)

  def stubAuth(implicit hc: HeaderCarrier): CallHandler[Future[Enrolments]] =
    MockAuthConnector
      .authorise(AuthProviders(AuthProvider.GovernmentGateway), Retrievals.allEnrolments, hc)

  def stubCSPAuth(implicit hc: HeaderCarrier): CallHandler[Future[Unit]] =
    MockAuthConnector.authorise(AuthProviders(AuthProvider.PrivilegedApplication), EmptyRetrieval, hc)

  "AuthService.authenticate" when {
    "X-Client-Id header present" when {
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("X-Client-Id" -> clientId)
      implicit val headers: Headers = Headers(X_CLIENT_ID -> clientId)

      "CSP authentication succeeds" when {
        "authenticated EORI present in subscription fields" when {
          "return that EORI" in {
            stubCSPAuth returns Future.successful(())
            MockApiSubscriptionFieldsConnector.getAuthenticatedEoriField(clientId) returns Future.successful(Some(eori))

            service.authenticate().futureValue shouldBe Some(eori)
          }
        }

        "no authenticated EORI present in subscription fields" when {
          "return None (without non-CSP auth)" in {
            stubCSPAuth returns Future.successful(Enrolments(Set(validICSEnrolment(eori))))
            MockApiSubscriptionFieldsConnector.getAuthenticatedEoriField(clientId) returns Future.successful(None)

            service.authenticate().futureValue shouldBe None
          }
        }
      }

      "CSP authentication fails" when {
        authenticateBasedOnSSEnrolment { () =>
          stubCSPAuth returns Future.failed(authException)
        }
      }
    }

    "no X-Client-Id header present" when {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val headers: Headers = Headers()
      authenticateBasedOnSSEnrolment { () =>
      }
    }

    "X-Client-Id header present with different case" must {
      implicit val headers: Headers = Headers(X_CLIENT_ID -> clientId)

      "Attempt CSP auth" in {
        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("x-client-id" -> clientId)
        stubCSPAuth returns Future.successful(())

        MockApiSubscriptionFieldsConnector.getAuthenticatedEoriField(clientId) returns  Future.successful(Some(eori))
        service.authenticate().futureValue shouldBe Some(eori)
      }
    }

    def authenticateBasedOnSSEnrolment(stubbings: () => Unit)(implicit hc: HeaderCarrier, headers: Headers): Unit = {

      "return Some(eori)" when {
        "S&S enrolment with an eori" in {
          stubbings()
          stubAuth returns Future.successful(Enrolments(Set(validICSEnrolment(eori))))
          service.authenticate().futureValue shouldBe Some(eori)
        }
      }

      "return None" when {
        "S&S enrolment with no identifiers" in {
          stubbings()
          stubAuth returns Future.successful(Enrolments(Set(validICSEnrolment(eori).copy(identifiers = Nil))))
          service.authenticate().futureValue shouldBe None
        }

        "no S&S enrolment in authorization header" in {
          stubbings()
          stubAuth returns Future.successful(Enrolments(
            Set(
              Enrolment(
                key               = "OTHER",
                identifiers       = Seq(EnrolmentIdentifier(identifier, eori)),
                state             = "Activated",
                delegatedAuthRule = None))))
          service.authenticate().futureValue shouldBe None
        }

        "no enrolments at all in authorization header" in {
          stubbings()
          stubAuth returns Future.successful(Enrolments(Set.empty))
          service.authenticate().futureValue shouldBe None
        }

        "S&S enrolment not activated" in {
          stubbings()
          stubAuth returns Future.successful(Enrolments(Set(validICSEnrolment(eori).copy(state = "inactive"))))
          service.authenticate().futureValue shouldBe None
        }

        "authorisation fails" in {
          stubbings()
          stubAuth returns Future.failed(authException)
          service.authenticate().futureValue shouldBe None
        }
      }
    }
  }
}