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

import com.jolbox.bonecp.BoneCPDataSource
import net.sf.log4jdbc.DriverSpy

/**
 * Created by KangWoo,Lee on 14. 5. 18.
 */
object DriverSetting {
  val dataSource = new BoneCPDataSource()
  dataSource.setDriverClass(classOf[DriverSpy].getName)
  dataSource.setJdbcUrl("jdbc:log4jdbc:postgresql://localhost/sample")
  dataSource.setUsername("sample_admin")
  dataSource.setPassword("0")
  //  val driver = scala.slick.driver.PostgresDriver.simple
  val driver = MyPostgresDriver.simple

  import driver._

  val database = Database.forDataSource(dataSource)
}

trait MapperTrait {

  import DriverSetting.driver._

  implicit val integerMapper = MappedColumnType.base[Integer, Int](_.intValue(), Integer.valueOf(_))
}

