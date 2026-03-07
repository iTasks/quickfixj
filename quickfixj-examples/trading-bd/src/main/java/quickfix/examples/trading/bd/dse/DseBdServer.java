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
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.examples.trading.bd.TradingBdApplication;

import java.io.InputStream;

/**
 * DSE-BD (Dhaka Stock Exchange, Bangladesh) FIX protocol server.
 *
 * <p>This server acts as a FIX acceptor for the Dhaka Stock Exchange, accepting
 * incoming FIX 4.4 sessions from trading clients. It processes new order requests,
 * cancellations, and order status queries.
 *
 * <p>DSE-BD server listens on port 5001 by default (configurable via {@code dse_server.cfg}).
 *
 * <p>Usage: run with {@code dse_server.cfg} as the configuration file:
 * <pre>
 *   DseBdServer server = new DseBdServer(settings);
 *   server.start();
 * </pre>
 */
public class DseBdServer {

    private static final Logger log = LoggerFactory.getLogger(DseBdServer.class);

    private final SocketAcceptor acceptor;

    /**
     * Constructs a DSE-BD FIX server with the given session settings.
     *
     * @param settings the session settings (loaded from dse_server.cfg)
     * @throws ConfigError       if the configuration is invalid
     * @throws FieldConvertError if a configuration field cannot be converted
     */
    public DseBdServer(SessionSettings settings) throws ConfigError, FieldConvertError {
        TradingBdApplication application = new TradingBdApplication("DSE-BD");
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();

        acceptor = new SocketAcceptor(application, messageStoreFactory, settings,
                logFactory, messageFactory);
    }

    /**
     * Starts the DSE-BD FIX server and begins accepting connections.
     *
     * @throws RuntimeError if the acceptor fails to start
     * @throws ConfigError  if the configuration is invalid
     */
    public void start() throws RuntimeError, ConfigError {
        acceptor.start();
        log.info("DSE-BD FIX server started");
    }

    /**
     * Stops the DSE-BD FIX server and closes all active sessions.
     */
    public void stop() {
        acceptor.stop();
        log.info("DSE-BD FIX server stopped");
    }

    /**
     * Creates and starts a DSE-BD server from the provided configuration stream.
     *
     * @param configStream the input stream for the server configuration
     * @return a started {@code DseBdServer} instance
     * @throws Exception if initialization or startup fails
     */
    public static DseBdServer create(InputStream configStream) throws Exception {
        SessionSettings settings = new SessionSettings(configStream);
        DseBdServer server = new DseBdServer(settings);
        server.start();
        return server;
    }
}
