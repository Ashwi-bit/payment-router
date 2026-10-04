# Payment Router

A mini payment orchestration service that sits between a merchant and 3 payment gateways. Routes payments by **real-time success rate (EMA)**, falls back automatically on failure, and guarantees **idempotency** — the same request never charges twice.

**Live demo:** https://payment-router.onrender.com *(free tier — first request may take ~30s to wake)*

---

## Why

Merchants lose revenue when a single gateway fails or degrades. Naive retries cause double charges. This project shows how real payment orchestration (like [Juspay's Hyperswitch]([https://github.com/juspay/hyperswitch](https://payment-router-jv3x.onrender.com/health))) works in miniature.

---

## Results

Simulated **1,000 payments** across 3 mock gateways (10% / 30% / 50% independent failure rates):

| Strategy | Success | Success % | Total Attempts |
|---|---|---|---|
| Random routing (single attempt) | 705 | **70.5%** | 1000 |
| Smart routing (EMA + fallback) | 982 | **98.2%** | 1150 |

**Improvement: +27.7 percentage points** (theoretical ceiling: 98.5%)

The router learned to prefer `gw-a` 91.5% of the time — its final EMA (0.8934) converged near gw-a's true success rate (0.90). A control test with all gateways equal (30% each) showed only ~90% success, confirming the gain comes from gateway differentiation, not just retries.

---

## Architecture

```
Merchant
   │  POST /payments {amount, currency, idempotencyKey}
   ▼
┌──────────────────────────────────────────────────────┐
│  PaymentController → PaymentService                  │
│                                                      │
│   1. Idempotency check (unique key in DB)            │
│   2. Rank gateways by EMA                            │
│   3. Try #1 → #2 → #3 (fallback chain)               │
│   4. Update EMA per attempt                          │
│   5. Log to gateway_events, persist to payments      │
└──────────────────────────────────────────────────────┘
   │
   ▼
┌────────────┐  ┌────────────┐  ┌────────────┐
│   gw-a     │  │   gw-b     │  │   gw-c     │
│ 10% fail   │  │ 30% fail   │  │ 50% fail   │
│ 80ms       │  │ 150ms      │  │ 250ms      │
└────────────┘  └────────────┘  └────────────┘
```

**EMA formula:** `score_new = α · outcome + (1 − α) · score_old` where `outcome ∈ {0, 1}` and `α = 0.3`. Cold-start prior: `5 successes / 5 failures = 0.5`.

---

## API

### POST /payments

```bash
curl -X POST https://payment-router.onrender.com/payments \
  -H "Content-Type: application/json" \
  -d '{"amount":10000,"currency":"INR","idempotencyKey":"order-12345"}'
```

Response:
```json
{
  "idempotencyKey": "order-12345",
  "status": "SUCCESS",
  "gateway": "gw-a",
  "attempts": 1,
  "attemptedGateways": ["gw-a"],
  "transactionId": "txn_...",
  "duplicate": false
}
```

**Send the same `idempotencyKey` again → same response, `"duplicate": true`, no new charge.**

### GET /stats

```bash
curl https://payment-router.onrender.com/stats
```

Per-gateway success rates, live EMA, and overall stats.

### GET /gateways

Lists all gateways with their killed status.

### POST /gateways/{id}/kill

```bash
curl -X POST https://payment-router.onrender.com/gateways/gw-a/kill
```

Kills a gateway — traffic immediately reroutes to the next-best.

---

## Tech Stack

- **Java 21**, **Spring Boot 3.4**
- **H2** (in-memory for demo; file mode for local)
- **JdbcTemplate** (raw SQL for idempotency via PRIMARY KEY)
- **Docker** (deployed on Render)

---

## Running Locally

```bash
git clone https://github.com/YOUR_USERNAME/payment-router.git
cd payment-router
./mvnw spring-boot:run
```

Then hit `http://localhost:8080/health`.

### Run the simulation

```bash
./mvnw compile
java -cp target/classes com.ashwini.router.simulation.Simulation
```

---

## Key Learnings

- **Idempotency via DB constraint:** `INSERT` + PRIMARY KEY on `idempotency_key` prevents double charges without distributed locks.
- **H2 vs Postgres:** `ON CONFLICT DO NOTHING` is Postgres-only; switched to check-then-insert with `DuplicateKeyException` fallback.
- **EMA vs sliding window:** EMA reacts faster to degradation, avoids cold-start cliffs with priors.
- **Fallback ≠ retry:** Retrying the same dead gateway is wasteful. Trying the *next-best* is what saves payments.

---

## What's Next

- Latency-aware routing (weight EMA by inverse latency)
- Circuit breaker per gateway
- Persistent storage (Postgres) for production
- Dashboard visualizing EMA in real-time

---

## License

MIT
