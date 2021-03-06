/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.amaterasu.common.execution

import java.util
import java.util.concurrent.LinkedBlockingQueue

import org.apache.amaterasu.common.configuration.enums.ActionStatus
import org.apache.amaterasu.common.dataobjects.ActionData
import org.apache.amaterasu.leader.common.execution.actions.SequentialAction
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer
import org.apache.zookeeper.CreateMode
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ActionStatusTests extends FlatSpec with Matchers {

  // setting up a testing zookeeper server (curator TestServer)
  val retryPolicy = new ExponentialBackoffRetry(1000, 3)
  val server = new TestingServer(2181, true)
  val jobId = s"job_${System.currentTimeMillis}"
  val data = new ActionData(ActionStatus.Pending, "test_action", "start.scala", "", "spark","scala", "0000001", new util.HashMap() , List[String]().asJava)

  "an Action" should "queue it's ActionData int the job queue when executed" in {

    val queue = new LinkedBlockingQueue[ActionData]()
    // val config = ClusterConfig()

    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()

    client.create().withMode(CreateMode.PERSISTENT).forPath(s"/$jobId")
    val action = new SequentialAction(data.getName, data.getSrc, "", data.getGroupId, data.getTypeId, Map.empty[String, String].asJava, jobId, queue, client, 1)

    action.execute()
    queue.peek().getName should be(data.getName)
    queue.peek().getSrc should be(data.getSrc)

  }

  it should "also create a sequential znode for the task with the value of Queued" in {

    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()

    val taskStatus = client.getData.forPath(s"/$jobId/task-0000000000")

    taskStatus should not be null
    new String(taskStatus) should be("Queued")

  }

}