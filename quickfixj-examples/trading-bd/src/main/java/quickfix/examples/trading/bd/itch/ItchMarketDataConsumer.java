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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ITCH market data consumer for Bangladesh stock exchanges (DSE-BD and CSE-BD).
 *
 * <p>This consumer parses incoming ITCH binary messages from the exchange's
 * market data feed and maintains an in-memory order book and trade history.
 * It supports the following message types:
 * <ul>
 *   <li>System Event (S) - system status changes</li>
 *   <li>Add Order (A) - new orders entering the book</li>
 *   <li>Order Executed (E) - partial or full execution of an order</li>
 *   <li>Order Cancel (X) - partial cancellation of an order</li>
 *   <li>Order Delete (D) - full cancellation of an order</li>
 *   <li>Trade (P) - non-cross trade executions</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 *   ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("DSE");
 *   consumer.processMessage(rawBytes);
 *   List&lt;TradeMessage&gt; trades = consumer.getTrades("SQURPHARMA");
 * </pre>
 */
public class ItchMarketDataConsumer {

    private static final Logger log = LoggerFactory.getLogger(ItchMarketDataConsumer.class);

    private static final int STOCK_SYMBOL_LENGTH = 8;

    private final String exchangeName;

    /** Active orders keyed by order reference number. */
    private final Map<Long, AddOrderMessage> orderBook = new HashMap<>();

    /** Executed trades keyed by stock symbol. */
    private final Map<String, List<TradeMessage>> tradesByStock = new HashMap<>();

    /** Statistics counters. */
    private long totalMessagesProcessed = 0;
    private long totalTradesProcessed = 0;
    private long totalOrdersAdded = 0;
    private long totalOrdersExecuted = 0;
    private long totalOrdersCancelled = 0;

    /**
     * Constructs a new ITCH market data consumer.
     *
     * @param exchangeName the exchange name (e.g., "DSE-BD" or "CSE-BD")
     */
    public ItchMarketDataConsumer(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    /**
     * Processes a raw ITCH message byte array.
     *
     * @param rawMessage the raw bytes of an ITCH message (without length prefix)
     */
    public void processMessage(byte[] rawMessage) {
        if (rawMessage == null || rawMessage.length < 1) {
            log.warn("[{}] Received empty or null ITCH message", exchangeName);
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(rawMessage);
        char messageType = (char) buffer.get();
        totalMessagesProcessed++;

        switch (messageType) {
            case ItchMessageType.SYSTEM_EVENT:
                processSystemEvent(buffer);
                break;
            case ItchMessageType.ADD_ORDER:
                processAddOrder(buffer);
                break;
            case ItchMessageType.ORDER_EXECUTED:
                processOrderExecuted(buffer);
                break;
            case ItchMessageType.ORDER_CANCEL:
                processOrderCancel(buffer);
                break;
            case ItchMessageType.ORDER_DELETE:
                processOrderDelete(buffer);
                break;
            case ItchMessageType.TRADE:
                processTrade(buffer);
                break;
            default:
                log.debug("[{}] Unhandled ITCH message type: {}", exchangeName, messageType);
                break;
        }
    }

    private void processSystemEvent(ByteBuffer buffer) {
        // Skip stock locate (2 bytes), tracking number (2 bytes), timestamp (6 bytes)
        buffer.position(buffer.position() + 10);
        char eventCode = (char) buffer.get();
        log.info("[{}] System Event: {}", exchangeName, describeEventCode(eventCode));
    }

    private String describeEventCode(char eventCode) {
        switch (eventCode) {
            case SystemEventMessage.EVENT_START_OF_MESSAGES:      return "Start of Messages";
            case SystemEventMessage.EVENT_START_OF_SYSTEM_HOURS:  return "Start of System Hours";
            case SystemEventMessage.EVENT_START_OF_MARKET_HOURS:  return "Start of Market Hours";
            case SystemEventMessage.EVENT_END_OF_MARKET_HOURS:    return "End of Market Hours";
            case SystemEventMessage.EVENT_END_OF_SYSTEM_HOURS:    return "End of System Hours";
            case SystemEventMessage.EVENT_END_OF_MESSAGES:        return "End of Messages";
            default:                                               return "Unknown(" + eventCode + ")";
        }
    }

    private void processAddOrder(ByteBuffer buffer) {
        int stockLocate = buffer.getShort() & 0xFFFF;
        int trackingNumber = buffer.getShort() & 0xFFFF;
        long timestamp = readTimestamp(buffer);
        long orderReferenceNumber = buffer.getLong();
        char buySellIndicator = (char) buffer.get();
        int shares = buffer.getInt();
        String stock = readStock(buffer);
        long price = buffer.getLong();

        AddOrderMessage order = new AddOrderMessage(stockLocate, trackingNumber, timestamp,
                orderReferenceNumber, buySellIndicator, shares, stock, price);
        orderBook.put(orderReferenceNumber, order);
        totalOrdersAdded++;

        log.debug("[{}] Add Order: {} {} {} shares of {} @ {}", exchangeName,
                orderReferenceNumber, buySellIndicator, shares, stock, price / 10000.0);
    }

    private void processOrderExecuted(ByteBuffer buffer) {
        // Skip stock locate (2), tracking (2), timestamp (6)
        buffer.position(buffer.position() + 10);
        long orderReferenceNumber = buffer.getLong();
        int executedShares = buffer.getInt();
        long matchNumber = buffer.getLong();

        AddOrderMessage order = orderBook.get(orderReferenceNumber);
        if (order != null) {
            totalOrdersExecuted++;
            log.debug("[{}] Order Executed: orderRef={}, shares={}, matchNumber={}", exchangeName,
                    orderReferenceNumber, executedShares, matchNumber);
        } else {
            log.warn("[{}] Order Executed: unknown orderRef={}", exchangeName, orderReferenceNumber);
        }
    }

    private void processOrderCancel(ByteBuffer buffer) {
        // Skip stock locate (2), tracking (2), timestamp (6)
        buffer.position(buffer.position() + 10);
        long orderReferenceNumber = buffer.getLong();
        int cancelledShares = buffer.getInt();

        if (orderBook.containsKey(orderReferenceNumber)) {
            totalOrdersCancelled++;
            log.debug("[{}] Order Cancel: orderRef={}, cancelledShares={}", exchangeName,
                    orderReferenceNumber, cancelledShares);
        } else {
            log.warn("[{}] Order Cancel: unknown orderRef={}", exchangeName, orderReferenceNumber);
        }
    }

    private void processOrderDelete(ByteBuffer buffer) {
        // Skip stock locate (2), tracking (2), timestamp (6)
        buffer.position(buffer.position() + 10);
        long orderReferenceNumber = buffer.getLong();

        if (orderBook.remove(orderReferenceNumber) != null) {
            totalOrdersCancelled++;
            log.debug("[{}] Order Delete: orderRef={}", exchangeName, orderReferenceNumber);
        } else {
            log.warn("[{}] Order Delete: unknown orderRef={}", exchangeName, orderReferenceNumber);
        }
    }

    private void processTrade(ByteBuffer buffer) {
        int stockLocate = buffer.getShort() & 0xFFFF;
        int trackingNumber = buffer.getShort() & 0xFFFF;
        long timestamp = readTimestamp(buffer);
        long orderReferenceNumber = buffer.getLong();
        char buySellIndicator = (char) buffer.get();
        int shares = buffer.getInt();
        String stock = readStock(buffer);
        long price = buffer.getLong();
        long matchNumber = buffer.getLong();

        TradeMessage trade = new TradeMessage(stockLocate, trackingNumber, timestamp,
                orderReferenceNumber, buySellIndicator, shares, stock, price, matchNumber);
        tradesByStock.computeIfAbsent(stock.trim(), k -> new ArrayList<>()).add(trade);
        totalTradesProcessed++;

        log.info("[{}] Trade: {} shares of {} @ {} (match={})", exchangeName,
                shares, stock.trim(), price / 10000.0, matchNumber);
    }

    private long readTimestamp(ByteBuffer buffer) {
        long upper = buffer.getShort() & 0xFFFFL;
        long lower = buffer.getInt() & 0xFFFFFFFFL;
        return (upper << 32) | lower;
    }

    private String readStock(ByteBuffer buffer) {
        byte[] stockBytes = new byte[STOCK_SYMBOL_LENGTH];
        buffer.get(stockBytes);
        return new String(stockBytes, StandardCharsets.US_ASCII).trim();
    }

    /**
     * Returns the active orders in the order book.
     *
     * @return unmodifiable view of the order book (keyed by order reference number)
     */
    public Map<Long, AddOrderMessage> getOrderBook() {
        return Collections.unmodifiableMap(orderBook);
    }

    /**
     * Returns the list of trades for the given stock symbol.
     *
     * @param stock the stock symbol
     * @return list of trade messages, or an empty list if none
     */
    public List<TradeMessage> getTrades(String stock) {
        return Collections.unmodifiableList(tradesByStock.getOrDefault(stock, Collections.emptyList()));
    }

    /**
     * Returns the exchange name.
     *
     * @return exchange name
     */
    public String getExchangeName() {
        return exchangeName;
    }

    /**
     * Returns the total number of messages processed.
     *
     * @return total messages processed
     */
    public long getTotalMessagesProcessed() {
        return totalMessagesProcessed;
    }

    /**
     * Returns the total number of trades processed.
     *
     * @return total trades
     */
    public long getTotalTradesProcessed() {
        return totalTradesProcessed;
    }

    /**
     * Returns the total number of orders added.
     *
     * @return total orders added
     */
    public long getTotalOrdersAdded() {
        return totalOrdersAdded;
    }

    /**
     * Returns the total number of orders executed.
     *
     * @return total orders executed
     */
    public long getTotalOrdersExecuted() {
        return totalOrdersExecuted;
    }

    /**
     * Returns the total number of orders cancelled.
     *
     * @return total orders cancelled
     */
    public long getTotalOrdersCancelled() {
        return totalOrdersCancelled;
    }

    /**
     * Prints a statistics summary to the logger.
     */
    public void printStatistics() {
        log.info("[{}] Statistics: messages={}, orders={}, executions={}, cancellations={}, trades={}",
                exchangeName, totalMessagesProcessed, totalOrdersAdded,
                totalOrdersExecuted, totalOrdersCancelled, totalTradesProcessed);
        log.info("[{}] Active orders in book: {}", exchangeName, orderBook.size());
    }
}
