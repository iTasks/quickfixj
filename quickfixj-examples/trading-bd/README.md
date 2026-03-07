# QuickFIX/J Examples - DSE-BD and CSE-BD Trading Sample

This example demonstrates integration with Bangladesh stock exchanges using QuickFIX/J:

- **DSE-BD**: Dhaka Stock Exchange (Dhaka, Bangladesh) — FIX server on port **5001**
- **CSE-BD**: Chittagong Stock Exchange (Chittagong, Bangladesh) — FIX server on port **5002**

The sample covers:
1. **FIX 4.4** order routing (server and client modes) for both exchanges
2. **ITCH** binary protocol for real-time market data streaming
3. **FAST** (FIX Adapted for STreaming) for bandwidth-optimised data encoding

---

## Running the Sample

Build the project first:

```bash
./mvnw package -pl quickfixj-examples/trading-bd -am -DskipTests -Dmaven.javadoc.skip=true -PskipBundlePlugin
```

### DSE-BD FIX Server (port 5001)

```bash
java -jar quickfixj-examples-trading-bd-*-standalone.jar dse-server
```

### DSE-BD FIX Client

In a second terminal:

```bash
java -jar quickfixj-examples-trading-bd-*-standalone.jar dse-client
```

### CSE-BD FIX Server (port 5002)

```bash
java -jar quickfixj-examples-trading-bd-*-standalone.jar cse-server
```

### CSE-BD FIX Client

```bash
java -jar quickfixj-examples-trading-bd-*-standalone.jar cse-client
```

### ITCH Market Data Streaming Demo

Demonstrates parsing ITCH binary messages (system events, add orders, trades):

```bash
java -jar quickfixj-examples-trading-bd-*-standalone.jar itch-demo
```

### FAST Encoding/Decoding Demo

Demonstrates FAST (FIX Adapted for STreaming) compression for market data:

```bash
java -jar quickfixj-examples-trading-bd-*-standalone.jar fast-demo
```

---

## Configuration

Custom configuration files can be supplied as a second argument:

```bash
java -jar ... dse-server /path/to/my_dse_server.cfg
java -jar ... dse-client /path/to/my_dse_client.cfg
```

Default configuration files are bundled in the JAR:

| File | Description |
|------|-------------|
| `dse_server.cfg` | DSE-BD acceptor (FIX 4.4, port 5001) |
| `dse_client.cfg` | DSE-BD initiator (connects to port 5001) |
| `cse_server.cfg` | CSE-BD acceptor (FIX 4.4, port 5002) |
| `cse_client.cfg` | CSE-BD initiator (connects to port 5002) |

---

## Architecture

```
quickfix.examples.trading.bd
├── TradingBdApplication   – shared FIX 4.4 application handler
├── TradingBdMain          – entry point / mode dispatcher
├── dse/
│   ├── DseBdServer        – DSE-BD FIX acceptor (port 5001)
│   └── DseBdClient        – DSE-BD FIX initiator
├── cse/
│   ├── CseBdServer        – CSE-BD FIX acceptor (port 5002)
│   └── CseBdClient        – CSE-BD FIX initiator
├── itch/
│   ├── ItchMessage        – base class for ITCH binary messages
│   ├── ItchMessageType    – ITCH message type constants
│   ├── SystemEventMessage – system state notifications
│   ├── AddOrderMessage    – new order entry events
│   ├── TradeMessage       – trade execution events
│   └── ItchMarketDataConsumer – parses and tracks ITCH feed
└── fast/
    ├── FastEncoder        – FAST stop-bit encoding
    └── FastDecoder        – FAST stop-bit decoding
```

---

## Protocols

### FIX 4.4 (Order Routing)
Standard FIX protocol messages are used for order routing. The `TradingBdApplication`
handles `NewOrderSingle (D)` messages on the server side, and `ExecutionReport (8)`
messages on the client side.

### ITCH (Market Data Streaming)
ITCH is a one-directional binary streaming protocol. The `ItchMarketDataConsumer`
parses the following message types from DSE-BD and CSE-BD:

| Type | Name | Description |
|------|------|-------------|
| `S` | System Event | Market session state changes |
| `A` | Add Order | New visible order added to book |
| `E` | Order Executed | Order executed in whole or part |
| `X` | Order Cancel | Partial order cancellation |
| `D` | Order Delete | Full order cancellation |
| `P` | Trade | Non-cross trade execution |

### FAST (FIX Adapted for STreaming)
FAST uses stop-bit encoding and presence maps (PMAP) to compress repetitive
financial data. The `FastEncoder`/`FastDecoder` pair supports three message templates:

| Template ID | Name | Fields |
|-------------|------|--------|
| 1 | Market Data Update | symbol, price, quantity |
| 2 | Order Event | orderReference, side, symbol, price, quantity |
| 3 | Trade Event | matchNumber, symbol, price, quantity |
