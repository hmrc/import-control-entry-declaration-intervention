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

package uk.gov.hmrc.entrydeclarationintervention.services

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import uk.gov.hmrc.entrydeclarationintervention.models.received._

import scala.xml.{Elem, Node}

object XMLBuilder {
  // All dates and time in XML are UTC - make this explicit here...
  private val xsdDateFormatter     = DateTimeFormatter.ofPattern("uuMMdd").withZone(ZoneOffset.UTC)
  private val xsdTimeFormatter     = DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC)
  private val xsdDateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMddHHmm").withZone(ZoneOffset.UTC)

  def getDateFromDateTime(dt: Instant): String = xsdDateFormatter.format(dt)

  def getTimeFromDateTime(dt: Instant): String = xsdTimeFormatter.format(dt)

  def getDateTimeInXSDFormat(dt: Instant): String = xsdDateTimeFormatter.format(dt)

  def xmlMessageType(messageType: MessageType): String =
    messageType match {
      case MessageType.IE351 => "CC351A"
    }
}

class XMLBuilder {

  import XMLBuilder._

  def buildXML(intervention: InterventionResponse): Elem = {
    import intervention._
    //@formatter:off
    <cc3:CC351A xmlns:cc3="http://ics.dgtaxud.ec/CC351A">
      <MesSenMES3>{ s"${metadata.senderEORI}/${metadata.senderBranch}" }</MesSenMES3>
      <MesRecMES6>{ s"${metadata.senderEORI}/${metadata.senderBranch}" }</MesRecMES6>
      <DatOfPreMES9>{ getDateFromDateTime(metadata.preparationDateTime) }</DatOfPreMES9>
      <TimOfPreMES10>{ getTimeFromDateTime(metadata.preparationDateTime) }</TimOfPreMES10>
      <MesIdeMES19>{ metadata.messageIdentification }</MesIdeMES19>
      <MesTypMES20>{ xmlMessageType(metadata.messageType) }</MesTypMES20>
      <CorIdeMES25>{ metadata.correlationId }</CorIdeMES25>

      { getHEAHEA (intervention) }
      { for(goodsItems <- goods.goodsItems.toSeq) yield getGOOITEGDS(goodsItems) }
      { for(office <- declaration.officeOfLodgement.toSeq) yield getCUSOFFLON(office)}
      { for(rep <- parties.representative.toSeq) yield getTRAREP(rep) }
      { getPERLODSUMDEC(parties.declarant) }
      { getCUSOFFFENT730( itinerary.officeOfFirstEntry) }
      { for (carrier <- parties.carrier.toSeq) yield getTRACARENT601(carrier) }
      { getCUSINT632(customsIntervention.interventions) }
    </cc3:CC351A>
    //@formatter:on
  }

  private def getHEAHEA(intervention: InterventionResponse): Elem = {
    import intervention._
    //@formatter:off
    <HEAHEA>
      <RefNumHEA4>{ declaration.localReferenceNumber }</RefNumHEA4>
      <DocNumHEA5>{ declaration.movementReferenceNumber }</DocNumHEA5>
      {for (modeOfTransportAtBorder <- itinerary.modeOfTransportAtBorder.toSeq)
        yield <TraModAtBorHEA76>{ modeOfTransportAtBorder }</TraModAtBorHEA76>}
      {for {
        identityOfMeansOfCrossingBorder <- itinerary.identityOfMeansOfCrossingBorder.toSeq
        nationality <- identityOfMeansOfCrossingBorder.nationality.toSeq
        } yield <NatHEA001>{ nationality }</NatHEA001>}
      {for (identityOfMeansOfCrossingBorder <- itinerary.identityOfMeansOfCrossingBorder.toSeq)
        yield <IdeOfMeaOfTraCroHEA85>{ identityOfMeansOfCrossingBorder.identity }</IdeOfMeaOfTraCroHEA85>}
      <TotNumOfIteHEA305>{ goods.numberOfItems }</TotNumOfIteHEA305>
      {for (commercialReference <- itinerary.commercialReferenceNumber.toSeq)
        yield <ComRefNumHEA>{ commercialReference }</ComRefNumHEA>}
      {for (conveyanceReference <- itinerary.conveyanceReference.toSeq)
        yield <ConRefNumHEA>{ conveyanceReference }</ConRefNumHEA>}
      <NotDatTimHEA104>{ getDateTimeInXSDFormat(customsIntervention.notificationDateTime) }</NotDatTimHEA104>
      <DecRegDatTimHEA115>{ getDateTimeInXSDFormat(declaration.registeredDateTime) }</DecRegDatTimHEA115>
      <DecSubDatTimHEA118>{ getDateTimeInXSDFormat(declaration.submittedDateTime) }</DecSubDatTimHEA118>
    </HEAHEA>
    //@formatter:on
  }

  private def getGOOITEGDS(goodsItems: Seq[GoodsItem]): Seq[Node] =
    for {
      item <- goodsItems
    } yield {
      //@formatter:off
      <GOOITEGDS>
        <IteNumGDS7>{ item.itemNumber }</IteNumGDS7>
        {for (value <- item.commercialReferenceNumber.toSeq) yield <ComRefNumGIM1>{ value }</ComRefNumGIM1>}
        {for (documents <- item.documents.toSeq) yield getPRODOCDC2(documents) }
        {for (containers <- item.containers.toSeq) yield getCONNR2(containers) }
        {for (meansOfTransport <- item.identityOfMeansOfCrossingBorder.toSeq) yield getIDEMEATRAGI970(meansOfTransport) }
      </GOOITEGDS>
      //@formatter:on
    }

  private def getPRODOCDC2(documents: Seq[Document]): Seq[Node] =
    for {
      document <- documents
    } yield {
      //@formatter:off
      <PRODOCDC2>
        <DocTypDC21>{document.`type`}</DocTypDC21>
        <DocRefDC23>{document.reference}</DocRefDC23>
        {for (value <- document.language.toSeq) yield <DocRefDCLNG>{value}</DocRefDCLNG>}
      </PRODOCDC2>
      //@formatter:on
    }

  private def getCONNR2(containers: Seq[Container]): Seq[Node] =
    for {
      container <- containers
    } yield {
      //@formatter:off
      <CONNR2>
        <ConNumNR21>{container.containerNumber}</ConNumNR21>
      </CONNR2>
      //@formatter:on
    }

  private def getIDEMEATRAGI970(identities: Seq[IdentityOfMeansOfCrossingBorder]): Seq[Node] =
    for {
      identity <- identities
    } yield {
      //@formatter:off
      <IDEMEATRAGI970>
        {for (value <- identity.nationality.toSeq) yield <NatIDEMEATRAGI973>{ value }</NatIDEMEATRAGI973>}
        <IdeMeaTraGIMEATRA971>{ identity.identity }</IdeMeaTraGIMEATRA971>
        {for (value <- identity.language.toSeq) yield <IdeMeaTraGIMEATRA972LNG>{ value }</IdeMeaTraGIMEATRA972LNG>
        }
      </IDEMEATRAGI970>
      //@formatter:on
    }

  private def getCUSOFFLON(officeOfLodgement: String): Elem =
    //@formatter:off
    <CUSOFFLON>
      <RefNumCOL1>{ officeOfLodgement }</RefNumCOL1>
    </CUSOFFLON>
  //@formatter:on

  private def getTRAREP(representative: Trader): Elem =
    //@formatter:off
    <TRAREP>
      { for (value <- representative.name.toSeq) yield <NamTRE1>{ value } </NamTRE1>}
      { for ( address <- representative.address.toSeq) yield {
      <StrAndNumTRE1>{address.streetAndNumber}</StrAndNumTRE1>
        <PosCodTRE1>{address.postalCode}</PosCodTRE1>
        <CitTRE1>{address.city}</CitTRE1>
        <CouCodTRE1>{address.countryCode}</CouCodTRE1>
      }}
      { for (value <- representative.language.toSeq)  yield <TRAREPLNG>{ value }</TRAREPLNG> }
      { for (value <- representative.eori.toSeq) yield <TINTRE1>{ value }</TINTRE1> }
    </TRAREP>
  //@formatter:on

  private def getPERLODSUMDEC(declarant: Trader): Elem =
    //@formatter:off
    <PERLODSUMDEC>
      { for (value <- declarant.name.toSeq) yield <NamPLD1>{value}</NamPLD1> }
      { for (address <- declarant.address.toSeq) yield {
        <StrAndNumPLD1>{address.streetAndNumber}</StrAndNumPLD1>
        <PosCodPLD1>{address.postalCode}</PosCodPLD1>
        <CitPLD1>{address.city}</CitPLD1>
        <CouCodPLD1>{address.countryCode}</CouCodPLD1>
      }}
      { for (value <- declarant.language.toSeq) yield <PERLODSUMDECLNG>{value}</PERLODSUMDECLNG> }
      { for (value <- declarant.eori.toSeq) yield <TINPLD1>{value}</TINPLD1> }
    </PERLODSUMDEC>
  //@formatter:on

  private def getCUSOFFFENT730(officeOfFirstEntry: OfficeOfFirstEntry): Elem =
    //@formatter:off
    <CUSOFFFENT730>
      <RefNumCUSOFFFENT731>{officeOfFirstEntry.reference}</RefNumCUSOFFFENT731>
      <ExpDatOfArrFIRENT733>{getDateTimeInXSDFormat(officeOfFirstEntry.expectedDateTimeOfArrival)}</ExpDatOfArrFIRENT733>
    </CUSOFFFENT730>
  //@formatter:on

  private def getTRACARENT601(carrier: Trader): Elem =
    //@formatter:off
    <TRACARENT601>
      { for (value <- carrier.name.toSeq) yield <NamTRACARENT604>{ value } </NamTRACARENT604>}
      { for ( address <- carrier.address.toSeq) yield {
      <StrNumTRACARENT607>{address.streetAndNumber}</StrNumTRACARENT607>
        <PstCodTRACARENT606>{address.postalCode}</PstCodTRACARENT606>
        <CtyTRACARENT603>{address.city}</CtyTRACARENT603>
        <CouCodTRACARENT605>{address.countryCode}</CouCodTRACARENT605>
      }}
      { for (value <- carrier.language.toSeq)  yield <TRACARENT601LNG>{ value }</TRACARENT601LNG> }
      { for (value <- carrier.eori.toSeq) yield <TINTRACARENT602>{ value }</TINTRACARENT602> }
    </TRACARENT601>
  //@formatter:on

  private def getCUSINT632(interventions: Seq[Intervention]): Seq[Node] =
    for {
      intervention <- interventions
    } yield {
      //@formatter:off
      <CUSINT632>
        {for (value <- intervention.itemNumber.toSeq) yield <IteNumConCUSINT668>{ value }</IteNumConCUSINT668>}
        <CusIntCodCUSINT665>{ intervention.code }</CusIntCodCUSINT665>
        {for (value <- intervention.text.toSeq) yield <CusIntTexCUSINT666>{ value }</CusIntTexCUSINT666>}
        {for (value <- intervention.language.toSeq) yield <CusIntTexCUSINT667LNG>{ value }</CusIntTexCUSINT667LNG>}
      </CUSINT632>
      //@formatter:on
    }

}
