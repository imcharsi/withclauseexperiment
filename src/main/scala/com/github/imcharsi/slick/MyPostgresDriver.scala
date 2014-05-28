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
package com.github.imcharsi.slick

import scala.slick.driver.PostgresDriver
import scala.slick.compiler.CompilerState
import scala.slick.ast._
import scala.slick.util.MacroSupport.macroSupportInterpolation
import scala.slick.SlickException
import scala.slick.ast.TableNode
import scala.Some
import scala.slick.ast.Comprehension
import com.github.imcharsi.slick.tminglei_pgarray.PgArraySupport
import com.github.imcharsi.slick.withclauseexperiment.{ WithNode, SampleUnion, FunctionTableNode }

trait MyPostgresDriver extends PostgresDriver with PgArraySupport {

  lazy override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  trait ImplicitsPlus extends Implicits with ArrayImplicits

  trait SimpleQLPlus extends SimpleQL with ImplicitsPlus

  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilderEx(n, state)

  class QueryBuilderEx(tree: Node, state: CompilerState) extends QueryBuilder(tree, state) {
    override protected def toComprehension(n: Node, liftExpression: Boolean): Comprehension = n match {
      case t: FunctionTableNode ⇒ Comprehension(from = Seq(newSym -> t))
      case s: SampleUnion ⇒ Comprehension(from = Seq(newSym -> s))
      case wt: WithNode ⇒ Comprehension(from = Seq(newSym -> wt))
      case _ ⇒ super.toComprehension(n, liftExpression)
    }

    override protected def buildComprehension(c: Comprehension): Unit = {
      // This is needed for with-recursive.
      // In with-recursive, parentheses should not be used.
      // for example,
      // with recursive a (
      // select x.x1, x.x2 from (select ... from ... union select ... from ...) x
      // )
      // is not worked.
      // It should be like this:
      // with recursive a (
      // select ... from ... union select ... from ...
      // )
      if (!c.from.isEmpty && c.from.head._2.isInstanceOf[SampleUnion])
        buildUnwrappedUnion4WithRecursive(c.from)
      else
        super.buildComprehension(c)
    }

    override protected def buildFrom(n: Node, alias: Option[Symbol], skipParens: Boolean): Unit = building(FromPart) {
      def addAlias = alias foreach { s ⇒ b += ' ' += symbolName(s) }
      n match {
        case su: SampleUnion ⇒ {
          buildComprehension(su.left.asInstanceOf[Comprehension])
          b" union "
          buildComprehension(su.right.asInstanceOf[Comprehension])
        }
        case t: FunctionTableNode ⇒ {
          b += quoteFunctionTableName(t)
          b"${t.openParens}"
          b.sep(t.argNode, ", ")(expr(_, true))
          b"${t.closeParens}"
          addAlias
        }
        case w: WithNode ⇒ {
          b"(with " // pair 1
          if (w.recursive)
            b"recursive "
          var tableNodes = w.tableNodes
          var queryNodes = w.queryNodes
          if (tableNodes.length != queryNodes.length)
            throw new SlickException("")
          while (!tableNodes.isEmpty && !queryNodes.isEmpty) {
            val first = tableNodes.head.asInstanceOf[TableExpansion]
            tableNodes = tableNodes.tail
            val second = toComprehension(queryNodes.head, true)
            queryNodes = queryNodes.tail
            b"${quoteTableName(first.table.asInstanceOf[TableNode])}"
            b" (" // pair 2-1
            buildWithClauseColumns(first)
            b")" // pair 2-1
            b" as (" // pair 2-2
            buildComprehension(second)
            b")" // pair 2-2
            if (!tableNodes.isEmpty && !queryNodes.isEmpty)
              b", "
          }
          b" "
          buildComprehension(w.mainQueryNode.asInstanceOf[Comprehension])
          b")" // pair 1
          addAlias
        }
        case _ ⇒ super.buildFrom(n, alias, skipParens)
      }
    }

    // 이것은 SqlUtilsComponent 에 들어가면 된다.
    // 원본 method 의 위치를 참고하기.
    protected def quoteFunctionTableName(t: FunctionTableNode): String = t.schemaName match {
      case Some(s) ⇒ {
        if (t.quote)
          quoteIdentifier(s) + "." + quoteIdentifier(t.tableName)
        else
          s + "." + t.tableName
      }
      case None ⇒ {
        if (t.quote)
          quoteIdentifier(t.tableName)
        else
          t.tableName
      }
    }

    protected def buildUnwrappedUnion4WithRecursive(from: Seq[(Symbol, Node)]) = building(FromPart) {
      if (from.isEmpty)
        throw new SlickException("")
      b" "
      b.sep(from, ", ") { case (sym, n) ⇒ buildFrom(n, Some(sym)) }
    }

    protected def buildWithClauseColumns(c: TableExpansion) = building(SelectPart) {
      // In original implementation, There is buildSelectClause(...).
      // but this write table's name, too.
      // so this method is needed to just write column's name.
      // And, if c is Comprehension's instance, we can't know Table information.
      c.columns.nodeChildren.head match {
        case ProductNode(ch) ⇒ {
          if (ch.isEmpty)
            throw new SlickException("")
          b.sep(ch, ", ") {
            case Path(field :: _) ⇒ b += symbolName(field)
          }
        }
        case Path(field :: _) ⇒ b += symbolName(field)
        case _ ⇒ {
          // It is not intended case. What happened?
          throw new SlickException("only for with clause")
        }
      }
    }
  }

}

object MyPostgresDriver extends MyPostgresDriver