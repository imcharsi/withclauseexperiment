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

import scala.slick.lifted._
import scala.slick.ast.Node
import scala.slick.ast.TableExpansion
import scala.slick.ast.TableNode

/**
 * Created by KangWoo,Lee on 14. 5. 27.
 */
class NodeChangeableTableQuery[E <: AbstractTable[_]](cons: Tag ⇒ E, n: Option[TableExpansion] = None) extends TableQuery[E](cons) {
  lazy override val toNode: Node = n.getOrElse(shaped.toNode)

  def asWithDeclaration(n: String): NodeChangeableTableQuery[E] = {
    val tableExpansion: TableExpansion = toNode.asInstanceOf[TableExpansion]
    val tableNode = tableExpansion.table match {
      case x: TableNode ⇒ x.copy(schemaName = None, tableName = n)
      case x: FunctionTableNode ⇒ TableNode(None, n, x.identity, x.driverTable, x.baseIdentity)
    }
    new NodeChangeableTableQuery[E](cons, Option(tableExpansion.copy(table = tableNode)))
  }

  protected[withclauseexperiment] def changeTableExpansion(n: TableExpansion) = {
    new NodeChangeableTableQuery[E](cons, Option(n))
  }
}
