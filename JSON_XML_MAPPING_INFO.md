```
<CC351A>
     <MesSenMES3> - metadata.senderEori + metadata.senderBranch
     <MesRecMES6> - metadata.senderEori + metadata.senderBranch
     <DatOfPreMES9> - metadata.preparationDateTime
     <TimOfPreMES10> - metadata.preparationDateTime
     <MesIdeMES19> - metadata.messageIdentification
     <MesTypMES20> - metadata.messageType
     <CorIdeMES25> - metadata.correlationId
     <HEAHEA>
       <RefNumHEA4> - declaration.localReferenceNumber
       <DocNumHEA5> - declaration.movementReferenceNumber
       <TraModAtBorHEA76> - itinerary.modeOfTransportAtBorder
       <NatHEA001> - itinerary.identityOfMeansOfCrossingBorder.nationality
       <IdeOfMeaOfTraCroHEA85> - itinerary.identityOfMeansOfCrossingBorder.identity
       <TotNumOfIteHEA305> - goods.numberOfItems
       <ComRefNumHEA> - itinerary.commercialReferenceNumber
       <ConRefNumHEA> - itinerary.conveyanceReference (conveyanceReferenceNumber for consistency?)
       <NotDatTimHEA104> - customsIntervention.notificationDateTime
       <DecRegDatTimHEA115> - declaration.registeredDateTime
       <DecSubDatTimHEA118> - declaration.submittedDateTime
     <GOOITEGDS>
       <IteNumGDS7> - goods.goodsItems[].itemNumber
       <ComRefNumGIM1> - goods.goodsItems[].commercialReferenceNumber
       <PRODOCDC2>
         <DocTypDC21> - goods.goodsItems[].documents[].type
         <DocRefDC23> - goods.goodsItems[].documents[].reference
         <DocRefDCLNG> - goods.goodsItems[].documents[].language
       <CONNR2>
         <ConNumNR21> - goods.goodsItems[].containers[].containerNumber
       <IDEMEATRAGI970>
         <NatIDEMEATRAGI973> - goods.goodsItems[].identityOfMeansOfCrossingBorder[].nationality
         <IdeMeaTraGIMEATRA971> - goods.goodsItems[].identityOfMeansOfCrossingBorder[].identity
         <IdeMeaTraGIMEATRA972LNG> - goods.goodsItems[].identityOfMeansOfCrossingBorder[].language
     <CUSOFFLON>
       <RefNumCOL1> - declaration.officeOfLodgement
     <TRAREP> - parties.representative
       <NamTRE1>
       <StrAndNumTRE1>
       <PosCodTRE1>
       <CitTRE1>
       <CouCodTRE1>
       <TRAREPLNG>
       <TINTRE1>
     <PERLODSUMDEC> - parties.declarant
       <NamPLD1>
       <StrAndNumPLD1>
       <PosCodPLD1>
       <CitPLD1>
       <CouCodPLD1>
       <PERLODSUMDECLNG>
       <TINPLD1>
     <CUSOFFFENT730>
       <RefNumCUSOFFFENT731> - itinerary.officeOfFirstEntry.reference
       <ExpDatOfArrFIRENT733> - itinerary.officeOfFirstEntry.expectedDateTimeOfArrival
     <TRACARENT601> - parties.carrier
       <NamTRACARENT604>
       <StrNumTRACARENT607>
       <PstCodTRACARENT606>
       <CtyTRACARENT603>
       <CouCodTRACARENT605>
       <TRACARENT601LNG>
       <TINTRACARENT602>
     <CUSINT632>
       <IteNumConCUSINT668> - customsIntervention.interventions[].itemNumber
       <CusIntCodCUSINT665> - customsIntervention.interventions[].code
       <CusIntTexCUSINT666> - customsIntervention.interventions[].text
       <CusIntTexCUSINT667LNG> - customsIntervention.interventions[].language
```