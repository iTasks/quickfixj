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

package quickfix.examples.trading.bd.cse;

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
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.examples.trading.bd.TradingBdApplication;

import java.io.InputStream;

/**
 * CSE-BD (Chittagong Stock Exchange, Bangladesh) FIX protocol server.
 *
 * <p>This server acts as a FIX acceptor for the Chittagong Stock Exchange, accepting
 * incoming FIX 4.4 sessions from trading clients. It processes new order requests,
 * cancellations, and order status queries.
 *
 * <p>CSE-BD server listens on port 5002 by default (configurable via {@code cse_server.cfg}).
 *
 * <p>Usage:
 * <pre>
 *   CseBdServer server = new CseBdServer(settings);
 *   server.start();
 * </pre>
 */
public class CseBdServer {

    private static final Logger log = LoggerFactory.getLogger(CseBdServer.class);

    private final SocketAcceptor acceptor;

    /**
     * Constructs a CSE-BD FIX server with the given session settings.
     *
     * @param settings the session settings (loaded from cse_server.cfg)
     * @throws ConfigError       if the configuration is invalid
     * @throws FieldConvertError if a configuration field cannot be converted
     */
    public CseBdServer(SessionSettings settings) throws ConfigError, FieldConvertError {
        TradingBdApplication application = new TradingBdApplication("CSE-BD");
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();

        acceptor = new SocketAcceptor(application, messageStoreFactory, settings,
                logFactory, messageFactory);
    }

    /**
     * Starts the CSE-BD FIX server and begins accepting connections.
     *
     * @throws RuntimeError if the acceptor fails to start
     * @throws ConfigError  if the configuration is invalid
     */
    public void start() throws RuntimeError, ConfigError {
        acceptor.start();
        log.info("CSE-BD FIX server started");
    }

    /**
     * Stops the CSE-BD FIX server and closes all active sessions.
     */
    public void stop() {
        acceptor.stop();
        log.info("CSE-BD FIX server stopped");
    }

    /**
     * Creates and starts a CSE-BD server from the provided configuration stream.
     *
     * @param configStream the input stream for the server configuration
     * @return a started {@code CseBdServer} instance
     * @throws Exception if initialization or startup fails
     */
    public static CseBdServer create(InputStream configStream) throws Exception {
        SessionSettings settings = new SessionSettings(configStream);
        CseBdServer server = new CseBdServer(settings);
        server.start();
        return server;
    }
}
