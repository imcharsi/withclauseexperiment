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

import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FlatSpec}
import DriverSetting.driver._
import java.util.logging.Logger
import scala.util.Try
import scala.slick.lifted.SimpleFunction
import com.github.imcharsi.slick.withclauseexperiment.{SampleUnion, With, FunctionTable}
import com.github.imcharsi.slick.model._
import com.github.imcharsi.slick.model.ModelA

/**
 * Created by KangWoo,Lee on 14. 5. 24.
 */
class TestOne extends FlatSpec with BeforeAndAfterAll with MapperTrait with BeforeAndAfterEach {
  val logger = Logger.getLogger("hi")
  val row_number = SimpleFunction.nullary[Long]("row_number")
  val string_to_array = SimpleFunction.binary[Option[String], String, Option[List[String]]]("string_to_array")
  val unnest = SimpleFunction.unary[Option[List[Int]], Option[Int]]("unnest")
  // this is user-defined sql function.
  val conv_trans = SimpleFunction.unary[Int, Option[List[Int]]]("conv_array")

  // http://sqlwithmanoj.wordpress.com/2011/05/23/cte-recursion-sequence-dates-factorial-fibonacci-series/
  "fibonacci series".should("be calculated").in {
    def fibonacciQuery(n: Column[Option[Int]]) = {
      val first = FunctionTable.
        binary(FibonacciTable, LiteralColumn(Option(1)), LiteralColumn(Option(1))).
        map(t => (LiteralColumn(Option(0)), LiteralColumn(Option(0)), LiteralColumn(Option(1)), LiteralColumn(Option(1))))
      val withDeclarationFib = FibonacciTable.asWithDeclaration("fib")
      val second = withDeclarationFib.
        filter(t => t.num < n).
        map(t => (t.seed + t.fibA, t.fibA + t.fibB, t.fibA, t.num + 1))
      With.recursive(withDeclarationFib, SampleUnion.sampleUnion(first, second)).mainQuery(withDeclarationFib.map(t => t.fibA))
    }
    val compiled = Compiled(fibonacciQuery _)
    val result = compiled.apply(Option(12)).run
    result.foreach(x => logger.info(x.toString))
  }

  // http://sqlwithmanoj.wordpress.com/2011/05/23/cte-recursion-sequence-dates-factorial-fibonacci-series/
  "factorial series".should("be calculated").in {
    def queryOne(o: Column[Option[Int]]) = {
      val first = FunctionTable.
        binary(FactorialTable, LiteralColumn(Option(1)), LiteralColumn(Option(1))).
        map(t => (Option(1), Option(1)))
      val withDeclarationFacto = FactorialTable.asWithDeclaration("facto")
      val second = withDeclarationFacto.
        filter(t => t.num < o).
        map(t => (t.fact * (t.num + 1), t.num + 1))
      With.recursive(withDeclarationFacto, SampleUnion.sampleUnion(first, second)).mainQuery(withDeclarationFacto)
    }
    val compiled = Compiled(queryOne _)
    val result = compiled.apply(Option(6)).run
    result.foreach(x => logger.info(x.toString))
  }

  "hierachical data".should("be ready").in {
    for (i <- 1.to(4)) {
      val a = ModelA(None, None, Option(f"root$i"))
      a.id = TableA.returning(TableA.map(_.id)).insert(a)
      for (i2 <- 1.to(3)) {
        val b = ModelA(None, a.id, Option(f"root$i-sub$i2"))
        b.id = TableA.returning(TableA.map(_.id)).insert(b)
        for (i3 <- 1.to(2)) {
          val c = ModelA(None, a.id, Option(f"root$i-sub$i2-sub$i3"))
          c.id = TableA.returning(TableA.map(_.id)).insert(c)
        }
      }
    }
  }

  it.should("be retrieved").in {
    // we need synthetic table name in WITH clause's declaration.
    val withDeclarationOne = UnnestFunctionTable.asWithDeclaration("with_one")
    val withDeclarationTwo = TableA.asWithDeclaration("with_two")
    val withDeclarationThree = HierachicalTable.asWithDeclaration("with_three")

    def conv_trans_macro(t: TableA): Column[Option[List[Int]]] = {
      conv_trans((row_number :: Over.partitionBy(t.parentId).orderBy(t.name.desc)).asColumnOf[Int])
    }
    def queryForHierachical(o: Column[Option[String]]) = {
      def mapOne(t: TableA) = {
        (t.id, t.parentId, t.name,
          conv_trans_macro(t), // like order siblings by
          LiteralColumn(Option(0))) // level
      }
      def mapTwo(x: (HierachicalTable, TableA)) = {
        val (a, b) = x
        (b.id, b.parentId, b.name,
          a.odr + (row_number :: Over.partitionBy(b.parentId).orderBy(b.name.desc)).asColumnOf[Int], // like order siblings by
          (a.depth + 1).asColumnOf[Option[Int]]) // level
      }
      def filterOne(x: (Column[Option[Int]], Column[Option[Int]], Column[Option[String]], Column[Option[List[Int]]], Column[Option[Int]])) = {
        val (_, b, c, _, _) = x
        b.in(withDeclarationOne.map(_.unnest)) && c.like(o)
      }
      // real table.
      val step1 = TableA.
        filter(t => t.parentId.isEmpty && t.name.like(o)).
        map(mapOne)
      // synthetic table.
      val step2 = withDeclarationThree.
        join(TableA).
        on(_.id === _.parentId).
        map(mapTwo).
        filter(filterOne)

      SampleUnion.sampleUnion(step1, step2)
    }
    def queryForINparameter(o: Column[Option[String]]) = {
      FunctionTable.
        binary(UnnestFunctionTable, LiteralColumn(Option(1)), LiteralColumn(Option(1))). // we need something like oracle's dual.
        map(t => unnest.apply(string_to_array.apply(o, LiteralColumn(",")).asColumnOf[Option[List[Int]]])) // this is postgres-specified feature.
    }
    def queryForMaster(p1: Column[Option[String]], p2: Column[Option[String]]) = {
      val nestedWithThatNothingToDo = With.
        recursive(withDeclarationTwo, TableA).
        mainQuery(withDeclarationTwo)
      With.
        recursive(withDeclarationOne, queryForINparameter(p2)).
        append(withDeclarationTwo, nestedWithThatNothingToDo).
        append(withDeclarationThree, queryForHierachical(p1)).
        mainQuery(withDeclarationThree.sortBy(t => t.odr))
    }

    val compiled = Compiled(queryForMaster _)
    val result = compiled.apply(Option("%"), Option("1,11,31")).run
    result.foreach(x => logger.info(x.toString))
  }

  implicit var session: Session = _

  override protected def beforeEach(): Unit = {
    session = DriverSetting.database.createSession()
  }

  override protected def afterEach(): Unit = {
    session.close()
    session = null
  }

  override protected def beforeAll(): Unit = {
    implicit val session = DriverSetting.database.createSession()
    Try(TableA.ddl.create)
    session.close()
  }

  override protected def afterAll(): Unit = {
    implicit val session = DriverSetting.database.createSession()
    Try(TableA.ddl.drop)
    session.close()
  }
}

