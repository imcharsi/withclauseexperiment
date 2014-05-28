/*
 * Copyright 2014 KangWoo, Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.imcharsi.slick.withclauseexperiment

import scala.slick.ast.{ Type, Node, SimplyTypedNode }
import scala.slick.lifted.{ WrappingQuery, Query }
import scala.language.higherKinds

/**
 * Created by KangWoo,Lee on 14. 5. 27.
 */
case class SampleUnion(left: Node, right: Node) extends SimplyTypedNode {
  override type Self = SampleUnion

  override protected def buildType: Type = left.nodeType

  override def nodeChildren: Seq[Node] = Seq(left, right)

  override protected[this] def nodeRebuild(ch: IndexedSeq[Node]): Self = copy(ch(0), ch(1))
}

object SampleUnion {
  def sampleUnion[F, T, D[_]](q1: Query[F, T, D], q2: Query[F, T, D]) = {
    new WrappingQuery[F, T, D](SampleUnion(q1.toNode, q2.toNode), q1.shaped)
  }
}
