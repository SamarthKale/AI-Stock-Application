# AI Crypto Predictor — Technical & Product Outline

> Native Android app for cryptocurrency market data, AI-driven price predictions, an AI assistant, and a global exchanges map. Built in phases (1 → 6), doubling as coverage for the CE mobile-computing lab practicals.
> Source: project `CLAUDE.md` (the authoritative phase-by-phase spec). Status reflects what is documented as built.

---

## 1. Product Overview

### 1.1 What it is
A mobile app where a user can:
- Browse live crypto prices (CoinGecko) and search any coin.
- Keep a **watchlist** that syncs across devices.
- Track a **portfolio** of holdings with gain/loss.
- See **AI predictions** (direction Up/Down/Flat + confidence) per coin, with a "not investment advice" disclaimer.
- Chat with an **"Ask AI"** assistant (Gemini, via backend proxy).
- Explore a **Global Exchanges Map** (MapLibre) with live 24h volume / trust score, and a "Locate me" distance-to-nearest-exchange feature.
- Hear a **spoken market briefing** (TTS) of watchlist movers and top predictions.
- Receive **push alerts** (FCM) when a watched coin moves sharply or a prediction crosses a confidence threshold; tapping deep-links to that coin.

### 1.2 Project history
Originally an "AI Stock Predictor" (NSE/BSE). Pivoted to crypto during Phase 4. Architecture carried over unchanged; only the domain (stock → coin) and market-data provider changed. `applicationId` = `com.stockpredictor.app` and backend package `com.stockpredictor.backend` were **deliberately kept** because Firebase's `google-services.json` is registered against that package.

### 1.3 Target users & value
Retail crypto users who want a clean, calm dashboard with data-backed (but honestly-caveated) signals. The model's real macro-F1 is ~0.39 and the on-device model is weaker (~0.30); the product is positioned as informational, never advice.

---

## 2. System Architecture

### 2.1 Target end state
```
Android App (Kotlin)
   ↓                       ↘
Spring Boot API (Java)      Firebase (Auth / Firestore / FCM)
   ↓
 ┌──────────────┬───────────────┬──────────────────────┐
PostgreSQL     Redis        Python AI Service (FastAPI)
Users          Rate limits   Feature engineering
Portfolio                    XGBoost (LSTM stub)
Watchlist                    POST /predict
```

### 2.2 Data flows as actually implemented
| Feature | Flow |
|---|---|
| Market data | Android → CoinGecko directly (Retrofit) → `CoinRepository` (SQLite cache-then-network) |
| Predictions | Android → Spring Boot `/api/predictions/{coinId}` → FastAPI `/predict` → back (fallback: `CachedPredictionDao`) |
| Chatbot | Android → Spring Boot `/api/chatbot/message` → Gemini (key only on backend) |
| Sync | Android SQLite ↔ Firestore (last-write-wins via server timestamp) |
| Alerts | Backend `AlertRuleService` (scheduled) → CoinGecko + Firestore watchlists → FCM → Android |
| On-device ML | Bundled TFLite model, fully offline |

Deliberate decision: market data is **not** proxied through the backend yet. The `CoinDataSource` interface is the seam where a backend-proxied implementation can later replace `CoinGeckoDirectDataSource` without touching repositories or ViewModels.

### 2.3 Core rule
Kotlin/Java never contains ML logic. Java = orchestration; Python = all AI/ML (including feature engineering).

---

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Kotlin, Jetpack Compose, Gradle Kotlin DSL, single-Activity + Navigation Compose |
| Local DB | Raw SQLite (`SQLiteOpenHelper` + DAOs; no Room) |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Cloud | Firebase Auth, Firestore, FCM, Crashlytics |
| Backend | Java + Spring Boot, Spring Security (Firebase ID-token filter), Flyway |
| Backend DB / cache | PostgreSQL; Redis (distributed rate limiting) |
| AI service | Python + FastAPI; XGBoost (trained), LSTM/GRU stub; joblib artifact |
| On-device ML | TensorFlow Lite (`momentum_model.tflite`) |
| Chatbot | Gemini via `GeminiChatbotClient` behind `ChatbotController` |
| Maps | MapLibre Native Android + OpenFreeMap (no API key / billing) |
| Location | `play-services-location` (one-shot `getCurrentLocation`) |
| Market data | CoinGecko Demo tier (~30 calls/min) |
| Deployment | Docker / docker-compose (nginx, backend, ai-service, Postgres, Redis); host TBD |

---

## 4. Android App

### 4.1 Package layout (`com.stockpredictor.app`)
```
MainActivity.kt
navigation/        AppNavHost, Destinations (typed routes)
ui/theme/          ClayTheme, ClayColor, ClayShapes (design tokens)
ui/components/     Clay* library, PriceChangeChip, PredictionConfidenceBar,
                   CoinListTile, CoinRankTile, Loading/Empty/ErrorState,
                   ChatbotFab, PredictionDisclaimer
ui/screens/        onboarding, auth, home, cryptodetail, watchlist, portfolio,
                   search, predictions, notifications, settings, chatbot,
                   exchangemap, privacy policy
ui/state/          UiState, DebugAwareState (debugAwareUiState overloads)
model/             Coin, Prediction, PortfolioHolding, WatchlistItem, NotificationItem
data/local/        AppDatabaseHelper, DbContract, dao/*, entity/*
data/remote/api/   CoinGeckoApiService, RetrofitClient, CoinDataSource,
                   CoinGeckoDirectDataSource, ChatbotApiService, dto/, mapper/
data/remote/firebase/ FirebaseAuthRepository, FirestoreSyncRepository, FcmTokenManager
data/repository/   CoinRepository, CoinSearchRepository, PredictionRepository,
                   ExchangeRepository, ChatbotRepository
ml/                OnDeviceMomentumClassifier
audio/             MarketBriefingSpeaker (TTS)
service/           StockPredictorFcmService
net/               BackendUrlResolver (debug/release source-set variants)
mock/              Remaining mock data (Portfolio; previews)
```

### 4.2 Screens
Onboarding · Login · Signup · Forgot Password · Home · Crypto Detail · Watchlist · Portfolio · Search · Predictions · Notifications · Settings · Chatbot · Exchange Map · Privacy Policy.
Bottom nav (5): Home, Watchlist, Predictions, Portfolio, Settings. A floating "Ask AI" bubble appears on all authenticated destinations except the chatbot itself.

### 4.3 Key patterns
- **ViewModel/UiState seam:** every screen has a ViewModel exposing `StateFlow<UiState>` (`Loading / Success / Empty / Error`); composables never read data sources directly. This is what let Phases 2, 2.5, 4, and 5 swap data sources without changing UI.
- **Design system (claymorphism):** off-white `#F4F3F1` background, white clay cards with dual shadows, 20–28dp radii, mint `#4CAF8C` for gains, coral `#E2685A` for losses, accent `#6C63FF`. All tokens live in `ui/theme/`; no hardcoded hex/dp in screens.
- **DTO isolation:** DTO → model mappers keep API types out of ViewModels/composables.
- **Coroutines hygiene:** `CancellationException` is always rethrown; `refreshJob` cancelled before relaunch; `Mutex` per repository operation.

### 4.4 Local persistence (raw SQLite)
Tables: `watchlist` (coin_id, symbol, name, image_url, added_at, sort_order, updated_at), `recent_searches`, `settings` (key-value), `cached_predictions`, `cached_coins`, `cached_price_history`, `chat_messages`.
- One DAO per table, `suspend` functions on `Dispatchers.IO`, no SQL outside DAOs.
- Atomic watchlist reorder via `updateSortOrders(orderedIds)` in a transaction.
- `DB_VERSION` 2→3 used real `ALTER TABLE` migration when coin columns were added.

### 4.5 Market data & caching (CoinGecko)
Endpoints used: `/coins/markets`, `/coins/{id}`, `/coins/{id}/market_chart`, `/search`, `/search/trending`. (`/coins/top_gainers_losers` is 401 on Demo tier → gainers/losers are derived client-side.)

| Data | TTL |
|---|---|
| Markets list (general and `ids=` batched) | 60 s |
| Coin detail | 120 s |
| Price history | 6 h |
| Trending (in-memory only) | 5 min |

On network failure the stale cache is served with an `isStale` flag surfaced in the UI. Exceptions: `CoinNotFoundException` (404 → Empty), `CoinDataUnavailableException` (network/429/5xx → Error + retry). Search is debounced 300 ms; an empty result is `Empty`, not `Error`.

### 4.6 Auth & sync (Firebase)
- Email/password sign-up, sign-in, password reset; Firebase error codes mapped to inline `ClayTextField` errors.
- Firestore: `users/{uid}/watchlist/{coinId}` and portfolio; `updated_at` is a **server timestamp** (last-write-wins, avoids client clock skew). Real-time snapshot listener attached on login, removed on sign-out. Firestore offline persistence provides write queueing.
- FCM token stored at `users/{uid}/fcmToken`; `POST_NOTIFICATIONS` requested after first login.

### 4.7 AI features on Android
- **Predictions:** `PredictionRepository` → backend; `CachedPredictionDao` fallback; shown on Predictions tab and Crypto Detail with `PredictionConfidenceBar` + `PredictionDisclaimer`.
- **On-device classifier:** `OnDeviceMomentumClassifier` runs `momentum_model.tflite` offline; shipped honestly as a weak, best-effort local signal alongside (never replacing) the server prediction.
- **Chatbot:** `ChatbotScreen` (clay message bubbles, typing indicator, retry), history in `ChatMessageDao`, survives restart.
- **Briefing:** `MarketBriefingSpeaker` waits for TTS `onInit`, has `stop()` on dispose, handles empty-watchlist fallback.

### 4.8 Exchange Map
MapLibre `MapView` in `AndroidView` rendering OpenFreeMap "Liberty" style, OSM attribution always shown. `ExchangeLocation` data uses CoinGecko exchange ids and registered-jurisdiction coordinates (no open/closed concept — crypto is 24/7). `ExchangeRepository` joins live `tradeVolume24hBtc` / `trustScore`. Tapping a marker opens `ExchangeDetailSheet`. **Locate me:** runtime location permissions → one-shot fused location (15 s timeout) → "you are here" marker, nearest exchange via haversine, sheet shows `distanceKm`, TTS speaks a one-sentence summary.

---

## 5. Backend (Spring Boot)

### 5.1 Structure (package by feature)
```
com.stockpredictor.backend
  StockPredictorApplication
  config/SecurityConfig        Firebase ID-token OncePerRequestFilter
  user/ watchlist/ portfolio/  Controller + Service each
  prediction/                  PredictionController, PredictionService,
                               PredictionClient, PredictionRateLimiter
  chatbot/                     ChatbotController, ChatbotService,
                               GeminiChatbotClient, ChatbotRateLimiter
  alerts/                      AlertRuleService, AlertEvaluator,
                               CoinGeckoMarketDataClient, FirestoreWatchlistReader,
                               FirebaseMessagingPushSender,
                               AlertCooldownService/Entity/Repository
  common/dto/                  DTOs matching Android model/
```
Config in `application.yml`; Flyway migrations in `db/migration` (`V1__init_schema.sql`: `users`, `watchlist`, `portfolio_holdings`).

### 5.2 Security
- Verifies **Firebase ID tokens** via Admin SDK (no second JWT system). Principal = Firebase `uid`; never trusted from request params.
- `/api/**` protected; health check public. Missing/invalid token → 401 JSON body.
- Other-user resources return 404 (not 403) to avoid confirming existence.
- Validation annotations produce field-level 400s; no raw DB errors leaked.

### 5.3 Endpoints (summary)
| Endpoint | Purpose |
|---|---|
| `GET/POST /api/watchlist`, `DELETE /api/watchlist/{id}`, `PUT /api/watchlist/reorder` | Watchlist CRUD |
| `/api/portfolio` equivalents | Portfolio CRUD |
| `/api/predictions/{coinId}` | Prediction via AI service (rate-limited) |
| `POST /api/chatbot/message` | Gemini proxy `{conversationId, message}` → `{reply, conversationId}` (rate-limited) |

Note: Android's watchlist is keyed by `coin_id`, while the backend watchlist schema is still `symbol`-keyed; this must be reconciled before Android syncs with the backend watchlist.

### 5.4 Alerts
`AlertRuleService` runs on a schedule; rules (prediction-confidence threshold, intraday % move on watchlisted coins) are configured in `application.yml`. A cooldown table prevents duplicate pushes per user/coin/rule. Pushes carry a **data payload with `coinId`** for deep-linking, sent via Firebase Admin SDK.

### 5.5 Redis
`RedisRateLimiter` shared base for chatbot and prediction limiters (distributed rate limiting).

---

## 6. AI Service (FastAPI)

```
ai-service/
  main.py
  api/prediction_router.py      POST /predict
  features/feature_engineering.py   pure indicator functions (MA, RSI, momentum…)
  models/xgboost_model.py, lstm_model.py (stub)
  training/build_dataset.py, train_xgboost.py, train_momentum_tflite.py
```
- **Model:** XGBoost, pooled multi-coin training, **time-based** train/validation split (no shuffling, to avoid look-ahead leakage). Honest result: macro-F1 ≈ 0.39. Artifact loaded once at startup.
- **Response shape:** single confidence + `PredictionDirection` (Up/Down/Flat), plus optional target price and horizon.
- **Validation:** rejects history shorter than the feature window with a clear error.
- **Resilience:** backend bounds AI-service wait time; if the service is down, a graceful fallback is used rather than a 5xx.
- Structured JSON logging.

---

## 7. Data Models (parity between Android and backend)
- `Coin` — id (CoinGecko id, the only safe key), symbol (display only), name, image, currentPrice, marketCap/rank, volume, 24h high/low, 24h change, supply fields, ATH/ATL, sparkline7d, history, description, lastUpdated.
- `Prediction` — symbol/coin, confidence (Float), direction, targetPrice?, horizon, generatedAt.
- `PortfolioHolding` — symbol, quantity, avgBuyPrice, currentPrice, derived `gainLossPercent`.
- `WatchlistItem` — coin_id, symbol, name, image_url, added_at, sort_order.
- `NotificationItem` — id, title, body, timestamp, isRead, related coin.

---

## 8. Security & Privacy
- Secrets only in `local.properties` / backend env — never committed. Documented exception: CoinGecko Demo key compiled into the app (low privilege, client-designed tier).
- Gemini key lives only on the backend.
- `google-services.json` and the release keystore are gitignored/uncommitted; release signing reads `keystore.properties`.
- R8/ProGuard verified on a signed `assembleRelease` (Retrofit/serialization, Firestore, TFLite, MapLibre, location all functional).
- `PrivacyPolicyScreen` (linked from Settings) written against the app's real data flows.
- `PredictionDisclaimer` shown on Crypto Detail's AI card and every Predictions row.
- Debug-only UI (state-preview toggle, test-crash button) gated by `BuildConfig.DEBUG`; verified absent in Release.

---

## 9. Build, Run & Deployment
- **Android:** Gradle Kotlin DSL. `BACKEND_BASE_URL` BuildConfig default `http://10.0.2.2:8080/`; debug-only `BackendUrlResolver` auto-picks emulator (`10.0.2.2`) vs physical device (`127.0.0.1` + `adb reverse tcp:8080 tcp:8080`); Release is a pass-through.
- **Backend stack:** `docker compose up` brings up nginx (TLS) + Spring Boot + FastAPI + PostgreSQL + Redis; Flyway runs migrations. `docker-compose.dev.yml` republishes the backend to localhost. Dockerfiles verified on `linux/amd64` and `linux/arm64`.
- **Infra:** `infra/` Terraform for OCI Always Free (written, never applied). OCI blocked on payment verification, AWS cancelled; final host (college server or VPS) decided at submission. CI/CD on GitHub Actions was planned for ARM64 images.
- **Observability:** Crashlytics with non-PII breadcrumbs; structured JSON logs on backend and AI service.

---

## 10. Phase Status

| Phase | Scope | Status |
|---|---|---|
| 1 | UI-only with mock data | Complete |
| 2 | Raw SQLite persistence | Complete (+ coin migration) |
| 2.5 | Firebase Auth / Firestore sync / FCM | Complete |
| 3 | Spring Boot skeleton | Complete |
| 4 | CoinGecko integration | Steps 1–3 complete |
| 5 | AI/ML service (XGBoost) | Complete |
| 5b | On-device ML + chatbot | Complete |
| 5c | Map, TTS briefing, AI alerts, GPS | Complete |
| 6 | Production hardening | Android + Docker complete; hosting undecided |

### Known gaps / open items
- Device verification outstanding for some Phase 4 criteria (offline stale data, rate-limit behavior, TTL re-entry) — code-reviewed only.
- Physical-device path for `BackendUrlResolver` not tested on real hardware.
- Predictions model is weak (macro-F1 0.39); on-device model weaker (0.30, 0% recall on UP).
- Backend watchlist is `symbol`-keyed vs Android's `coin_id`.
- Price chart is still a Canvas placeholder (no MPAndroidChart/Vico).
- Portfolio remains on mock data; LSTM/GRU is a stub.
- Cosmetic debt: `StockPredictorFcmService`, `StockPredictorApplication`, `stockpredictor.db` naming.
- Final hosting target and CI/CD deployment undecided.

---

## 11. Lab Practical Coverage

| # | Topic | Covered by |
|---|---|---|
| 1 | Basic UI, input controls | Phase 1 |
| 2 | Multi-screen navigation | Phase 1 (bottom nav, back stack) |
| 3 | Local DB | Phase 2 (raw SQLite CRUD) |
| 4 | Firebase data sync | Phase 2.5 |
| 5 | REST APIs | Phase 4 (Retrofit + CoinGecko) |
| 6 | Maps | Phase 5c (MapLibre exchange map) |
| 7 | Network + multimedia + GPS | Phase 5c (TTS briefing + map + "Locate me") |
| 8 | AI chatbot | Phase 5b |
| 9 | ML features | Phase 5 (server) + 5b (on-device) |
| 10 | AI alerts/notifications | Phase 5c (FCM + rule engine) |

---

## 12. User Journey (How it works as a product)
1. **Onboard → sign up/log in** (Firebase). Notification permission requested after first login.
2. **Home:** watchlist summary, top gainers/losers (derived client-side), trending coins, "play briefing" button.
3. **Search:** type a name → debounced CoinGecko search → open a coin.
4. **Crypto Detail:** live price, 24h change, stats, price chart, AI prediction card (direction + confidence + disclaimer), on-device momentum signal, add/remove watchlist.
5. **Watchlist:** reorder, swipe-to-remove; syncs to other devices in real time.
6. **Predictions tab:** all coins with confidence bars, filter/sort by confidence or direction.
7. **Ask AI:** floating bubble opens the chatbot; history persists.
8. **Exchange Map:** explore exchanges, tap for volume/trust score, "Locate me" for nearest exchange.
9. **Alerts:** server detects a threshold event → push → tap opens that coin's detail (even from a killed app).
10. **Settings:** account, notifications, privacy policy, logout.
