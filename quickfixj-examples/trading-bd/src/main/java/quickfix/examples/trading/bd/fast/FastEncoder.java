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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FAST (FIX Adapted for STreaming) encoder for Bangladesh stock exchanges.
 *
 * <p>FAST is a binary encoding protocol designed to optimize the bandwidth used for
 * transmitting financial data, particularly FIX-based market data. It uses stop-bit
 * encoding and a presence map (PMAP) to compress repetitive and predictable fields.
 *
 * <p>This encoder provides basic FAST encoding operations:
 * <ul>
 *   <li>Stop-bit encoding for unsigned integers</li>
 *   <li>Stop-bit encoding for signed integers</li>
 *   <li>Stop-bit encoding for ASCII strings</li>
 *   <li>Presence map (PMAP) encoding</li>
 * </ul>
 *
 * <p>Supported by DSE-BD (Dhaka Stock Exchange) and CSE-BD (Chittagong Stock Exchange)
 * for streaming market data with reduced bandwidth consumption.
 *
 * <p>Usage example:
 * <pre>
 *   FastEncoder encoder = new FastEncoder();
 *   // Encode a market data update
 *   byte[] encoded = encoder.encodeMarketDataUpdate("SQURPHARMA", 102500L, 1000);
 * </pre>
 */
public class FastEncoder {

    private static final Logger log = LoggerFactory.getLogger(FastEncoder.class);

    /** The stop bit mask - the most significant bit of the last byte in a field. */
    private static final int STOP_BIT = 0x80;

    /** The data bits mask - the 7 data bits in each FAST byte. */
    private static final int DATA_BITS = 0x7F;

    /** Template ID for market data updates. */
    public static final int TEMPLATE_MARKET_DATA = 1;

    /** Template ID for order events. */
    public static final int TEMPLATE_ORDER_EVENT = 2;

    /** Template ID for trade events. */
    public static final int TEMPLATE_TRADE_EVENT = 3;

    /**
     * Encodes an unsigned integer using FAST stop-bit encoding.
     *
     * <p>The value is encoded in groups of 7 bits, with the most significant bit
     * of the last byte set to 1 (stop bit).
     *
     * @param value  the unsigned integer value to encode
     * @param output the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void encodeUnsignedInt(long value, ByteArrayOutputStream output) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        byte[] temp = new byte[10];
        int index = temp.length - 1;
        temp[index] = (byte) ((value & DATA_BITS) | STOP_BIT);
        value >>= 7;
        while (value > 0) {
            index--;
            temp[index] = (byte) (value & DATA_BITS);
            value >>= 7;
        }
        output.write(temp, index, temp.length - index);
    }

    /**
     * Encodes a signed integer using FAST stop-bit encoding.
     *
     * <p>Negative values use sign extension to fill the high bits.
     *
     * @param value  the signed integer value to encode
     * @param output the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void encodeSignedInt(long value, ByteArrayOutputStream output) throws IOException {
        boolean more = true;
        while (more) {
            byte b = (byte) (value & DATA_BITS);
            value >>= 7; // arithmetic right shift preserves sign
            // This is the last byte if the remaining value can be correctly sign-extended
            // from the current 7-bit group (i.e., no more significant bits are needed).
            if ((value == 0 && (b & 0x40) == 0) || (value == -1 && (b & 0x40) != 0)) {
                output.write(b | STOP_BIT);
                more = false;
            } else {
                output.write(b);
            }
        }
    }

    /**
     * Encodes an ASCII string using FAST stop-bit encoding.
     *
     * <p>Each character is encoded as a 7-bit value; the stop bit is set
     * on the last character of the string.
     *
     * @param value  the string to encode (ASCII characters only)
     * @param output the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void encodeAsciiString(String value, ByteArrayOutputStream output) throws IOException {
        if (value == null || value.isEmpty()) {
            // Null/empty string is encoded as a single zero byte with stop bit
            output.write(STOP_BIT);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < bytes.length - 1; i++) {
            output.write(bytes[i] & DATA_BITS);
        }
        output.write(bytes[bytes.length - 1] | STOP_BIT);
    }

    /**
     * Encodes a presence map (PMAP) as a byte array.
     *
     * <p>The PMAP indicates which optional fields are present in the message.
     * Each bit corresponds to a field; 1 means the field is present, 0 means absent.
     * The stop bit is set on the last byte.
     *
     * @param presentFields boolean array where true means the field is present
     * @param output        the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void encodePmap(boolean[] presentFields, ByteArrayOutputStream output) throws IOException {
        int byteCount = (presentFields.length + 6) / 7; // 7 bits per byte
        for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
            int pmapByte = 0;
            for (int bitIndex = 0; bitIndex < 7; bitIndex++) {
                int fieldIndex = byteIndex * 7 + bitIndex;
                if (fieldIndex < presentFields.length && presentFields[fieldIndex]) {
                    pmapByte |= (1 << (6 - bitIndex));
                }
            }
            if (byteIndex == byteCount - 1) {
                pmapByte |= STOP_BIT; // Set stop bit on last byte
            }
            output.write(pmapByte);
        }
    }

    /**
     * Encodes a market data update message using FAST encoding.
     *
     * <p>The encoded message contains: template ID, stock symbol, price, and quantity.
     *
     * @param stockSymbol the stock symbol (up to 8 characters)
     * @param price       the price (with 4 implicit decimal places, e.g. 102500 = 10.2500)
     * @param quantity    the quantity
     * @return the FAST-encoded byte array
     */
    public byte[] encodeMarketDataUpdate(String stockSymbol, long price, int quantity) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            // Encode template ID
            encodeUnsignedInt(TEMPLATE_MARKET_DATA, output);
            // Encode PMAP: all fields present
            encodePmap(new boolean[]{true, true, true}, output);
            // Encode fields
            encodeAsciiString(stockSymbol, output);
            encodeUnsignedInt(price, output);
            encodeUnsignedInt(quantity, output);
        } catch (IOException e) {
            log.error("Error encoding market data update", e);
        }
        return output.toByteArray();
    }

    /**
     * Encodes an order event message using FAST encoding.
     *
     * <p>The encoded message contains: template ID, order reference, side,
     * stock symbol, price, and quantity.
     *
     * @param orderReference the unique order reference number
     * @param buySell        'B' for buy or 'S' for sell
     * @param stockSymbol    the stock symbol
     * @param price          the order price (4 implicit decimal places)
     * @param quantity       the order quantity
     * @return the FAST-encoded byte array
     */
    public byte[] encodeOrderEvent(long orderReference, char buySell,
            String stockSymbol, long price, int quantity) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            encodeUnsignedInt(TEMPLATE_ORDER_EVENT, output);
            encodePmap(new boolean[]{true, true, true, true, true}, output);
            encodeUnsignedInt(orderReference, output);
            encodeAsciiString(String.valueOf(buySell), output);
            encodeAsciiString(stockSymbol, output);
            encodeUnsignedInt(price, output);
            encodeUnsignedInt(quantity, output);
        } catch (IOException e) {
            log.error("Error encoding order event", e);
        }
        return output.toByteArray();
    }

    /**
     * Encodes a trade event message using FAST encoding.
     *
     * <p>The encoded message contains: template ID, match number, stock symbol,
     * execution price, and executed quantity.
     *
     * @param matchNumber    the unique trade match number
     * @param stockSymbol    the stock symbol
     * @param executionPrice the execution price (4 implicit decimal places)
     * @param executedQty    the number of shares executed
     * @return the FAST-encoded byte array
     */
    public byte[] encodeTradeEvent(long matchNumber, String stockSymbol,
            long executionPrice, int executedQty) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            encodeUnsignedInt(TEMPLATE_TRADE_EVENT, output);
            encodePmap(new boolean[]{true, true, true, true}, output);
            encodeUnsignedInt(matchNumber, output);
            encodeAsciiString(stockSymbol, output);
            encodeUnsignedInt(executionPrice, output);
            encodeUnsignedInt(executedQty, output);
        } catch (IOException e) {
            log.error("Error encoding trade event", e);
        }
        return output.toByteArray();
    }
}
