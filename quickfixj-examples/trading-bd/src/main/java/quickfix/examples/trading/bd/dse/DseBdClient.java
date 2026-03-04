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

package quickfix.examples.trading.bd.dse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.examples.trading.bd.TradingBdApplication;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

import java.io.InputStream;

/**
 * DSE-BD (Dhaka Stock Exchange, Bangladesh) FIX protocol client.
 *
 * <p>This client acts as a FIX initiator connecting to the Dhaka Stock Exchange
 * server. It can submit new orders, cancel existing orders, and request order
 * status updates using FIX 4.4.
 *
 * <p>DSE-BD client connects to the server on port 5001 by default
 * (configurable via {@code dse_client.cfg}).
 *
 * <p>Usage:
 * <pre>
 *   DseBdClient client = new DseBdClient(settings);
 *   client.start();
 *   client.sendNewOrder("SQURPHARMA", Side.BUY, 100, 10.25);
 * </pre>
 */
public class DseBdClient {

    private static final Logger log = LoggerFactory.getLogger(DseBdClient.class);

    private final SocketInitiator initiator;
    private final TradingBdApplication application;

    private int orderIdCounter = 0;

    /**
     * Constructs a DSE-BD FIX client with the given session settings.
     *
     * @param settings the session settings (loaded from dse_client.cfg)
     * @throws ConfigError       if the configuration is invalid
     * @throws FieldConvertError if a configuration field cannot be converted
     */
    public DseBdClient(SessionSettings settings) throws ConfigError, FieldConvertError {
        application = new TradingBdApplication("DSE-BD-CLIENT");
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();

        initiator = new SocketInitiator(application, messageStoreFactory, settings,
                logFactory, messageFactory);
    }

    /**
     * Starts the DSE-BD FIX client and initiates a session with the exchange.
     *
     * @throws RuntimeError if the initiator fails to start
     * @throws ConfigError  if the configuration is invalid
     */
    public void start() throws RuntimeError, ConfigError {
        initiator.start();
        log.info("DSE-BD FIX client started, connecting to exchange");
    }

    /**
     * Stops the DSE-BD FIX client and closes the session.
     */
    public void stop() {
        initiator.stop();
        log.info("DSE-BD FIX client stopped");
    }

    /**
     * Sends a new limit order to the DSE-BD exchange.
     *
     * @param symbol   the stock symbol (e.g., "SQURPHARMA")
     * @param side     the order side ({@link Side#BUY} or {@link Side#SELL})
     * @param quantity the number of shares to buy or sell
     * @param price    the limit price per share
     * @return the generated client order ID, or {@code null} if no session is available
     */
    public String sendNewOrder(String symbol, char side, int quantity, double price) {
        SessionID sessionID = getActiveSession();
        if (sessionID == null) {
            log.warn("DSE-BD: No active FIX session available");
            return null;
        }

        String clOrdId = "DSE-" + (++orderIdCounter);
        NewOrderSingle order = new NewOrderSingle(
                new ClOrdID(clOrdId),
                new Side(side),
                new TransactTime(),
                new OrdType(OrdType.LIMIT));
        order.set(new Symbol(symbol));
        order.set(new OrderQty(quantity));
        order.set(new Price(price));

        try {
            Session.sendToTarget(order, sessionID);
            log.info("DSE-BD: Sent new order clOrdId={}, symbol={}, side={}, qty={}, price={}",
                    clOrdId, symbol, side, quantity, price);
        } catch (Exception e) {
            log.error("DSE-BD: Failed to send new order", e);
            return null;
        }
        return clOrdId;
    }

    private SessionID getActiveSession() {
        for (SessionID sessionID : initiator.getSessions()) {
            Session session = Session.lookupSession(sessionID);
            if (session != null && session.isLoggedOn()) {
                return sessionID;
            }
        }
        return null;
    }

    /**
     * Creates and starts a DSE-BD client from the provided configuration stream.
     *
     * @param configStream the input stream for the client configuration
     * @return a started {@code DseBdClient} instance
     * @throws Exception if initialization or startup fails
     */
    public static DseBdClient create(InputStream configStream) throws Exception {
        SessionSettings settings = new SessionSettings(configStream);
        DseBdClient client = new DseBdClient(settings);
        client.start();
        return client;
    }
}
