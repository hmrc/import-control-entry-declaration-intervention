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

package uk.gov.hmrc.entrydeclarationintervention.repositories

import org.mongodb.scala.model.Filters

import java.time.Instant
import java.util.UUID
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, Injecting}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.entrydeclarationintervention.logging.LoggingContext
import uk.gov.hmrc.entrydeclarationintervention.models._
import uk.gov.hmrc.entrydeclarationintervention.utils.SaveError

import scala.concurrent.ExecutionContext.Implicits.global

class InterventionRepoISpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with DefaultAwaitTimeout
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll
    with Injecting {

  lazy val repository: InterventionRepoImpl = inject[InterventionRepoImpl]

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(repository.removeAll())
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure("metrics.enabled" -> "false", "response.max.list" -> listInterventionsLimit.toString)
    .build()

  val listInterventionsLimit     = 2
  val notificationId1            = "notificationId1"
  val correlationId              = "correlationId"
  val submissionId               = "submissionId"
  val eori                       = "eori"
  val acknowledgedNotificationId = "acknowledgedNotificationId"
  val acknowledgedCorrelationId  = "acknowledgedCorrelationId"
  val acknowledgedSubmissionId   = "acknowledgedsubmissionId"
  val acknowledgedEori           = "acknowledgedEori"
  val interventionXml            = "somexml"
  val receivedDateTime: Instant  = Instant.parse("2020-12-31T23:59:00Z")
  val housekeepingAt: Instant    = Instant.parse("2021-12-31T23:59:00Z")

  implicit val lc: LoggingContext = LoggingContext("eori", "correlationId", "submissionId", "notificationId", "mrn")

  def intervention(
    notificationId: String    = notificationId1,
    receivedDateTime: Instant = receivedDateTime): InterventionModel = InterventionModel(
    eori,
    notificationId,
    correlationId,
    acknowledged = false,
    receivedDateTime,
    housekeepingAt,
    submissionId,
    interventionXml
  )

  val acknowledgedIntervention: InterventionModel = InterventionModel(
    acknowledgedEori,
    acknowledgedNotificationId,
    acknowledgedCorrelationId,
    acknowledged = true,
    receivedDateTime,
    housekeepingAt,
    acknowledgedSubmissionId,
    interventionXml
  )

  def lookupIntervention(notificationId: String): Option[InterventionModel] =
    await(repository
      .collection
      .find(Filters.equal("notificationId", notificationId))
      .toFuture()
      .map(_.map(_.toIntervention))).headOption

  "InterventionRepo" when {
    "saving an intervention" when {

      "successful" must {
        "return None" in {
          await(repository.save(intervention())) shouldBe None
        }

        "store it in the database" in {
          lookupIntervention(notificationId1) shouldBe Some(intervention())
        }
      }

      "successful with Acknowledged True" must {
        "return None" in {
          await(repository.save(acknowledgedIntervention)) shouldBe None
        }

        "store it in the database" in {
          lookupIntervention(acknowledgedNotificationId) shouldBe Some(acknowledgedIntervention)
        }
      }

      "unique submissionId + notificationId constraint would be violated" must {
        "return duplicate" in {
          val duplicate = intervention().copy(eori = "otherEori")
          await(repository.save(duplicate)) shouldBe Some(SaveError.ServerError)
        }
      }

      "unique eori + notificationId constraint would be violated" must {
        "return duplicate error" in {
          val duplicate = intervention().copy(submissionId = "other")
          await(repository.save(duplicate)) shouldBe Some(SaveError.ServerError)
        }
      }
    }

    "looking up by eori and notification id" when {
      "it exists in the database" must {
        "return it" in {
          await(repository.lookupIntervention(eori, notificationId1)) shouldBe Some(intervention())
        }
      }

      "it exists in the database and has been acknowledged" must {
        "ignore it" in {
          await(repository.save(acknowledgedIntervention))
          await(repository.lookupIntervention(acknowledgedEori, acknowledgedNotificationId)) shouldBe None
        }
      }

      "it does not exist in the database" must {
        "return None" in {
          await(repository.lookupIntervention("unknownEori", "unknownId")) shouldBe None
        }
      }

      "it does not exist in the database for the eori" must {
        "return None" in {
          await(repository.lookupIntervention("otherEori", notificationId1)) shouldBe None
        }
      }
    }

    "looking up full intervention by eori and notification id" when {
      "it exists in the database" must {
        "return it" in {
          await(repository.lookupFullIntervention(eori, notificationId1)) shouldBe Some(intervention())
        }
      }

      "it exists in the database and has been acknowledged" must {
        "return it" in {
          await(repository.save(acknowledgedIntervention))
          await(repository.lookupFullIntervention(acknowledgedEori, acknowledgedNotificationId)) shouldBe
            Some(acknowledgedIntervention)
        }
      }

      "it does not exist in the database" must {
        "return None" in {
          await(repository.lookupFullIntervention("unknownEori", "unknownId")) shouldBe None
        }
      }
    }

    "acknowledging an intervention" when {
      "intervention exists and is unacknowledged" must {
        "return the updated intervention" in {
          await(repository.acknowledgeIntervention(eori, notificationId1)) shouldBe Some(
            intervention().copy(acknowledged = true))
        }
        "update it" in {
          lookupIntervention(notificationId1) shouldBe Some(intervention().copy(acknowledged = true))
        }
      }

      "intervention does not exist" must {
        "return None" in {
          await(repository.acknowledgeIntervention("unknownEori", "unknownId")) shouldBe None
        }
      }

      "intervention does not exist for the eori" must {
        "return None" in {
          await(repository.acknowledgeIntervention("otherEori", notificationId1)) shouldBe None
        }
      }

      "intervention exists and is already acknowledged" must {
        "return None" in {
          await(repository.acknowledgeIntervention(eori, notificationId1)) shouldBe None
        }
      }
    }

    "listing interventions" when {
      def correlationId(i: Int): String  = s"corId$i"
      def notificationId(i: Int): String = s"notId$i"

      val listedIntervention =
        InterventionModel(
          "testEori",
          notificationId(1),
          correlationId(1),
          acknowledged = false,
          receivedDateTime,
          housekeepingAt,
          "subId1",
          interventionXml)

      "unacknowledged messages exist" should {
        "return a sequence of the messages" in {
          await(repository.removeAll())
          await(repository.save(listedIntervention))
          await(
            repository.save(listedIntervention
              .copy(notificationId = notificationId(2), correlationId = correlationId(2), submissionId = "subId2")))
          await(repository.listInterventions("testEori")) shouldBe List(
            InterventionIds(correlationId(1), notificationId(1)),
            InterventionIds(correlationId(2), notificationId(2)))
        }
        "return a sequence of the messages in order of the receivedDateTime" in {
          await(repository.removeAll())
          val time1 = Instant.parse("2020-12-31T23:59:00.001Z")
          val time2 = Instant.parse("2020-12-31T23:59:00.002Z")

          await(repository.save(listedIntervention.copy(receivedDateTime = time2)))
          await(
            repository.save(listedIntervention
              .copy(notificationId = notificationId(2), correlationId = correlationId(2), receivedDateTime = time1)))
          await(repository.listInterventions("testEori")) shouldBe
            List(
              InterventionIds(correlationId(2), notificationId(2)),
              InterventionIds(correlationId(1), notificationId(1)))
        }
        "return a sequence of the messages in order of the receivedDateTime when oldest ends in 0" in {
          await(repository.removeAll())
          val time1 = Instant.parse("2020-12-31T23:59:00.000Z")
          val time2 = Instant.parse("2020-12-31T23:59:00.002Z")

          await(repository.save(listedIntervention.copy(receivedDateTime = time2)))
          await(
            repository.save(listedIntervention
              .copy(notificationId = notificationId(2), correlationId = correlationId(2), receivedDateTime = time1)))
          await(repository.listInterventions("testEori")) shouldBe
            List(
              InterventionIds(correlationId(2), notificationId(2)),
              InterventionIds(correlationId(1), notificationId(1)))
        }
        //Limit is set to 2 in app startup
        "limit the number of messages to the value set in appConfig" in {
          await(repository.removeAll())
          await(repository.save(listedIntervention))
          await(
            repository.save(
              listedIntervention.copy(notificationId = notificationId(2), correlationId = correlationId(2))))
          await(
            repository.save(
              listedIntervention.copy(notificationId = notificationId(2), correlationId = correlationId(3))))
          await(repository.listInterventions("testEori")).length shouldBe listInterventionsLimit
        }

        "limit the number of messages to the oldest receivedDateTime" in {
          val time1 = Instant.parse("2020-12-31T23:59:00.001Z")
          val time2 = Instant.parse("2020-12-31T23:59:00.002Z")
          val time3 = Instant.parse("2020-12-31T23:59:00.003Z")

          await(repository.removeAll())
          await(
              repository.save(listedIntervention
                .copy(notificationId = notificationId(1), correlationId = correlationId(1), receivedDateTime = time2)))
          await(
              repository.save(listedIntervention
                .copy(notificationId = notificationId(2), correlationId = correlationId(2), receivedDateTime = time3)))
          await(
              repository.save(listedIntervention
                .copy(notificationId = notificationId(3), correlationId = correlationId(3), receivedDateTime = time1)))
          await(repository.listInterventions("testEori")) shouldBe List(
            InterventionIds(correlationId(3), notificationId(3)),
            InterventionIds(correlationId(1), notificationId(1)))
        }
      }
      "no unacknowledged messages exist" must {
        "return Empty List" in {
          await(repository.listInterventions("unknowntestEori")) shouldBe List.empty[InterventionIds]
        }
      }
    }
  }
  "looking up notificationIds" when {
    "a single notification exists" must {
      "return the notificationId" in {
        await(repository.removeAll())
        await(repository.save(intervention(notificationId1)))
        await(repository.lookupNotificationIds(submissionId)) shouldBe Seq(notificationId1)
      }
    }
    "multiple nofifications exist" must {
      "return all notificationIds in increasing receivedDateTime order" in {
        await(repository.removeAll())

        // Use random ids to avoid ascending index on submissionId and notificationId
        // giving us a false pass...
        val notificationIds = (1 to 10).map(_ => UUID.randomUUID.toString)

        notificationIds.zipWithIndex.foreach {
          case (notificationId, idx) =>
            await(repository.save(intervention(notificationId, receivedDateTime.plusSeconds(idx))))
        }

        await(repository.lookupNotificationIds(submissionId)) shouldBe notificationIds
      }
    }
    "no notifications exist" must {
      "return an empty list" in {
        await(repository.lookupNotificationIds("unknownSubmissionId")) shouldBe Nil
      }
    }
  }
  "expireAfterSeconds" must {
    "initially be zero" in {
      await(repository.getExpireAfterSeconds).get shouldBe 0
    }
  }
}
