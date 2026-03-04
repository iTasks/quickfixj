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
 * ITCH System Event message (type 'S').
 *
 * <p>The Exchange uses this message to indicate a change in system status.
 * These messages will be sent whenever the system changes status or an event
 * occurs that impacts exchange operations.
 *
 * <p>Event codes used by DSE-BD and CSE-BD:
 * <ul>
 *   <li>'O' - Start of Messages</li>
 *   <li>'S' - Start of System Hours</li>
 *   <li>'Q' - Start of Market Hours</li>
 *   <li>'M' - End of Market Hours</li>
 *   <li>'E' - End of System Hours</li>
 *   <li>'C' - End of Messages</li>
 * </ul>
 */
public class SystemEventMessage extends ItchMessage {

    /** Start of Messages. */
    public static final char EVENT_START_OF_MESSAGES = 'O';
    /** Start of System Hours. */
    public static final char EVENT_START_OF_SYSTEM_HOURS = 'S';
    /** Start of Market Hours. */
    public static final char EVENT_START_OF_MARKET_HOURS = 'Q';
    /** End of Market Hours. */
    public static final char EVENT_END_OF_MARKET_HOURS = 'M';
    /** End of System Hours. */
    public static final char EVENT_END_OF_SYSTEM_HOURS = 'E';
    /** End of Messages. */
    public static final char EVENT_END_OF_MESSAGES = 'C';

    private final char eventCode;

    /**
     * Constructs a System Event message.
     *
     * @param stockLocate    the stock locate code (0 for system events)
     * @param trackingNumber the tracking number
     * @param timestamp      the timestamp in nanoseconds since midnight
     * @param eventCode      the event code character
     */
    public SystemEventMessage(int stockLocate, int trackingNumber, long timestamp, char eventCode) {
        super(ItchMessageType.SYSTEM_EVENT, stockLocate, trackingNumber, timestamp);
        this.eventCode = eventCode;
    }

    /**
     * Returns the event code character.
     *
     * @return event code
     */
    public char getEventCode() {
        return eventCode;
    }

    @Override
    protected void writeBody(ByteBuffer buffer) {
        buffer.put((byte) eventCode);
    }

    @Override
    public String toString() {
        return "SystemEventMessage[eventCode=" + eventCode + ", " + super.toString() + "]";
    }
}
