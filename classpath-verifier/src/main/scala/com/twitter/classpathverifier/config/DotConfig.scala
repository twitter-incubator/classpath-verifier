/*
 * Copyright 2021 Twitter, Inc.
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

package com.twitter.classpathverifier.config

import java.nio.file.Path

import scopt.Read

final case class DotConfig(
    output: Option[Path],
    granularity: DotConfig.Granularity,
    packageFilter: Set[String]
)

object DotConfig {
  sealed trait Granularity
  object Granularity {
    case object Package extends Granularity
    case object Class extends Granularity
    case object Method extends Granularity

    implicit val GranularityReader: Read[Granularity] =
      Read.reads(_.toLowerCase match {
        case "package" => Package
        case "class"   => Class
        case "method"  => Method
        case other     => throw new IllegalArgumentException(s"Unknown granularity: $other")
      })
  }

  val empty: DotConfig = DotConfig(
    output = None,
    granularity = Granularity.Class,
    packageFilter = Set.empty
  )
}
