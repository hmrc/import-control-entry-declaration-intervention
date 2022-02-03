/*
 * Copyright 2021 HM Revenue & Customs
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

import com.mongodb.client.model.IndexModel
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
import org.mongodb.scala.model._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.UpdateOptions
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, InterventionModel, NotificationId}
import uk.gov.hmrc.entrydeclarationintervention.utils.SaveError
import uk.gov.hmrc.mongo.{MongoComponent, ReactiveRepository}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
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
    with InterventionRepo {

  val mongoErrorCodeForDuplicate: Int = 11000

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
          .find(equal("submissionId", submissionId))
          .projection(fields(include("notificationId"), excludeId()))
          .sort(ascending("receivedDateTime"))
          .toFuture()
      )
      .map(_.map(_.notificationId))

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

  def listInterventions(eori: String)(implicit lc: LoggingContext): Future[List[InterventionIds]] =
    Mdc
      .preservingMdc(
        collection
          .find(and(equal("eori", eori), equal("acknowledged", false)))
          .projection(fields(include("correlationId", "notificationId")))
          .sort(ascending("receivedDateTime"))
          .batchSize(appConfig.listInterventionsLimit)
          .collect()
          .toFutureOption()
          .map {
            case Some(list) => list.map(intervention => InterventionIds(intervention.correlationId, intervention.notificationId))
            case None =>
              ContextLogger.error("No results for listInterventions")
              Seq.empty[InterventionIds]
          }
          .map(_.toList)
      )

  private[repositories] def getExpireAfterSeconds: Future[Option[Long]] =
    Mdc
      .preservingMdc(collection.indexesManager.list())
      .map { indexes =>
        for {
          idx <- indexes.find(_.key.map(_._1).contains("housekeepingAt"))
          // Read the expiry from JSON (rather than BSON) so that we can control widening to Long
          // (from the more strongly typed BSON values which can be either Int32 or Int64)
          value <- Json.toJson(idx.options).as((JsPath \ "expireAfterSeconds").readNullable[Long])
        } yield value
      }
}
