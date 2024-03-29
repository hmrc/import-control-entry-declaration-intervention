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

import org.apache.pekko.util.Timeout
import com.codahale.metrics.MetricRegistry
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Logging
import play.api.test.Helpers.await

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

class TimerSpec extends AnyWordSpecLike with Matchers with OptionValues with Timer with Logging {
  val metrics: MetricRegistry = new MockMetrics

  var timeMs: Long = _

  override def stopAndLog[A](name: String, timer: com.codahale.metrics.Timer.Context): Unit =
    timeMs = timer.stop() / 1000000

  "Timer" should {
    val sleepMs = 300
    val timeout : Timeout = FiniteDuration(400, MILLISECONDS)

    "Time a future correctly" in {
      await(timeFuture("test timer", "test.sleep") {
        Thread.sleep(sleepMs)
        Future.successful((): Unit)
      })(timeout)
      val beWithinTolerance = be >= sleepMs.toLong and be <= (sleepMs + 100).toLong
      timeMs should beWithinTolerance
    }

    "Time a block correctly" in {
      await(time("test timer", "test.sleep") {
        Thread.sleep(sleepMs)
        Future.successful((): Unit)
      })(timeout)
      val beWithinTolerance = be >= sleepMs.toLong and be <= (sleepMs + 100).toLong
      timeMs should beWithinTolerance
    }
  }
}
