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

package uk.gov.hmrc.entrydeclarationintervention.utils

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class IdGeneratorSpec extends AnyWordSpecLike with Matchers with OptionValues {

  val idGenerator = new IdGenerator

  "IdGenerator" when {
    "generating submissionIds" must {
      "generate ids that are different" in {
        val num = 1000000
        val ids = for (_ <- 1 to num) yield idGenerator.generateNotificationId

        ids.toSet.size shouldBe num
      }

      "generate ids that are uuids" in {
        idGenerator.generateNotificationId should fullyMatch regex """([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})""".r
      }
    }
  }
}
