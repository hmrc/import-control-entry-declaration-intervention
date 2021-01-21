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

package uk.gov.hmrc.entrydeclarationintervention.models.received

import java.time.Instant

import org.scalacheck.Arbitrary.{arbitrary, _}
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryIntervention {
  implicit val arbZonedDateTime: Arbitrary[Instant] =
    Arbitrary(Gen.choose(0, Long.MaxValue).map(ts => Instant.ofEpochMilli(ts)))

  implicit val arbitraryInterventionReceived: Arbitrary[InterventionResponse] = Arbitrary(
    for {
      submissionId        <- arbitrary[String]
      metadata            <- arbitrary[Metadata]
      parties             <- arbitrary[Parties]
      goods               <- arbitrary[Goods]
      declaration         <- arbitrary[Declaration]
      itinerary           <- arbitrary[Itinerary]
      customsIntervention <- arbitrary[CustomsIntervention]
    } yield InterventionResponse(submissionId, metadata, parties, goods, declaration, itinerary, customsIntervention)
  )

  implicit val arbitraryMetadata: Arbitrary[Metadata] = Arbitrary(
    for {
      senderEORI            <- arbitrary[String]
      senderBranch          <- arbitrary[String]
      preparationDateTime   <- arbitrary[Instant]
      messageIdentification <- arbitrary[String]
      receivedDateTime      <- arbitrary[Instant]
      correlationId         <- arbitrary[String]
    } yield
      Metadata(
        senderEORI,
        senderBranch,
        preparationDateTime,
        MessageType.IE351,
        messageIdentification,
        receivedDateTime,
        correlationId)
  )

  implicit val arbitraryCustomsIntervention: Arbitrary[CustomsIntervention] = Arbitrary(
    for {
      notificationDateTime <- arbitrary[Instant]
      // Non-empty list...
      interventions <- arbitrary[(Intervention, Seq[Intervention])].map { case (i, is) => i +: is }
    } yield CustomsIntervention(notificationDateTime, interventions)
  )

  implicit val arbitraryIntervention: Arbitrary[Intervention] = Arbitrary(
    for {
      code       <- arbitrary[String]
      itemNumber <- arbitrary[Option[String]]
      text       <- arbitrary[Option[String]]
      language   <- arbitrary[Option[String]]
    } yield Intervention(code, itemNumber, text, language)
  )

  implicit val arbitraryDocument: Arbitrary[Document] = Arbitrary(
    for {
      documentType      <- arbitrary[String]
      documentReference <- arbitrary[String]
      language          <- arbitrary[Option[String]]
    } yield Document(documentType, documentReference, language)
  )

  implicit val arbitraryAddress: Arbitrary[Address] = Arbitrary(
    for {
      streetAndNumber <- arbitrary[String]
      city            <- arbitrary[String]
      postalCode      <- arbitrary[String]
      countryCode     <- arbitrary[String]
    } yield Address(streetAndNumber, city, postalCode, countryCode)
  )

  implicit val arbitraryIdentityOfMeansOfCrossingBorder: Arbitrary[IdentityOfMeansOfCrossingBorder] = Arbitrary(
    for {
      nationality <- arbitrary[Option[String]]
      identity    <- arbitrary[String]
      language    <- arbitrary[Option[String]]
    } yield IdentityOfMeansOfCrossingBorder(identity, nationality, language)
  )

  implicit val arbitraryContainer: Arbitrary[Container] = Arbitrary(
    for {
      containerNumber <- arbitrary[String]
    } yield Container(containerNumber)
  )

  implicit val arbitraryGoodItem: Arbitrary[GoodsItem] = Arbitrary(
    for {
      itemNumber                      <- arbitrary[String]
      commercialReferenceNumber       <- arbitrary[Option[String]]
      documents                       <- arbitrary[Option[Seq[Document]]]
      containers                      <- arbitrary[Option[Seq[Container]]]
      identityOfMeansOfCrossingBorder <- arbitrary[Option[Seq[IdentityOfMeansOfCrossingBorder]]]
    } yield GoodsItem(itemNumber, commercialReferenceNumber, documents, containers, identityOfMeansOfCrossingBorder)
  )

  implicit val arbitraryGoods: Arbitrary[Goods] = Arbitrary(
    for {
      numItems   <- arbitrary[Int]
      goodsItems <- arbitrary[Option[Seq[GoodsItem]]]
    } yield Goods(numItems, goodsItems)
  )

  implicit val arbitraryTrader: Arbitrary[Trader] = Arbitrary(
    for {
      name     <- arbitrary[Option[String]]
      address  <- arbitrary[Option[Address]]
      language <- arbitrary[Option[String]]
      eori     <- arbitrary[String]
    } yield Trader(name, address, language, Some(eori))
  )

  implicit val arbitraryParties: Arbitrary[Parties] = Arbitrary(
    for {
      declarant      <- arbitrary[Trader]
      representative <- arbitrary[Option[Trader]]
      carrier        <- arbitrary[Option[Trader]]
    } yield Parties(declarant, representative, carrier)
  )

  implicit val arbitraryDeclaration: Arbitrary[Declaration] = Arbitrary(
    for {
      localReferenceNumber    <- arbitrary[String]
      movementReferenceNumber <- arbitrary[String]
      registeredDateTime      <- arbitrary[Instant]
      submittedDateTime       <- arbitrary[Instant]
      officeOfLodgement       <- arbitrary[Option[String]]
    } yield
      Declaration(
        localReferenceNumber,
        movementReferenceNumber,
        registeredDateTime,
        submittedDateTime,
        officeOfLodgement)
  )

  implicit val arbitraryOfficeOfFirstEntry: Arbitrary[OfficeOfFirstEntry] = Arbitrary(
    for {
      reference                 <- arbitrary[String]
      expectedDateTimeOfArrival <- arbitrary[Instant]
    } yield OfficeOfFirstEntry(reference, expectedDateTimeOfArrival)
  )

  implicit val arbitraryItinerary: Arbitrary[Itinerary] = Arbitrary(
    for {
      modeOfTransportAtBorder         <- arbitrary[Option[String]]
      identityOfMeansOfCrossingBorder <- arbitrary[Option[IdentityOfMeansOfCrossingBorder]]
      commercialReferenceNumber       <- arbitrary[Option[String]]
      conveyanceReference             <- arbitrary[Option[String]]
      officeOfFirstEntry              <- arbitrary[OfficeOfFirstEntry]
    } yield
      Itinerary(
        officeOfFirstEntry,
        modeOfTransportAtBorder,
        identityOfMeansOfCrossingBorder,
        commercialReferenceNumber,
        conveyanceReference)
  )

}
