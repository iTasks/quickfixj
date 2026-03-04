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
import java.nio.charset.StandardCharsets;

/**
 * ITCH Trade message (type 'P').
 *
 * <p>Trade messages are generated on executions on the exchange. It includes
 * a match number that uniquely identifies the execution.
 *
 * <p>Used by DSE-BD (Dhaka Stock Exchange) and CSE-BD (Chittagong Stock Exchange)
 * for broadcasting trade execution events in the market data stream.
 */
public class TradeMessage extends ItchMessage {

    /** Maximum stock symbol length (right-padded with spaces). */
    private static final int STOCK_SYMBOL_LENGTH = 8;

    private final long orderReferenceNumber;
    private final char buySellIndicator;
    private final int shares;
    private final String stock;
    private final long price;
    private final long matchNumber;

    /**
     * Constructs a Trade message.
     *
     * @param stockLocate          the stock locate code
     * @param trackingNumber       the tracking number
     * @param timestamp            the timestamp in nanoseconds since midnight
     * @param orderReferenceNumber the reference to the order that was executed
     * @param buySellIndicator     the side of the order that triggered the trade
     * @param shares               the number of shares executed in the trade
     * @param stock                the stock symbol
     * @param price                the execution price (4 decimal places implicit)
     * @param matchNumber          the unique match identifier for this trade
     */
    public TradeMessage(int stockLocate, int trackingNumber, long timestamp,
            long orderReferenceNumber, char buySellIndicator, int shares,
            String stock, long price, long matchNumber) {
        super(ItchMessageType.TRADE, stockLocate, trackingNumber, timestamp);
        this.orderReferenceNumber = orderReferenceNumber;
        this.buySellIndicator = buySellIndicator;
        this.shares = shares;
        this.stock = stock;
        this.price = price;
        this.matchNumber = matchNumber;
    }

    /**
     * Returns the order reference number.
     *
     * @return order reference number
     */
    public long getOrderReferenceNumber() {
        return orderReferenceNumber;
    }

    /**
     * Returns the buy/sell indicator.
     *
     * @return 'B' for buy, 'S' for sell
     */
    public char getBuySellIndicator() {
        return buySellIndicator;
    }

    /**
     * Returns the number of shares traded.
     *
     * @return shares quantity
     */
    public int getShares() {
        return shares;
    }

    /**
     * Returns the stock symbol.
     *
     * @return stock symbol
     */
    public String getStock() {
        return stock;
    }

    /**
     * Returns the execution price (with 4 implicit decimal places).
     *
     * @return price in units with 4 decimal places
     */
    public long getPrice() {
        return price;
    }

    /**
     * Returns the execution price as a double.
     *
     * @return price as double
     */
    public double getPriceAsDouble() {
        return price / 10000.0;
    }

    /**
     * Returns the unique match number for this execution.
     *
     * @return match number
     */
    public long getMatchNumber() {
        return matchNumber;
    }

    @Override
    protected void writeBody(ByteBuffer buffer) {
        buffer.putLong(orderReferenceNumber);
        buffer.put((byte) buySellIndicator);
        buffer.putInt(shares);
        byte[] stockBytes = new byte[STOCK_SYMBOL_LENGTH];
        byte[] symbolBytes = stock.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(symbolBytes, 0, stockBytes, 0,
                Math.min(symbolBytes.length, STOCK_SYMBOL_LENGTH));
        for (int i = symbolBytes.length; i < STOCK_SYMBOL_LENGTH; i++) {
            stockBytes[i] = ' ';
        }
        buffer.put(stockBytes);
        buffer.putLong(price);
        buffer.putLong(matchNumber);
    }

    @Override
    public String toString() {
        return "TradeMessage[orderRef=" + orderReferenceNumber
                + ", side=" + buySellIndicator
                + ", shares=" + shares
                + ", stock=" + stock
                + ", price=" + getPriceAsDouble()
                + ", matchNumber=" + matchNumber
                + ", " + super.toString() + "]";
    }
}
