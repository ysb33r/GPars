//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
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

package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.remote.RemoteActor;
import org.gparallelizer.remote.RemoteNode;

import java.util.UUID;

public abstract class MemoryNode extends RemoteNode<MemoryTransportProvider> {
    protected final LocalNode localNode;

    public MemoryNode(LocalNode node, MemoryTransportProvider provider) {
        super(node.getId(), provider);
        localNode = node;
    }

    public void onConnect(LocalNode node) {
        localNode.onConnect(getProvider().getLocalRemote(node, true));
    }

    public void onDisconnect(LocalNode node) {
        localNode.onDisconnect(getProvider().getLocalRemote(node,false));
    }

    protected RemoteActor createRemoteActor(UUID uid) {
        if (uid == localNode.getId())
            return new RemoteActor(this, uid);

        throw new IllegalStateException();
    }
}
