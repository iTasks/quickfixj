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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the FAST encoder and decoder.
 */
public class FastEncoderDecoderTest {

    private final FastEncoder encoder = new FastEncoder();
    private final FastDecoder decoder = new FastDecoder();

    @Test
    public void testEncodeDecodeUnsignedInt() throws IOException {
        long[] testValues = {0, 1, 127, 128, 255, 256, 16383, 16384, Integer.MAX_VALUE, Long.MAX_VALUE / 2};
        for (long value : testValues) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            encoder.encodeUnsignedInt(value, out);
            long decoded = decoder.decodeUnsignedInt(new ByteArrayInputStream(out.toByteArray()));
            assertEquals(value, decoded, "Failed for value: " + value);
        }
    }

    @Test
    public void testEncodeDecodeSignedInt() throws IOException {
        long[] testValues = {0, 1, -1, 63, 64, -64, -65, 127, -128, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (long value : testValues) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            encoder.encodeSignedInt(value, out);
            long decoded = decoder.decodeSignedInt(new ByteArrayInputStream(out.toByteArray()));
            assertEquals(value, decoded, "Failed for value: " + value);
        }
    }

    @Test
    public void testEncodeDecodeAsciiString() throws IOException {
        String[] testStrings = {"A", "SQURPHARMA", "BSRMSTEEL", "ACIFORMULA"};
        for (String str : testStrings) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            encoder.encodeAsciiString(str, out);
            String decoded = decoder.decodeAsciiString(new ByteArrayInputStream(out.toByteArray()));
            assertEquals(str, decoded, "Failed for string: " + str);
        }
    }

    @Test
    public void testEncodeDecodeEmptyString() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encoder.encodeAsciiString("", out);
        String decoded = decoder.decodeAsciiString(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("", decoded);
    }

    @Test
    public void testEncodeMarketDataUpdate() {
        byte[] encoded = encoder.encodeMarketDataUpdate("SQURPHARMA", 2105000L, 200);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        Map<String, Object> decoded = decoder.decode(encoded);
        assertEquals((long) FastEncoder.TEMPLATE_MARKET_DATA, decoded.get("templateId"));
        assertEquals("SQURPHARMA", decoded.get("symbol"));
        assertEquals(2105000L, decoded.get("price"));
        assertEquals(200L, decoded.get("quantity"));
    }

    @Test
    public void testEncodeOrderEvent() {
        byte[] encoded = encoder.encodeOrderEvent(100001L, 'B', "BSRMSTEEL", 855000L, 500);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        Map<String, Object> decoded = decoder.decode(encoded);
        assertEquals((long) FastEncoder.TEMPLATE_ORDER_EVENT, decoded.get("templateId"));
        assertEquals(100001L, decoded.get("orderReference"));
        assertEquals("B", decoded.get("side"));
        assertEquals("BSRMSTEEL", decoded.get("symbol"));
        assertEquals(855000L, decoded.get("price"));
        assertEquals(500L, decoded.get("quantity"));
    }

    @Test
    public void testEncodeTradeEvent() {
        byte[] encoded = encoder.encodeTradeEvent(500001L, "SQURPHARMA", 2105000L, 150);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        Map<String, Object> decoded = decoder.decode(encoded);
        assertEquals((long) FastEncoder.TEMPLATE_TRADE_EVENT, decoded.get("templateId"));
        assertEquals(500001L, decoded.get("matchNumber"));
        assertEquals("SQURPHARMA", decoded.get("symbol"));
        assertEquals(2105000L, decoded.get("price"));
        assertEquals(150L, decoded.get("quantity"));
    }

    @Test
    public void testEncodedSizeIsSmaller() {
        // FAST encoding should be more compact than the equivalent string representation
        String marketDataString = "symbol=SQURPHARMA,price=2105000,qty=200";
        byte[] encoded = encoder.encodeMarketDataUpdate("SQURPHARMA", 2105000L, 200);
        assertTrue(encoded.length < marketDataString.length(),
                "FAST encoded size (" + encoded.length + ") should be smaller than string representation ("
                        + marketDataString.length() + ")");
    }

    @Test
    public void testDecodeEmptyBytes() {
        Map<String, Object> decoded = decoder.decode(new byte[0]);
        assertNotNull(decoded);
        assertTrue(decoded.isEmpty());
    }

    @Test
    public void testDecodeNullBytes() {
        Map<String, Object> decoded = decoder.decode(null);
        assertNotNull(decoded);
        assertTrue(decoded.isEmpty());
    }

    @Test
    public void testEncodePmap() throws IOException {
        boolean[] allPresent = {true, true, true};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encoder.encodePmap(allPresent, out);
        assertTrue(out.size() > 0);

        // Last byte should have stop bit set
        byte[] bytes = out.toByteArray();
        assertTrue((bytes[bytes.length - 1] & 0x80) != 0, "Stop bit should be set on last PMAP byte");
    }
}
