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

package quickfix.examples.trading.bd.fast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FAST (FIX Adapted for STreaming) decoder for Bangladesh stock exchanges.
 *
 * <p>Decodes FAST-encoded binary messages back into their field values.
 * This decoder supports the same template IDs as {@link FastEncoder}:
 * <ul>
 *   <li>Template 1 ({@link FastEncoder#TEMPLATE_MARKET_DATA}): market data updates</li>
 *   <li>Template 2 ({@link FastEncoder#TEMPLATE_ORDER_EVENT}): order events</li>
 *   <li>Template 3 ({@link FastEncoder#TEMPLATE_TRADE_EVENT}): trade events</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 *   FastDecoder decoder = new FastDecoder();
 *   Map&lt;String, Object&gt; fields = decoder.decode(encodedBytes);
 *   String symbol = (String) fields.get("symbol");
 * </pre>
 */
public class FastDecoder {

    private static final Logger log = LoggerFactory.getLogger(FastDecoder.class);

    private static final int STOP_BIT = 0x80;
    private static final int DATA_BITS = 0x7F;

    /**
     * Decodes a FAST-encoded message and returns its fields as a map.
     *
     * @param encoded the FAST-encoded byte array
     * @return a map of field names to their decoded values, or an empty map on error
     */
    public Map<String, Object> decode(byte[] encoded) {
        Map<String, Object> fields = new HashMap<>();
        if (encoded == null || encoded.length == 0) {
            return fields;
        }
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(encoded);
            long templateId = decodeUnsignedInt(input);
            fields.put("templateId", templateId);

            // Skip PMAP bytes (read until stop bit is found)
            readPmap(input);

            if (templateId == FastEncoder.TEMPLATE_MARKET_DATA) {
                fields.put("symbol", decodeAsciiString(input));
                fields.put("price", decodeUnsignedInt(input));
                fields.put("quantity", decodeUnsignedInt(input));
            } else if (templateId == FastEncoder.TEMPLATE_ORDER_EVENT) {
                fields.put("orderReference", decodeUnsignedInt(input));
                fields.put("side", decodeAsciiString(input));
                fields.put("symbol", decodeAsciiString(input));
                fields.put("price", decodeUnsignedInt(input));
                fields.put("quantity", decodeUnsignedInt(input));
            } else if (templateId == FastEncoder.TEMPLATE_TRADE_EVENT) {
                fields.put("matchNumber", decodeUnsignedInt(input));
                fields.put("symbol", decodeAsciiString(input));
                fields.put("price", decodeUnsignedInt(input));
                fields.put("quantity", decodeUnsignedInt(input));
            } else {
                log.warn("Unknown FAST template ID: {}", templateId);
            }
        } catch (IOException e) {
            log.error("Error decoding FAST message", e);
        }
        return fields;
    }

    /**
     * Decodes an unsigned integer from the input stream using FAST stop-bit decoding.
     *
     * @param input the input stream
     * @return the decoded unsigned integer
     * @throws IOException if an I/O error occurs or the stream ends unexpectedly
     */
    public long decodeUnsignedInt(ByteArrayInputStream input) throws IOException {
        long value = 0;
        int b;
        do {
            b = input.read();
            if (b == -1) {
                throw new IOException("Unexpected end of FAST stream while reading unsigned int");
            }
            value = (value << 7) | (b & DATA_BITS);
        } while ((b & STOP_BIT) == 0);
        return value;
    }

    /**
     * Decodes a signed integer from the input stream using FAST stop-bit decoding.
     *
     * @param input the input stream
     * @return the decoded signed integer
     * @throws IOException if an I/O error occurs or the stream ends unexpectedly
     */
    public long decodeSignedInt(ByteArrayInputStream input) throws IOException {
        long value = 0;
        int b;
        int shift = 0;
        do {
            b = input.read();
            if (b == -1) {
                throw new IOException("Unexpected end of FAST stream while reading signed int");
            }
            value |= ((long) (b & DATA_BITS)) << shift;
            shift += 7;
        } while ((b & STOP_BIT) == 0);
        // Sign-extend if the high bit of the last data byte was set
        if ((b & 0x40) != 0) {
            value |= -(1L << shift);
        }
        return value;
    }

    /**
     * Decodes an ASCII string from the input stream using FAST stop-bit decoding.
     *
     * @param input the input stream
     * @return the decoded ASCII string
     * @throws IOException if an I/O error occurs or the stream ends unexpectedly
     */
    public String decodeAsciiString(ByteArrayInputStream input) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        do {
            b = input.read();
            if (b == -1) {
                throw new IOException("Unexpected end of FAST stream while reading ASCII string");
            }
            sb.append((char) (b & DATA_BITS));
        } while ((b & STOP_BIT) == 0);
        // Remove the null-character placeholder used for empty strings
        String result = sb.toString();
        if (result.equals("\0")) {
            return "";
        }
        return result;
    }

    /**
     * Reads and discards the PMAP bytes from the input stream.
     *
     * @param input the input stream
     * @throws IOException if an I/O error occurs
     */
    private void readPmap(ByteArrayInputStream input) throws IOException {
        int b;
        do {
            b = input.read();
            if (b == -1) {
                throw new IOException("Unexpected end of FAST stream while reading PMAP");
            }
        } while ((b & STOP_BIT) == 0);
    }
}
