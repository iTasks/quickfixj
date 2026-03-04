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

/**
 * ITCH protocol message type constants used by Bangladesh stock exchanges
 * (DSE-BD and CSE-BD) for market data streaming.
 *
 * <p>Based on the NASDAQ ITCH 5.0 specification adapted for Bangladesh exchanges.
 */
public final class ItchMessageType {

    /** System event message - notifies of system-level events. */
    public static final char SYSTEM_EVENT = 'S';

    /** Stock directory message - provides stock symbol information. */
    public static final char STOCK_DIRECTORY = 'R';

    /** Add order (no MPID attribution) - a new order has been accepted. */
    public static final char ADD_ORDER = 'A';

    /** Add order with MPID attribution - a new order with market participant ID. */
    public static final char ADD_ORDER_WITH_MPID = 'F';

    /** Order executed - an order has been executed in whole or in part. */
    public static final char ORDER_EXECUTED = 'E';

    /** Order executed with price - an order executed at a non-displayed price. */
    public static final char ORDER_EXECUTED_WITH_PRICE = 'C';

    /** Order cancel - cancel a portion of an order at the given price. */
    public static final char ORDER_CANCEL = 'X';

    /** Order delete - an order is being cancelled. */
    public static final char ORDER_DELETE = 'D';

    /** Order replace - an existing order has been replaced. */
    public static final char ORDER_REPLACE = 'U';

    /** Trade message - non-cross trade. */
    public static final char TRADE = 'P';

    /** Cross trade - a cross trade has been executed. */
    public static final char CROSS_TRADE = 'Q';

    /** Broken trade - trade break of an execution on the exchange. */
    public static final char BROKEN_TRADE = 'B';

    /** Net order imbalance indicator - used during opening and closing processes. */
    public static final char NET_ORDER_IMBALANCE = 'I';

    private ItchMessageType() {
        // Utility class, prevent instantiation
    }
}
