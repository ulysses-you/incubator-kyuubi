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

package org.apache.kyuubi.kubernetes.test.spark

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.net.NetUtils

import org.apache.kyuubi.{Logging, Utils, WithKyuubiServer, WithSimpleDFSService}
import org.apache.kyuubi.config.KyuubiConf
import org.apache.kyuubi.kubernetes.test.MiniKube
import org.apache.kyuubi.operation.SparkQueryTests

abstract class SparkOnKubernetesSuiteBase
  extends WithKyuubiServer with SparkQueryTests with Logging {
  private val apiServerAddress = {
    MiniKube.getKubernetesClient.getMasterUrl.toString
  }

  protected def sparkOnK8sConf: KyuubiConf = {
    // TODO Support more Spark version
    // Spark official docker image: https://hub.docker.com/r/apache/spark/tags
    KyuubiConf().set("spark.master", s"k8s://$apiServerAddress")
      .set("spark.kubernetes.container.image", "apache/spark:v3.2.1")
      .set("spark.kubernetes.container.image.pullPolicy", "IfNotPresent")
      .set("spark.executor.instances", "1")
      .set("spark.executor.cores", "1")
      .set("spark.executor.memory", "512M")
      .set("spark.driver.memory", "512M")
  }

  override protected def jdbcUrl: String = getJdbcUrl
}

/**
 * This test is for Kyuubi Server with Spark engine on Kubernetes:
 *
 *                        Real World                                   Kubernetes Pod
 *  -------------------------------------------------------         ---------------------
 *  |          JDBC                                       |         |                   |
 *  |  Client  ---->  Kyuubi Server  ---->  Spark Driver  |  ---->  |  Spark Executors  |
 *  |                                                     |         |                   |
 *  -------------------------------------------------------         ---------------------
 */
class SparkClientModeOnKubernetesSuite extends SparkOnKubernetesSuiteBase {
  override protected val conf: KyuubiConf = {
    sparkOnK8sConf.set("spark.submit.deployMode", "client")
  }
}

/**
 * This test is for Kyuubi Server with Spark engine Using cluster deploy-mode on Kubernetes:
 *
 *               Real World                         Kubernetes Pod                Kubernetes Pod
 *  ----------------------------------          ---------------------         ---------------------
 *  |          JDBC                   |         |                   |         |                   |
 *  |  Client  ---->  Kyuubi Server   |  ---->  |    Spark Driver   |  ---->  |  Spark Executors  |
 *  |                                 |         |                   |         |                   |
 *  ----------------------------------          ---------------------         ---------------------
 */
class SparkClusterModeOnKubernetesSuite
  extends SparkOnKubernetesSuiteBase with WithSimpleDFSService {

  private val localHost = Utils.findLocalInetAddress.getHostName

  override val hadoopConf: Configuration = {
    val hdfsConf: Configuration = new Configuration()
    hdfsConf.set("dfs.namenode.rpc-bind-host", "0.0.0.0")
    hdfsConf.set("dfs.namenode.servicerpc-bind-host", "0.0.0.0")
    hdfsConf.set("dfs.datanode.hostname", localHost)
    hdfsConf.set("dfs.datanode.address", s"0.0.0.0:${NetUtils.getFreeSocketPort}")
    hdfsConf
  }

  override protected lazy val conf: KyuubiConf = {
    sparkOnK8sConf.set("spark.submit.deployMode", "cluster")
      .set("spark.kubernetes.file.upload.path", s"hdfs://$localHost:$getDFSPort/spark")
      .set("spark.hadoop.dfs.client.use.datanode.hostname", "true")
  }
}
