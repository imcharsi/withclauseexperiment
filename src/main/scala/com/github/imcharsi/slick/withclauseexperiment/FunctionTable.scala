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

import scala.slick.ast._
import scala.slick.ast.TableNode
import scala.slick.lifted._
import scala.slick.ast.CollectionType
import scala.slick.ast.NominalType
import scala.slick.ast.UnassignedStructuralType

/**
 * Created by KangWoo,Lee on 14. 5. 26.
 */
case class FunctionTableNode(schemaName: Option[String], tableName: String, argNode: Seq[Node],
    identity: TableIdentitySymbol, driverTable: Any, baseIdentity: TableIdentitySymbol,
    quote: Boolean, openParens: String, closeParens: String) extends NullaryNode with TypedNode {
  type Self = FunctionTableNode

  def tpe = CollectionType(TypedCollectionTypeConstructor.seq, NominalType(identity)(UnassignedStructuralType(identity)))

  def nodeRebuild = copy()

  override def toString = "FunctionTable " + tableName
}

trait FunctionTable {
  private def apply[E <: AbstractTable[_]](q: NodeChangeableTableQuery[E], quote: Boolean = true, openParens: String = "(", closeParens: String = ")")(c: Seq[Node]): NodeChangeableTableQuery[E] = {
    val tableExpansion: TableExpansion = q.toNode.asInstanceOf[TableExpansion]
    val tableNode: TableNode = tableExpansion.table.asInstanceOf[TableNode]
    val functionTableNode: FunctionTableNode = FunctionTableNode(
      tableNode.schemaName, tableNode.tableName, c, tableNode.identity, tableNode.driverTable, tableNode.baseIdentity,
      quote, openParens, closeParens)
    q.changeTableExpansion(tableExpansion.copy(table = functionTableNode))
  }

  def nullary[E <: AbstractTable[_]](q: NodeChangeableTableQuery[E], quote: Boolean = true, openParens: String = "(", closeParens: String = ")")(): NodeChangeableTableQuery[E] = {
    this(q, quote, openParens, closeParens)(Nil)
  }

  def unary[E <: AbstractTable[_], T1](q: NodeChangeableTableQuery[E], c1: Column[T1],
    quote: Boolean = true, openParens: String = "(", closeParens: String = ")"): NodeChangeableTableQuery[E] = {
    this(q, quote, openParens, closeParens)(Seq(c1.toNode))
  }

  def binary[E <: AbstractTable[_], T1, T2](q: NodeChangeableTableQuery[E], c1: Column[T1], c2: Column[T2],
    quote: Boolean = true, openParens: String = "(", closeParens: String = ")"): NodeChangeableTableQuery[E] = {
    this(q, quote, openParens, closeParens)(Seq(c1.toNode, c2.toNode))
  }

  def tenary[E <: AbstractTable[_], T1, T2, T3](q: NodeChangeableTableQuery[E], c1: Column[T1], c2: Column[T2], c3: Column[T3],
    quote: Boolean = true, openParens: String = "(", closeParens: String = ")"): NodeChangeableTableQuery[E] = {
    this(q, quote, openParens, closeParens)(Seq(c1.toNode, c2.toNode, c3.toNode))
  }
}

// If you need 4 parameters, 5 parameters, you can make them easily.
object FunctionTable extends FunctionTable
