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

package quickfix.examples.trading.bd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.SessionSettings;
import quickfix.examples.trading.bd.cse.CseBdClient;
import quickfix.examples.trading.bd.cse.CseBdServer;
import quickfix.examples.trading.bd.dse.DseBdClient;
import quickfix.examples.trading.bd.dse.DseBdServer;
import quickfix.examples.trading.bd.fast.FastDecoder;
import quickfix.examples.trading.bd.fast.FastEncoder;
import quickfix.examples.trading.bd.itch.AddOrderMessage;
import quickfix.examples.trading.bd.itch.ItchMarketDataConsumer;
import quickfix.examples.trading.bd.itch.SystemEventMessage;
import quickfix.examples.trading.bd.itch.TradeMessage;
import quickfix.field.Side;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Main entry point for the DSE-BD and CSE-BD trading sample application.
 *
 * <p>This application demonstrates integration with Bangladesh stock exchanges
 * (DSE-BD: Dhaka Stock Exchange, CSE-BD: Chittagong Stock Exchange) using:
 * <ul>
 *   <li><b>FIX 4.4</b> for order routing (server and client modes)</li>
 *   <li><b>ITCH</b> binary protocol for real-time market data streaming</li>
 *   <li><b>FAST</b> (FIX Adapted for STreaming) for compressed data encoding</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   # Start DSE-BD FIX server (accepts client connections on port 5001)
 *   java -cp ... TradingBdMain dse-server [configFile]
 *
 *   # Start DSE-BD FIX client (connects to server on port 5001)
 *   java -cp ... TradingBdMain dse-client [configFile]
 *
 *   # Start CSE-BD FIX server (accepts client connections on port 5002)
 *   java -cp ... TradingBdMain cse-server [configFile]
 *
 *   # Start CSE-BD FIX client (connects to server on port 5002)
 *   java -cp ... TradingBdMain cse-client [configFile]
 *
 *   # Run ITCH market data consumer demo
 *   java -cp ... TradingBdMain itch-demo
 *
 *   # Run FAST encoding/decoding demo
 *   java -cp ... TradingBdMain fast-demo
 * </pre>
 */
public class TradingBdMain {

    private static final Logger log = LoggerFactory.getLogger(TradingBdMain.class);

    /**
     * Application entry point.
     *
     * @param args command-line arguments: mode [configFile]
     * @throws Exception if an error occurs during execution
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "dse-server":
                runDseBdServer(args);
                break;
            case "dse-client":
                runDseBdClient(args);
                break;
            case "cse-server":
                runCseBdServer(args);
                break;
            case "cse-client":
                runCseBdClient(args);
                break;
            case "itch-demo":
                runItchDemo();
                break;
            case "fast-demo":
                runFastDemo();
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                printUsage();
                System.exit(1);
        }
    }

    private static void runDseBdServer(String[] args) throws Exception {
        InputStream configStream = getConfigStream(args, 1, "dse_server.cfg");
        SessionSettings settings = new SessionSettings(configStream);
        configStream.close();

        DseBdServer server = new DseBdServer(settings);
        server.start();

        System.out.println("DSE-BD FIX server started. Press <enter> to quit.");
        System.in.read();

        server.stop();
    }

    private static void runDseBdClient(String[] args) throws Exception {
        InputStream configStream = getConfigStream(args, 1, "dse_client.cfg");
        SessionSettings settings = new SessionSettings(configStream);
        configStream.close();

        DseBdClient client = new DseBdClient(settings);
        client.start();

        System.out.println("DSE-BD FIX client started. Waiting for session to establish...");
        Thread.sleep(2000);

        System.out.println("Sending sample orders to DSE-BD exchange...");
        client.sendNewOrder("SQURPHARMA", Side.BUY, 100, 210.50);
        client.sendNewOrder("GRAMEEN", Side.SELL, 200, 35.75);
        client.sendNewOrder("BSRMSTEEL", Side.BUY, 500, 85.20);

        System.out.println("Press <enter> to quit.");
        System.in.read();

        client.stop();
    }

    private static void runCseBdServer(String[] args) throws Exception {
        InputStream configStream = getConfigStream(args, 1, "cse_server.cfg");
        SessionSettings settings = new SessionSettings(configStream);
        configStream.close();

        CseBdServer server = new CseBdServer(settings);
        server.start();

        System.out.println("CSE-BD FIX server started. Press <enter> to quit.");
        System.in.read();

        server.stop();
    }

    private static void runCseBdClient(String[] args) throws Exception {
        InputStream configStream = getConfigStream(args, 1, "cse_client.cfg");
        SessionSettings settings = new SessionSettings(configStream);
        configStream.close();

        CseBdClient client = new CseBdClient(settings);
        client.start();

        System.out.println("CSE-BD FIX client started. Waiting for session to establish...");
        Thread.sleep(2000);

        System.out.println("Sending sample orders to CSE-BD exchange...");
        client.sendNewOrder("ACIFORMULA", Side.BUY, 300, 28.50);
        client.sendNewOrder("BEACONPHAR", Side.SELL, 150, 120.00);

        System.out.println("Press <enter> to quit.");
        System.in.read();

        client.stop();
    }

    private static void runItchDemo() {
        System.out.println("=== ITCH Market Data Streaming Demo ===");
        System.out.println("Simulating DSE-BD ITCH market data feed...\n");

        // Create ITCH consumer for DSE-BD
        ItchMarketDataConsumer dseConsumer = new ItchMarketDataConsumer("DSE-BD");

        // Simulate system event: Start of Market Hours
        SystemEventMessage startEvent = new SystemEventMessage(
                0, 1, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                SystemEventMessage.EVENT_START_OF_MARKET_HOURS);
        dseConsumer.processMessage(startEvent.toBytes());

        // Simulate add orders for DSE-BD listed stocks
        AddOrderMessage buyOrder1 = new AddOrderMessage(
                1, 2, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                100001L, AddOrderMessage.SIDE_BUY, 200, "SQURPHAR", 2105000L);
        dseConsumer.processMessage(buyOrder1.toBytes());

        AddOrderMessage sellOrder1 = new AddOrderMessage(
                1, 3, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                100002L, AddOrderMessage.SIDE_SELL, 150, "SQURPHAR", 2110000L);
        dseConsumer.processMessage(sellOrder1.toBytes());

        AddOrderMessage buyOrder2 = new AddOrderMessage(
                2, 4, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                100003L, AddOrderMessage.SIDE_BUY, 500, "BSRMSTEEL", 855000L);
        dseConsumer.processMessage(buyOrder2.toBytes());

        // Simulate a trade
        TradeMessage trade = new TradeMessage(
                1, 5, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                100001L, AddOrderMessage.SIDE_BUY, 150, "SQURPHAR", 2105000L, 500001L);
        dseConsumer.processMessage(trade.toBytes());

        // Also demonstrate CSE-BD ITCH consumer
        System.out.println("\nSimulating CSE-BD ITCH market data feed...\n");
        ItchMarketDataConsumer cseConsumer = new ItchMarketDataConsumer("CSE-BD");

        SystemEventMessage cseStartEvent = new SystemEventMessage(
                0, 1, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                SystemEventMessage.EVENT_START_OF_MARKET_HOURS);
        cseConsumer.processMessage(cseStartEvent.toBytes());

        AddOrderMessage cseBuyOrder = new AddOrderMessage(
                1, 2, System.nanoTime() % (24L * 60 * 60 * 1_000_000_000L),
                200001L, AddOrderMessage.SIDE_BUY, 300, "ACIFORMU", 285000L);
        cseConsumer.processMessage(cseBuyOrder.toBytes());

        // Print statistics
        System.out.println();
        dseConsumer.printStatistics();
        cseConsumer.printStatistics();
        System.out.println("\nITCH demo complete.");
    }

    private static void runFastDemo() {
        System.out.println("=== FAST (FIX Adapted for STreaming) Demo ===");
        System.out.println("Demonstrating FAST encoding/decoding for market data streaming...\n");

        FastEncoder encoder = new FastEncoder();
        FastDecoder decoder = new FastDecoder();

        // Demonstrate market data update encoding
        System.out.println("1. Encoding market data update (SQURPHARMA, price=210.5000, qty=200):");
        byte[] encoded = encoder.encodeMarketDataUpdate("SQURPHARMA", 2105000L, 200);
        System.out.println("   Encoded length: " + encoded.length + " bytes (vs. ~50 bytes uncompressed)");

        Map<String, Object> decoded = decoder.decode(encoded);
        System.out.println("   Decoded: symbol=" + decoded.get("symbol")
                + ", price=" + ((Long) decoded.get("price") / 10000.0)
                + ", qty=" + decoded.get("quantity"));

        // Demonstrate order event encoding
        System.out.println("\n2. Encoding order event (orderRef=100001, BUY, BSRMSTEEL, price=85.50, qty=500):");
        byte[] orderEncoded = encoder.encodeOrderEvent(100001L, 'B', "BSRMSTEEL", 855000L, 500);
        System.out.println("   Encoded length: " + orderEncoded.length + " bytes");

        Map<String, Object> orderDecoded = decoder.decode(orderEncoded);
        System.out.println("   Decoded: orderRef=" + orderDecoded.get("orderReference")
                + ", side=" + orderDecoded.get("side")
                + ", symbol=" + orderDecoded.get("symbol")
                + ", price=" + ((Long) orderDecoded.get("price") / 10000.0)
                + ", qty=" + orderDecoded.get("quantity"));

        // Demonstrate trade event encoding
        System.out.println("\n3. Encoding trade event (matchNum=500001, SQURPHARMA, price=210.5000, qty=150):");
        byte[] tradeEncoded = encoder.encodeTradeEvent(500001L, "SQURPHARMA", 2105000L, 150);
        System.out.println("   Encoded length: " + tradeEncoded.length + " bytes");

        Map<String, Object> tradeDecoded = decoder.decode(tradeEncoded);
        System.out.println("   Decoded: matchNum=" + tradeDecoded.get("matchNumber")
                + ", symbol=" + tradeDecoded.get("symbol")
                + ", price=" + ((Long) tradeDecoded.get("price") / 10000.0)
                + ", qty=" + tradeDecoded.get("quantity"));

        System.out.println("\nFAST demo complete.");
    }

    private static InputStream getConfigStream(String[] args, int configArgIndex, String defaultConfig)
            throws Exception {
        if (args.length > configArgIndex) {
            return new FileInputStream(args[configArgIndex]);
        }
        InputStream stream = TradingBdMain.class.getResourceAsStream(
                "quickfix/examples/trading/bd/" + defaultConfig);
        if (stream == null) {
            throw new IllegalArgumentException("Config file not found: " + defaultConfig
                    + ". Please provide a config file path as argument.");
        }
        return stream;
    }

    private static void printUsage() {
        System.out.println("DSE-BD and CSE-BD Trading Sample Application");
        System.out.println("Usage: TradingBdMain <mode> [configFile]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  dse-server [configFile]  - Start DSE-BD FIX server (default port 5001)");
        System.out.println("  dse-client [configFile]  - Start DSE-BD FIX client");
        System.out.println("  cse-server [configFile]  - Start CSE-BD FIX server (default port 5002)");
        System.out.println("  cse-client [configFile]  - Start CSE-BD FIX client");
        System.out.println("  itch-demo                - Run ITCH market data streaming demo");
        System.out.println("  fast-demo                - Run FAST encoding/decoding demo");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  TradingBdMain dse-server");
        System.out.println("  TradingBdMain dse-client /path/to/dse_client.cfg");
        System.out.println("  TradingBdMain itch-demo");
        System.out.println("  TradingBdMain fast-demo");
    }
}
