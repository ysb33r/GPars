//  GParallelizer
//
//  Copyright � 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.gparallelizer.remote.netty;

import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.messages.BaseMsg;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection using Netty
 *
 * @author Alex Tkachman
 */
public class NettyRemoteConnection extends RemoteConnection {
    private NettyHandler handler;
    private final MyChannelFutureListener writeListener = new MyChannelFutureListener();

    public NettyRemoteConnection(NettyTransportProvider provider, NettyHandler netHandler) {
        super(provider);
        this.handler = netHandler;
    }

    public void write(BaseMsg msg) {
        if(handler.getChannel().isConnected() && handler.getChannel().isOpen()) {
            writeListener.incrementAndGet();
            handler.getChannel().write(msg).addListener(writeListener);
        }
    }

    public void disconnect() {
        writeListener.incrementAndGet();
        writeListener.handler = handler;
        try {
            writeListener.operationComplete(null);
        } catch (Exception e) {
        }
    }

    private static class MyChannelFutureListener extends AtomicInteger implements ChannelFutureListener {
        public volatile NettyHandler handler;

        public void operationComplete(ChannelFuture future) throws Exception {
            if (decrementAndGet() == 0 && handler != null) {
                final CountDownLatch cdl = new CountDownLatch(1);
                handler.getChannel().close().addListener(new ChannelFutureListener(){
                    public void operationComplete(ChannelFuture future) throws Exception {
                        cdl.countDown();
                    }
                });
                try {
                    cdl.await ();
                } catch (InterruptedException e) {//
                    e.printStackTrace();
                }
            }
        }
    }
}