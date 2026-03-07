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
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * FIX application handler for Bangladesh stock exchanges (DSE-BD and CSE-BD).
 *
 * <p>This application implements the QuickFIX/J {@link Application} interface and
 * handles FIX 4.4 messages for both DSE-BD and CSE-BD exchanges. When operating
 * as a server (acceptor), it processes incoming orders and sends back execution
 * reports. When operating as a client (initiator), it logs received execution reports.
 *
 * <p>Supported FIX messages:
 * <ul>
 *   <li>D - New Order Single: accepts the order and sends an execution report</li>
 *   <li>8 - Execution Report: logs the execution details on the client side</li>
 * </ul>
 */
public class TradingBdApplication extends MessageCracker implements Application {

    private static final Logger log = LoggerFactory.getLogger(TradingBdApplication.class);

    private final String exchangeName;
    private final AtomicInteger orderIdCounter = new AtomicInteger(0);
    private final AtomicInteger execIdCounter = new AtomicInteger(0);

    /**
     * Constructs a TradingBdApplication for the specified exchange.
     *
     * @param exchangeName the exchange name (e.g., "DSE-BD", "CSE-BD")
     */
    public TradingBdApplication(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    @Override
    public void onCreate(SessionID sessionID) {
        log.info("[{}] Session created: {}", exchangeName, sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("[{}] Session logged on: {}", exchangeName, sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.info("[{}] Session logged out: {}", exchangeName, sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    /**
     * Handles an incoming FIX 4.4 New Order Single (server-side).
     *
     * <p>Accepts the order and sends back an execution report with status NEW,
     * followed by a FILL execution report if the order can be executed.
     *
     * @param order     the incoming new order
     * @param sessionID the FIX session ID
     * @throws FieldNotFound          if a required field is missing
     * @throws UnsupportedMessageType if the message type is not supported
     * @throws IncorrectTagValue      if a field value is invalid
     */
    public void onMessage(NewOrderSingle order, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String clOrdId = order.getClOrdID().getValue();
        String symbol = order.getSymbol().getValue();
        char side = order.getSide().getValue();
        double qty = order.getOrderQty().getValue();
        char ordType = order.getOrdType().getValue();

        log.info("[{}] Received NewOrderSingle: clOrdId={}, symbol={}, side={}, qty={}, type={}",
                exchangeName, clOrdId, symbol, side, qty, ordType);

        // Generate order ID
        String orderId = exchangeName + "-" + orderIdCounter.incrementAndGet();
        String execId = "EXEC-" + execIdCounter.incrementAndGet();

        // Send acknowledgement (NEW status)
        ExecutionReport ack = new ExecutionReport(
                new OrderID(orderId),
                new ExecID(execId),
                new ExecType(ExecType.NEW),
                new OrdStatus(OrdStatus.NEW),
                new Side(side),
                new LeavesQty(qty),
                new CumQty(0),
                new AvgPx(0));
        ack.set(order.getClOrdID());
        ack.set(order.getSymbol());
        ack.set(order.getOrderQty());
        sendMessage(sessionID, ack);

        // If it's a limit order, simulate a fill
        if (ordType == OrdType.LIMIT) {
            double price = order.getPrice().getValue();
            String fillExecId = "EXEC-" + execIdCounter.incrementAndGet();

            ExecutionReport fill = new ExecutionReport(
                    new OrderID(orderId),
                    new ExecID(fillExecId),
                    new ExecType(ExecType.TRADE),
                    new OrdStatus(OrdStatus.FILLED),
                    new Side(side),
                    new LeavesQty(0),
                    new CumQty(qty),
                    new AvgPx(price));
            fill.set(order.getClOrdID());
            fill.set(order.getSymbol());
            fill.set(order.getOrderQty());
            fill.set(new Price(price));
            sendMessage(sessionID, fill);

            log.info("[{}] Order filled: clOrdId={}, symbol={}, qty={}, price={}",
                    exchangeName, clOrdId, symbol, qty, price);
        }
    }

    /**
     * Handles an incoming FIX 4.4 Execution Report (client-side).
     *
     * @param report    the incoming execution report
     * @param sessionID the FIX session ID
     * @throws FieldNotFound          if a required field is missing
     * @throws UnsupportedMessageType if the message type is not supported
     * @throws IncorrectTagValue      if a field value is invalid
     */
    public void onMessage(ExecutionReport report, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String orderId = report.getOrderID().getValue();
        String execType = String.valueOf(report.getExecType().getValue());
        String ordStatus = String.valueOf(report.getOrdStatus().getValue());

        log.info("[{}] Received ExecutionReport: orderId={}, execType={}, ordStatus={}",
                exchangeName, orderId, execType, ordStatus);
    }

    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFound e) {
            log.error("[{}] Session not found: {}", exchangeName, sessionID, e);
        }
    }
}
