package io.shinto.amaterasu.execution

import java.util.concurrent.LinkedBlockingQueue

import io.shinto.amaterasu.Config
import io.shinto.amaterasu.dataObjects.ActionData
import io.shinto.amaterasu.enums.ActionStatus
import io.shinto.amaterasu.execution.actions.{ SequentialAction }
import org.apache.curator.framework.{ CuratorFramework, CuratorFrameworkFactory }
import org.apache.curator.test.TestingServer
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.CreateMode
import org.scalatest.{ Matchers, FlatSpec }

class ActionTests extends FlatSpec with Matchers {

  // setting up a testing zookeeper server (curator TestServer)
  val retryPolicy = new ExponentialBackoffRetry(1000, 3)
  val server = new TestingServer(2181, true)
  val jobId = s"job_${System.currentTimeMillis}"
  val data = ActionData("test_action", "http://github.com", "master", "", "1", jobId)

  "an Action" should "queue it's ActionData int the job queue when executed" in {

    val queue = new LinkedBlockingQueue[ActionData]()
    val config = Config()

    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()

    client.create().withMode(CreateMode.PERSISTENT).forPath(s"/${data.jobId}")
    val action = SequentialAction(data, config, queue, client)

    action.execute()
    queue.peek() should be(data)

  }

  it should "also create a sequential note for the task with the value of queued" in {

    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()

    val actionExists = client.getData.forPath(s"/${data.jobId}/task-0000000000")

    assert(actionExists != null)
    new String(actionExists) should be("queued")
  }
}