// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

public class MultiMessageTest extends GroovyTestCase {
    public void testReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oldActor {
            receive {a, b, c ->
                result = a + b + c
                latch.countDown()
            }
        }

        actor.send 2
        actor.send 3
        actor.send 4

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 9, result
    }

    public void testNoMessageReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oldActor {
            receive {->
                result = 1
                latch.countDown()
            }
        }

        actor.send 2

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 1, result
    }

    public void testDefaultMessageReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oldActor {
            receive {
                result = 1
                latch.countDown()
            }
        }

        actor.send 2

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 1, result
    }

    public void testArrayReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oldActor {
            receive {a, b, c ->
                result = a[2] + b + c
                latch.countDown()
            }
        }

        actor.send([2, 10, 20])
        actor.send 3
        actor.send 4

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 27, result
    }

    public void testMessageReply() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(3)
        volatile AtomicInteger result = new AtomicInteger(0)
        final def group = new DefaultPGroup(10)

        def actor = group.oldActor {
            receive {a, b, c ->
                a.reply(a + 1)
                b.reply(b + 1)
                c.reply(c + 1)
            }
        }

        createReplyActor actor, 10, barrier, latch, result, group
        createReplyActor actor, 100, barrier, latch, result, group
        createReplyActor actor, 1000, barrier, latch, result, group

        latch.await(90, TimeUnit.SECONDS)

        assertEquals 1113, result.get()
        group.shutdown()
    }

    public void testActorReply() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(3)
        volatile AtomicInteger result = new AtomicInteger(0)
        final def group = new DefaultPGroup(10)

        def actor = group.oldActor {
            receive {a, b, c ->
                reply(20)
            }
        }

        createReplyActor actor, 10, barrier, latch, result, group
        createReplyActor actor, 100, barrier, latch, result, group
        createReplyActor actor, 1000, barrier, latch, result, group

        latch.await(90, TimeUnit.SECONDS)

        assertEquals 60, result.get()
        group.shutdown()
    }

    Actor createReplyActor(Actor actor, int num, CyclicBarrier barrier, CountDownLatch latch, AtomicInteger result, group) {
        group.oldActor {
            barrier.await()
            actor.send(num)
            receive {
                result.addAndGet(it)
                latch.countDown()
            }
        }
    }
}
