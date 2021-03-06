// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.remote.dataflow.queuebalancer

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9111
def NUMBER_OF_TASKS = 100

println "Example: DataflowQueue load balancer"

def remoteDataflows = RemoteDataflows.create()
remoteDataflows.startServer HOST, PORT

def queue = new DataflowQueue()

remoteDataflows.publish queue, "queue-balancer"

println "Press any key to start..."
System.in.read()

(1..NUMBER_OF_TASKS).each { i ->
    queue << "task ${i}"
    sleep 50
}

