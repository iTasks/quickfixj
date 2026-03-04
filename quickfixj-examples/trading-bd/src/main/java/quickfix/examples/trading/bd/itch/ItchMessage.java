/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix.examples.trading.bd.itch;

import java.nio.ByteBuffer;

/**
 * Base class for all ITCH protocol messages used in Bangladesh stock exchanges
 * (DSE-BD and CSE-BD).
 *
 * <p>ITCH is a binary, unidirectional market data protocol. Messages are transmitted
 * as a stream of binary data, each with a length prefix and message type byte.
 */
public abstract class ItchMessage {

    private final char messageType;
    private final int stockLocate;
    private final int trackingNumber;
    private final long timestamp;

    /**
     * Constructs an ITCH message with the common header fields.
     *
     * @param messageType    the single-character message type identifier
     * @param stockLocate    the stock locate code assigned to the security
     * @param trackingNumber the NASDAQ internal tracking number
     * @param timestamp      the nanoseconds since midnight
     */
    protected ItchMessage(char messageType, int stockLocate, int trackingNumber, long timestamp) {
        this.messageType = messageType;
        this.stockLocate = stockLocate;
        this.trackingNumber = trackingNumber;
        this.timestamp = timestamp;
    }

    /**
     * Returns the message type character.
     *
     * @return message type
     */
    public char getMessageType() {
        return messageType;
    }

    /**
     * Returns the stock locate code.
     *
     * @return stock locate
     */
    public int getStockLocate() {
        return stockLocate;
    }

    /**
     * Returns the tracking number.
     *
     * @return tracking number
     */
    public int getTrackingNumber() {
        return trackingNumber;
    }

    /**
     * Returns the timestamp (nanoseconds since midnight).
     *
     * @return timestamp in nanoseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Serializes the message body fields into the provided ByteBuffer.
     * Subclasses must implement this to append their specific fields.
     *
     * @param buffer the ByteBuffer to write to
     */
    protected abstract void writeBody(ByteBuffer buffer);

    /**
     * Serializes the complete ITCH message (header + body) into a byte array.
     * The header consists of: messageType (1 byte), stockLocate (2 bytes),
     * trackingNumber (2 bytes), timestamp (6 bytes).
     *
     * @return the serialized message bytes
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put((byte) messageType);
        buffer.putShort((short) stockLocate);
        buffer.putShort((short) trackingNumber);
        // Write 6-byte timestamp (upper 2 bytes + lower 4 bytes)
        buffer.putShort((short) (timestamp >> 32));
        buffer.putInt((int) (timestamp & 0xFFFFFFFFL));
        writeBody(buffer);
        buffer.flip();
        byte[] result = new byte[buffer.limit()];
        buffer.get(result);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[type=" + messageType
                + ", stockLocate=" + stockLocate
                + ", trackingNumber=" + trackingNumber
                + ", timestamp=" + timestamp + "]";
    }
}
