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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ITCH market data consumer.
 */
public class ItchMarketDataConsumerTest {

    @Test
    public void testProcessSystemEvent() {
        ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("TEST-EXCHANGE");

        SystemEventMessage event = new SystemEventMessage(
                0, 1, 1000000L, SystemEventMessage.EVENT_START_OF_MARKET_HOURS);
        consumer.processMessage(event.toBytes());

        assertEquals(1, consumer.getTotalMessagesProcessed());
        assertEquals(0, consumer.getTotalOrdersAdded());
        assertEquals(0, consumer.getTotalTradesProcessed());
    }

    @Test
    public void testProcessAddOrder() {
        ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("DSE-BD");

        AddOrderMessage order = new AddOrderMessage(
                1, 2, 2000000L, 100001L, AddOrderMessage.SIDE_BUY, 200, "SQURPHAR", 2105000L);
        consumer.processMessage(order.toBytes());

        assertEquals(1, consumer.getTotalMessagesProcessed());
        assertEquals(1, consumer.getTotalOrdersAdded());

        Map<Long, AddOrderMessage> book = consumer.getOrderBook();
        assertTrue(book.containsKey(100001L));
        AddOrderMessage stored = book.get(100001L);
        assertEquals("SQURPHAR", stored.getStock());
        assertEquals(AddOrderMessage.SIDE_BUY, stored.getBuySellIndicator());
        assertEquals(200, stored.getShares());
        assertEquals(2105000L, stored.getPrice());
        assertEquals(210.5, stored.getPriceAsDouble(), 0.001);
    }

    @Test
    public void testProcessTrade() {
        ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("DSE-BD");

        TradeMessage trade = new TradeMessage(
                1, 3, 3000000L, 100001L, AddOrderMessage.SIDE_BUY,
                150, "SQURPHAR", 2105000L, 500001L);
        consumer.processMessage(trade.toBytes());

        assertEquals(1, consumer.getTotalMessagesProcessed());
        assertEquals(1, consumer.getTotalTradesProcessed());

        List<TradeMessage> trades = consumer.getTrades("SQURPHAR");
        assertEquals(1, trades.size());
        TradeMessage stored = trades.get(0);
        assertEquals(150, stored.getShares());
        assertEquals(2105000L, stored.getPrice());
        assertEquals(500001L, stored.getMatchNumber());
    }

    @Test
    public void testProcessMultipleMessages() {
        ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("CSE-BD");

        // System event
        consumer.processMessage(new SystemEventMessage(
                0, 1, 1000000L, SystemEventMessage.EVENT_START_OF_MARKET_HOURS).toBytes());

        // Add two orders
        consumer.processMessage(new AddOrderMessage(
                1, 2, 2000000L, 200001L, AddOrderMessage.SIDE_BUY, 300, "ACIFORMU", 285000L).toBytes());
        consumer.processMessage(new AddOrderMessage(
                1, 3, 3000000L, 200002L, AddOrderMessage.SIDE_SELL, 200, "ACIFORMU", 290000L).toBytes());

        // Add a trade
        consumer.processMessage(new TradeMessage(
                1, 4, 4000000L, 200001L, AddOrderMessage.SIDE_BUY,
                200, "ACIFORMU", 285000L, 600001L).toBytes());

        assertEquals(4, consumer.getTotalMessagesProcessed());
        assertEquals(2, consumer.getTotalOrdersAdded());
        assertEquals(1, consumer.getTotalTradesProcessed());
        assertEquals(2, consumer.getOrderBook().size());
    }

    @Test
    public void testAddOrderMessageFields() {
        AddOrderMessage order = new AddOrderMessage(
                5, 10, 5000000L, 999L, AddOrderMessage.SIDE_SELL, 1000, "GRAMEEN", 357500L);

        assertEquals(ItchMessageType.ADD_ORDER, order.getMessageType());
        assertEquals(5, order.getStockLocate());
        assertEquals(10, order.getTrackingNumber());
        assertEquals(5000000L, order.getTimestamp());
        assertEquals(999L, order.getOrderReferenceNumber());
        assertEquals(AddOrderMessage.SIDE_SELL, order.getBuySellIndicator());
        assertEquals(1000, order.getShares());
        assertEquals("GRAMEEN", order.getStock());
        assertEquals(357500L, order.getPrice());
        assertEquals(35.75, order.getPriceAsDouble(), 0.001);
    }

    @Test
    public void testTradeMessageFields() {
        TradeMessage trade = new TradeMessage(
                3, 7, 7000000L, 888L, AddOrderMessage.SIDE_BUY,
                500, "BSRMSTEE", 855000L, 777777L);

        assertEquals(ItchMessageType.TRADE, trade.getMessageType());
        assertEquals(888L, trade.getOrderReferenceNumber());
        assertEquals(AddOrderMessage.SIDE_BUY, trade.getBuySellIndicator());
        assertEquals(500, trade.getShares());
        assertEquals("BSRMSTEE", trade.getStock());
        assertEquals(855000L, trade.getPrice());
        assertEquals(85.5, trade.getPriceAsDouble(), 0.001);
        assertEquals(777777L, trade.getMatchNumber());
    }

    @Test
    public void testSystemEventMessageFields() {
        SystemEventMessage event = new SystemEventMessage(
                0, 0, 9000000L, SystemEventMessage.EVENT_END_OF_MARKET_HOURS);

        assertEquals(ItchMessageType.SYSTEM_EVENT, event.getMessageType());
        assertEquals(SystemEventMessage.EVENT_END_OF_MARKET_HOURS, event.getEventCode());
    }

    @Test
    public void testNullMessageHandledGracefully() {
        ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("TEST");
        // Should not throw
        consumer.processMessage(null);
        consumer.processMessage(new byte[0]);
        assertEquals(0, consumer.getTotalMessagesProcessed());
    }

    @Test
    public void testEmptyTradeListForUnknownStock() {
        ItchMarketDataConsumer consumer = new ItchMarketDataConsumer("TEST");
        List<TradeMessage> trades = consumer.getTrades("UNKNOWN");
        assertNotNull(trades);
        assertTrue(trades.isEmpty());
    }
}
