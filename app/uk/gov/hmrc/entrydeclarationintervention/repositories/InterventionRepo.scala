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

package uk.gov.hmrc.entrydeclarationintervention.repositories

import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Projections.{excludeId, fields, include}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsPath, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import org.mongodb.scala._
import org.mongodb.scala.bson.{BsonNumber, BsonValue}
import org.mongodb.scala.model._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.UpdateOptions
import play.api.Logging
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, InterventionModel, NotificationId}
import uk.gov.hmrc.entrydeclarationintervention.utils.SaveError
import uk.gov.hmrc.mongo.{MongoComponent, ReactiveRepository}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.play.http.logging.Mdc

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

trait InterventionRepo {
  def save(intervention: InterventionModel)(implicit lc: LoggingContext): Future[Option[SaveError]]

  def lookupNotificationIds(submissionId: String): Future[Seq[String]]

  def lookupIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]]

  def lookupFullIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]]

  /**
    * @return the acknowledged intervention
    */
  def acknowledgeIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]]

  def listInterventions(eori: String): Future[List[InterventionIds]]

}

@Singleton
class InterventionRepoImpl @Inject()(appConfig: AppConfig)(implicit mongo: MongoComponent, ec: ExecutionContext)
    extends PlayMongoRepository[InterventionPersisted](
      collectionName = "intervention",
      mongoComponent = mongo,
      domainFormat = InterventionPersisted.format,
      indexes = Seq(
        IndexModel(
          ascending("submissionId", "notificationId"),
          IndexOptions().name("lookupNotificationIdIndex").unique(true)
        ),
        IndexModel(
          ascending("housekeepingAt"),
          IndexOptions().name("housekeepingIndex").expireAfter(0, TimeUnit.SECONDS)
        ),
        IndexModel(
          ascending("eori", "acknowledged", "receivedDateTime", "notificationId", "correlationId"),
          IndexOptions().name("listIndex")
        ),
        IndexModel(
          ascending("eori", "notificationId"),
          IndexOptions().name("eoriPlusNotificationIdIndex").unique(true)
        )
      ))
    with InterventionRepo with Logging {

  val mongoErrorCodeForDuplicate: Int = 11000

  def removeAll(): Future[Unit] =
    collection
      .deleteMany(exists("_id"))
      .toFutureOption
      .map(_ => ())

  def save(intervention: InterventionModel)(implicit lc: LoggingContext): Future[Option[SaveError]] = {
    val interventionPersisted = InterventionPersisted.from(intervention)

    Mdc
      .preservingMdc(collection.insertOne(interventionPersisted).toFuture())
      .map(_ => None)
      .recover {
        case e: DatabaseException =>
          ContextLogger.error("Unable to save entry declaration intervention", e)
          Some(SaveError.ServerError)
      }
  }

  def lookupNotificationIds(submissionId: String): Future[Seq[String]] =
    Mdc
      .preservingMdc(
        collection
          .find[BsonValue](equal("submissionId", submissionId))
          .projection(fields(include("notificationId"), excludeId()))
          .collect
          .toFutureOption
          .map{
            case None => Nil
            case Some(l) => l.map(Codecs.fromBson[NotificationId](_).value).toList
          }
      )

  def lookupIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    Mdc
      .preservingMdc(
        collection
          .find(and(equal("eori", eori), equal("notificationId", notificationId), equal("acknowledged", false)))
          .first()
          .toFutureOption()
      ).map(_.map(_.toIntervention))


  def lookupFullIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    Mdc
      .preservingMdc(
        collection
          .find(and(equal("eori", eori), equal("notificationId", notificationId)))
          .first()
          toFutureOption()
      ).map(_.map(_.toIntervention))

  def acknowledgeIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    Mdc
      .preservingMdc(
        collection.findOneAndUpdate(
          and(equal("eori", eori), equal("notificationId", notificationId), equal("acknowledged", false)),
          set("acknowledged", true),
          FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        ).toFutureOption()
      ).map(_.map(_.toIntervention))

  def listInterventions(eori: String): Future[List[InterventionIds]] =
    Mdc
      .preservingMdc(
        collection
          .find[BsonValue](and(equal("eori", eori), equal("acknowledged", false)))
          .projection(fields(include("correlationId", "notificationId")))
          .sort(ascending("receivedDateTime"))
          .limit(appConfig.listInterventionsLimit)
          .collect()
          .toFutureOption()
          .map {
            case Some(list) => list.map { bsonValue =>
              val intervention = Codecs.fromBson[InterventionIds](bsonValue)

              InterventionIds(intervention.correlationId, intervention.notificationId)
            }
            case None =>
              logger.error("No results for listInterventions")
              Seq.empty[InterventionIds]
          }
          .map(_.toList)
      )

  private[repositories] def getExpireAfterSeconds: Future[Option[Long]] =
    Mdc
      .preservingMdc(
        collection
          .listIndexes()
          .collect
          .toFutureOption
      )
      .map {
        case None => None
        case Some(indexes) =>
          indexes.find(_.containsKey("expireAfterSeconds")).map{idx =>
            idx.get("expireAfterSeconds") match {
              case Some(value: BsonNumber) => value.longValue()
              case _ => 0L
            }
          }
      }
}
