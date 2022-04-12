/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kyuubi

import org.apache.hadoop.conf.Configuration

import org.apache.kyuubi.config.KyuubiConf
import org.apache.kyuubi.server.MiniDFSService

trait WithSimpleDFSService extends KyuubiFunSuite {

  private var miniDFSService: MiniDFSService = _

  def hadoopConf: Configuration = new Configuration()

  override def beforeAll(): Unit = {
    miniDFSService = new MiniDFSService(hadoopConf)
    miniDFSService.initialize(new KyuubiConf(false))
    miniDFSService.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    miniDFSService.stop()
    super.afterAll()
  }

  def getHadoopConf: Configuration = miniDFSService.getHadoopConf

  def getDefaultFS: String = {
    miniDFSService.getHadoopConf.get("fs.defaultFS")
  }

  def getDFSPort: Int = miniDFSService.getDFSPort
}
