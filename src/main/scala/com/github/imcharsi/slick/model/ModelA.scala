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
package com.github.imcharsi.slick.model

import scala.beans.BeanProperty
import com.github.imcharsi.slick.{MapperTrait, DriverSetting}
import DriverSetting.driver._
import com.github.imcharsi.slick.withclauseexperiment.NodeChangeableTableQuery

/**
 * Created by KangWoo,Lee on 14. 5. 18.
 */
case class ModelA(@BeanProperty var id: Option[Int],
                  @BeanProperty var parentId: Option[Int],
                  @BeanProperty var name: Option[String])

class TableA(tag: Tag) extends Table[ModelA](tag, Option("sample"), "model_a") with MapperTrait {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)

  def parentId = column[Option[Int]]("parent_id", O.Nullable)

  def name = column[Option[String]]("name", O.Nullable)

  override def * = (id, parentId, name) <>(ModelA.tupled, ModelA.unapply)

  def foreignKey1 = foreignKey(f"${tableName}_fk1", parentId, TableA)(_.id)
}

object TableA extends NodeChangeableTableQuery[TableA](new TableA(_))

