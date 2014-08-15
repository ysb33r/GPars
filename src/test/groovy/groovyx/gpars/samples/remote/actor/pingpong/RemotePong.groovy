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

package groovyx.gpars.samples.remote.actor.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def HOST = "localhost"
def PORT = 9000

def pongActor = Actors.actor {
    println "Pong Actor"

    // handle incoming messages
    loop {
        react {
            println it
            reply "PONG"
        }
    }
}

def remoteActors = RemoteActors.create()
remoteActors.startServer HOST, PORT

// publish pongActor as a remote actor
remoteActors.publish pongActor, "pong"

pongActor.join()