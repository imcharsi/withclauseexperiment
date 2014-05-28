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

import scala.slick.lifted.{ WrappingQuery, Query, TableQuery, AbstractTable }
import scala.slick.ast._
import scala.slick.SlickException
import scala.language.higherKinds

/**
 * Created by KangWoo,Lee on 14. 5. 27.
 */
case class WithAppend(val withNode: WithNode) {
  // FIXME If, even there are some differences between TableQuery and Query, compiler does not complain anything in this impl.
  // Example case is: TableQuery = [Option[Int], Option[String]], Query = [Option[Date], Option[Int]]
  def append[A <: AbstractTable[_], B, C, D[_]](t: TableQuery[A], q: Query[B, C, D]): WithAppend = {
    WithAppend(WithNode(withNode.recursive, withNode.tableNodes, withNode.queryNodes, t.toNode, q.toNode, null))
  }

  def mainQuery[F, T, D[_]](q: Query[F, T, D]): Query[F, T, D] = {
    new WrappingQuery[F, T, D](withNode.copy(mainQueryNode = q.toNode), q.shaped)
  }
}

trait With {
  def noRecursive[A <: AbstractTable[_], B, C, D[_]](t: TableQuery[A], q: Query[B, C, D]): WithAppend = {
    WithAppend(WithNode(false, Seq(), Seq(), t.toNode, q.toNode, null))
  }

  def recursive[A <: AbstractTable[_], B, C, D[_]](t: TableQuery[A], q: Query[B, C, D]): WithAppend = {
    WithAppend(WithNode(true, Seq(), Seq(), t.toNode, q.toNode, null))
  }
}

object With extends With

case class WithNode(val recursive: Boolean, val prevTableNodes: Seq[Node], val prevQueryNodes: Seq[Node],
    val tableNode: Node, val queryNode: Node, val mainQueryNode: Node) extends SimplyTypedNode {
  override type Self = WithNode

  override protected def buildType: Type = mainQueryNode.nodeType

  val tableNodes: Seq[Node] = prevTableNodes :+ tableNode
  val queryNodes: Seq[Node] = prevQueryNodes :+ queryNode
  override val nodeChildren: Seq[Node] = queryNodes :+ mainQueryNode

  override protected[this] def nodeRebuild(ch: IndexedSeq[Node]): Self = {
    // The case which is ch.length < 2 is impossible. It is not intended.
    // With first the With.recursive method call,
    // at least, nodeChildren should be one QueryNode plus a main query node.
    if (ch.length < 2)
      throw new SlickException("")
    // with Comprehension class node,
    // we can't know table information which is needed for the sql with clause's declaration.
    // so, TableExpansions are not rebuilt and just query-related nodes are rebuilt.
    val newMainQueryNode = ch(ch.length - 1)
    val newQueryNode = ch(ch.length - 2)
    val newPrevQueryNodes = ch.drop(0).take(ch.length - 2)
    copy(prevQueryNodes = newPrevQueryNodes, queryNode = newQueryNode, mainQueryNode = newMainQueryNode)
  }
}

