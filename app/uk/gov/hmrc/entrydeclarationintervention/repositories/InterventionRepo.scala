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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsPath, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.entrydeclarationintervention.config.AppConfig
import uk.gov.hmrc.entrydeclarationintervention.logging.{ContextLogger, LoggingContext}
import uk.gov.hmrc.entrydeclarationintervention.models.{InterventionIds, InterventionModel, NotificationId}
import uk.gov.hmrc.entrydeclarationintervention.utils.SaveError
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.http.logging.Mdc

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
class InterventionRepoImpl @Inject()(appConfig: AppConfig)(implicit mongo: ReactiveMongoComponent, ec: ExecutionContext)
    extends ReactiveRepository[InterventionPersisted, BSONObjectID](
      "intervention",
      mongo.mongoConnector.db,
      InterventionPersisted.format,
      ReactiveMongoFormats.objectIdFormats)
    with InterventionRepo {

  override def indexes: Seq[Index] = Seq(
    Index(
      Seq(("submissionId", Ascending), ("notificationId", Ascending)),
      name   = Some("lookupNotificationIdIndex"),
      unique = true),
    //TTL index
    Index(
      Seq("housekeepingAt" -> Ascending),
      name    = Some("housekeepingIndex"),
      options = BSONDocument("expireAfterSeconds" -> 0)),
    // Covering index for list...
    Index(
      Seq(
        ("eori", Ascending),
        ("acknowledged", Ascending),
        ("receivedDateTime", Ascending),
        ("notificationId", Ascending),
        ("correlationId", Ascending)),
      name = Some("listIndex")
    ),
    Index(
      Seq(("eori", Ascending), ("notificationId", Ascending)),
      name   = Some("eoriPlusNotificationIdIndex"),
      unique = true)
  )

  val mongoErrorCodeForDuplicate: Int = 11000

  def save(intervention: InterventionModel)(implicit lc: LoggingContext): Future[Option[SaveError]] = {
    val interventionPersisted = InterventionPersisted.from(intervention)

    Mdc
      .preservingMdc(insert(interventionPersisted))
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
          .find(Json.obj("submissionId" -> submissionId), Some(Json.obj("notificationId" -> 1)))
          .sort(Json.obj("receivedDateTime" -> 1))
          .cursor[NotificationId](ReadPreference.primaryPreferred)
          .collect(maxDocs = -1, FailOnError[Seq[NotificationId]]()))
      .map(_.map(_.value))

  def lookupIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    Mdc
      .preservingMdc(
        collection
          .find(
            Json.obj("eori" -> eori, "notificationId" -> notificationId, "acknowledged" -> false),
            Option.empty[JsObject])
          .one[InterventionPersisted])
      .map(_.map(_.toIntervention))

  def lookupFullIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    Mdc
      .preservingMdc(
        collection
          .find(Json.obj("eori" -> eori, "notificationId" -> notificationId), Option.empty[JsObject])
          .one[InterventionPersisted])
      .map(_.map(_.toIntervention))

  def acknowledgeIntervention(eori: String, notificationId: String): Future[Option[InterventionModel]] =
    Mdc
      .preservingMdc(
        findAndUpdate(
          query          = Json.obj("eori" -> eori, "notificationId" -> notificationId, "acknowledged" -> false),
          update         = Json.obj("$set" -> Json.obj("acknowledged" -> true)),
          fetchNewObject = true
        ))
      .map(result => result.result[InterventionPersisted].map(_.toIntervention))

  def listInterventions(eori: String): Future[List[InterventionIds]] =
    Mdc
      .preservingMdc(
        collection
          .find(
            Json.obj("eori" -> eori, "acknowledged" -> false),
            Some(Json.obj("correlationId" -> 1, "notificationId" -> 1)))
          .sort(Json.obj("receivedDateTime" -> 1))
          .cursor[InterventionIds]()
          .collect[List](maxDocs = appConfig.listInterventionsLimit, err = Cursor.FailOnError[List[InterventionIds]]()))

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
