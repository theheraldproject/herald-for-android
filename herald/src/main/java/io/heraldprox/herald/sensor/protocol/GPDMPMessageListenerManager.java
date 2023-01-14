//  Copyright 2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//
package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

public interface GPDMPMessageListenerManager {
    /**
     * Adds a listener that should be notified when a matching Layer7 GPDMP channel message arrives
     * @param channelId The Channel ID that the listener is interested in
     * @param listener The listener to call when a message is received
     */
    void addMessageListener(UUID channelId, GPDMPMessageListener listener);

    /**
     * Adds a listener that is currently being notified when a matching Layer7 GPDMP channel message arrives
     * @param channelId The Channel ID that the listener was interested in
     * @param listener The listener to remove from the listener list
     */
    void removeMessageListener(UUID channelId,GPDMPMessageListener listener);
}
