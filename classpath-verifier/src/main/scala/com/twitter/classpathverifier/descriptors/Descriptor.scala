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

package com.twitter.classpathverifier.descriptors

import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Summarizer

case class Descriptor(args: List[Type], result: Type) {
  def compatibleWith(summarize: Summarizer, decl: Descriptor)(implicit ctx: Context): Boolean =
    decl match {
      case Descriptor(formals, declResult) =>
        args.zip(formals).forall {
          case (subType, superType) =>
            subtypeOf(summarize, superType, subType)
        } &&
          subtypeOf(summarize, result, declResult)
    }

  private def subtypeOf(
      summarize: Summarizer,
      superType: Type,
      subType: Type
  )(implicit ctx: Context): Boolean =
    (superType, subType) match {
      case (a: Type.Primitive, b: Type.Primitive) => a eq b
      case (Type.Array(sup), Type.Array(sub))     => subtypeOf(summarize, sup, sub)
      case (Type.Reference(sup), Type.Reference(sub)) =>
        summarize(sub).subtypeOf(summarize, sup)
      case _ => false
    }
}
