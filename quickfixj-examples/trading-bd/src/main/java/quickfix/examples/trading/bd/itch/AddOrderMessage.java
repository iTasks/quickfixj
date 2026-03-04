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
 * ITCH Add Order message (type 'A').
 *
 * <p>An Add Order message represents a newly accepted visible order on the exchange.
 * It includes a day-specific reference number. The price field uses 4 decimal places.
 *
 * <p>Used by DSE-BD (Dhaka Stock Exchange) and CSE-BD (Chittagong Stock Exchange)
 * for broadcasting new order events in the market data stream.
 */
public class AddOrderMessage extends ItchMessage {

    /** Buy side indicator. */
    public static final char SIDE_BUY = 'B';
    /** Sell side indicator. */
    public static final char SIDE_SELL = 'S';

    /** Maximum stock symbol length (right-padded with spaces). */
    private static final int STOCK_SYMBOL_LENGTH = 8;

    private final long orderReferenceNumber;
    private final char buySellIndicator;
    private final int shares;
    private final String stock;
    private final long price;

    /**
     * Constructs an Add Order message.
     *
     * @param stockLocate          the stock locate code
     * @param trackingNumber       the tracking number
     * @param timestamp            the timestamp in nanoseconds since midnight
     * @param orderReferenceNumber the unique reference number for this order
     * @param buySellIndicator     'B' for buy or 'S' for sell
     * @param shares               the total number of shares associated with the order
     * @param stock                the stock symbol (up to 8 characters)
     * @param price                the display price of the new order (4 decimal places implicit)
     */
    public AddOrderMessage(int stockLocate, int trackingNumber, long timestamp,
            long orderReferenceNumber, char buySellIndicator, int shares, String stock, long price) {
        super(ItchMessageType.ADD_ORDER, stockLocate, trackingNumber, timestamp);
        this.orderReferenceNumber = orderReferenceNumber;
        this.buySellIndicator = buySellIndicator;
        this.shares = shares;
        this.stock = stock;
        this.price = price;
    }

    /**
     * Returns the unique order reference number.
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
     * Returns the number of shares.
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
     * Returns the price (with 4 implicit decimal places).
     * For example, a price of 102500 represents 10.2500.
     *
     * @return price in units with 4 decimal places
     */
    public long getPrice() {
        return price;
    }

    /**
     * Returns the price as a double.
     *
     * @return price as double
     */
    public double getPriceAsDouble() {
        return price / 10000.0;
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
        // Pad remaining bytes with spaces
        for (int i = symbolBytes.length; i < STOCK_SYMBOL_LENGTH; i++) {
            stockBytes[i] = ' ';
        }
        buffer.put(stockBytes);
        buffer.putInt((int) price);
    }

    @Override
    public String toString() {
        return "AddOrderMessage[orderRef=" + orderReferenceNumber
                + ", side=" + buySellIndicator
                + ", shares=" + shares
                + ", stock=" + stock
                + ", price=" + getPriceAsDouble()
                + ", " + super.toString() + "]";
    }
}
