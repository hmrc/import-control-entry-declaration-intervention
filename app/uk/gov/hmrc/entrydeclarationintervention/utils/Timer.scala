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

import com.codahale.metrics._
import play.api.Logging
import com.codahale.metrics.MetricRegistry

import scala.concurrent.{ExecutionContext, Future}

trait Timer {
  self: Logging =>
  type Metric = String

  val metrics: MetricRegistry
  val localMetrics = new LocalMetrics

  class LocalMetrics {
    def startTimer(metric: Metric): Timer.Context = metrics.timer(s"$metric-timer").time()
  }

  def timeFuture[A](name: String, metric: Metric)(block: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    val timer = localMetrics.startTimer(metric)
    block andThen { case _ => stopAndLog(name, timer) }
  }

  def time[A](name: String, metric: Metric)(block: => A): A = {
    val timer = localMetrics.startTimer(metric)
    try block
    finally stopAndLog(name, timer)
  }

  protected def stopAndLog[A](name: String, timer: Timer.Context): Unit = {
    val timeMillis = timer.stop() / 1000000

    if (timeMillis > 1000) {
      logger.info(getClass.getName + s" $name took $timeMillis ms")
    } else {
      logger.debug(getClass.getName + s" $name took $timeMillis ms")
    }
  }
}
