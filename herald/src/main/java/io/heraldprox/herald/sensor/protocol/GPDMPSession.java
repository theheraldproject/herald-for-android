//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.UInt16;

import java.util.UUID;

/**
 * Represents the information required to establish a Session between two participants on a
 * shared communication channel.
 *
 * Note: Each recipient UUID is unique for a recipient on THIS session only. A single participant
 * will have many recipient UUIDs, one per channel.
 */
public class GPDMPSession {
    private UUID channelId;
    private UUID mySenderRecipientId;
    private Date channelEpoch;
    private UUID remoteRecipientId = null;

    /**
     * Create a GPDMP Session initiated from this device, for another device. (I.e. missing remote info)
     * @param channelId
     */
    public GPDMPSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch) {
        this.channelId = channelId;
        this.mySenderRecipientId = mySenderRecipientId;
        this.channelEpoch = channelEpoch;
    }

    /**
     * Create a GPDMP Session initiated by a remote device, with full recipient information
     * @param channelId
     * @param mySenderRecipientId
     * @param channelEpoch
     * @param remoteRecipientId
     */
    public GPDMPSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch,
                        UUID remoteRecipientId) {
        this.channelId = channelId;
        this.mySenderRecipientId = mySenderRecipientId;
        this.channelEpoch = channelEpoch;
        this.remoteRecipientId = remoteRecipientId;
    }

    /**
     * Returns whether this session has enough information for data to be sent and received
     * @return
     */
    public boolean isPartialSession() {
        return null == remoteRecipientId;
    }

    /**
     * Returns whether the given hash could have been derived from this session's remote
     * recipient information
     * @param senderPartialHash
     * @return
     */
    public boolean senderMatchesPartialHash(Date timeToAccess,UInt16 senderPartialHash) {
        // NOTE: THIS IS UNENCRYPTED FOR NOW
        if (null == remoteRecipientId) {
            return false;
        }
        return (new UInt16((int)(remoteRecipientId.getMostSignificantBits() & 0xffff))).equals(senderPartialHash);
    }

    /**
     * Return the TOTP partial hash for this device's sender recipient ID for the agree time and
     * epoch on this channel.
     *
     * @param timeToAccess
     * @return
     */
    public UInt16 getMySenderPartialHash(Date timeToAccess) {
        // NOTE: THIS IS UNENCRYPTED FOR NOW
        return new UInt16((int)(mySenderRecipientId.getMostSignificantBits() & 0xFFFF));
    }

    public UUID getChannelId() {
        return channelId;
    }

    public UUID getEncryptedChannelID() {
        // NOTE UNENCRYPTED FOR NOW
        return channelId;
    }

    public boolean matchesEncryptedChannelID(Date timeToAccess,UUID encryptedChannelId) {
        // NOTE THIS IS UNENCRYPTED FOR NOW
        return encryptedChannelId.equals(channelId);
    }

    public Data decrypt(Date timeToAccess, Data encryptedData) {
        // NOTE UNENCRYPTED FOR NOW
        return encryptedData;
    }

    public Data encrypt(Date timeToAccess, Data unencryptedData) {
        // NOTE UNENCRYPTED FOR NOW
        return unencryptedData;
    }

    public UUID getMySenderRecipientId() {
        return mySenderRecipientId;
    }

    public Date getChannelEpoch() {
        return channelEpoch;
    }

    public UUID getRemoteRecipientId() {
        return remoteRecipientId;
    }

    /**
     * Setting the remote recipient Id completes the Session description.
     * @note The recipient ID is passed via an out of band carrier.
     *
     * @param remoteRecipientId Recipient ID received from the remote
     */
    public void setRemoteRecipientId(UUID remoteRecipientId) {
        this.remoteRecipientId = remoteRecipientId;
    }
}
