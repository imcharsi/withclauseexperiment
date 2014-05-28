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

import com.github.imcharsi.slick.{MapperTrait, DriverSetting}
import DriverSetting.driver._
import com.github.imcharsi.slick.withclauseexperiment.NodeChangeableTableQuery

/**
 * Created by KangWoo,Lee on 14. 5. 28.
 */
case class UnnestType(var x: Option[Int])

class UnnestFunctionTable(tag: Tag) extends Table[UnnestType](tag, "generate_series") with MapperTrait {
  def unnest = column[Option[Int]]("unnest", O.NotNull)

  override def * = (unnest) <>(UnnestType.apply, UnnestType.unapply)
}

object UnnestFunctionTable extends NodeChangeableTableQuery[UnnestFunctionTable](new UnnestFunctionTable(_))

