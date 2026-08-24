# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**AI Crypto Predictor** — a native Android app (Kotlin/Java) for
cryptocurrency market data and AI-driven price predictions, built in
phases starting with a fully interactive UI (no backend) and growing
into a full production system. Phases double as coverage for the CE
mobile-computing lab practicals — see "Lab Practical Coverage" at the
end.

> Native Android Studio project (`C:\ADL\appli`, Gradle Kotlin DSL).
> `applicationId`/`namespace` is `com.stockpredictor.app` and the backend
> Java package is `com.stockpredictor.backend` — both are **intentionally
> kept as-is** after the stock→crypto pivot (see "Project History" below)
> rather than renamed, because Firebase's `google-services.json` is
> registered against that exact Android package; renaming it would
> require re-registering the Firebase app and re-downloading config for
> no functional benefit. Treat these as fixed, internal-only identifiers
> — every user-facing name, screen, class, and file *within* the app was
> renamed to the crypto domain (`Coin`, `CryptoDetailScreen`,
> `CoinRepository`, etc.); only the root package and a couple of
> deliberately-low-priority internal names (`StockPredictorFcmService`,
> `StockPredictorApplication`) were left alone as cosmetic debt.

**Project History:** this app was originally built and specced as an
"AI Stock Predictor" (NSE/BSE stocks via Finnhub/Alpha Vantage/Twelve
Data). Phases 1 through 3 were built against that spec. Partway through
Phase 4, the product was pivoted to crypto market data via CoinGecko —
Phases 1–3's *architecture* (ViewModel/UiState seam, raw-SQLite DAO
pattern, Firebase Auth/Firestore sync, Spring Boot backend skeleton) all
carried over unchanged; only the domain (stock→coin) and Phase 4's
market-data provider changed. This document has been updated in place to
describe the app as it actually exists today — stock-specific text below
describes historical phases' original framing only where it's still
accurate to what was built structurally; concrete class/file names and
provider references have been updated to match the current crypto
codebase.

Each phase below is written as a self-contained work order: goal, exact
files/folders to create or touch, ordered tasks, and a Definition of Done.
**Claude Code should not start work on a phase until the previous
phase's Definition of Done is met and the user has explicitly said to
proceed to the next phase** (per Working Conventions).

## Target Architecture (end state)

```
Android App (Kotlin/Java)
   ↓                    ↘
Spring Boot API (Java)   Firebase (Auth / Firestore / FCM — sync + push)
   ↓
 ┌───────────────┬─────────────────┐
 │               │                 │
PostgreSQL      Redis        Python AI Service (FastAPI)
 │                                 │
Users                        Feature Engineering
Portfolio                    XGBoost / LSTM / GRU
Watchlist                    Prediction Endpoint
Predictions
```

Local device layer:
```
Android App
   ↓
SQLite (raw, via SQLiteOpenHelper/ContentValues — no Room)
   → Watchlist cache (coin_id-keyed), recent searches, settings,
     cached_coins + cached_price_history (Phase 4 quote/history cache),
     offline predictions cache
```

Request flow, **target end-state** (Spring Boot proxies market data —
not yet built; see Phase 4's "Current status" for what actually runs
today):

```
Android App → GET /api/coins/bitcoin
            → Spring Boot
            → Market Data Service (CoinGecko)
            → Python AI Service (prediction)
            → Spring Boot (aggregates response)
            → Android App
```

Request flow, **as actually implemented today (Phase 4, Steps 1–3)**:
```
Android App → CoinGecko directly (Retrofit, Demo-tier API key)
            → CoinRepository (cache-then-network, SQLite-backed)
            → Android App
```
Android does not yet talk to the Spring Boot backend for market data —
see Phase 4 below for why, and `CoinDataSource`'s doc comment in code
for the seam that lets a backend-proxied path replace this later without
touching ViewModels.

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Kotlin (primary) + Java interop, native Android, Gradle Kotlin DSL |
| UI toolkit | Jetpack Compose (assumption — confirm if you want XML Views instead) |
| Local DB | SQLite — raw `SQLiteOpenHelper` + DAO-style helper classes, no Room |
| Cloud sync/auth | Firebase (Firestore for real-time sync, Firebase Auth, FCM for push) |
| Main backend | Java + Spring Boot (Spring Security + JWT) |
| Networking | Retrofit + OkHttp |
| AI/ML service | Python + FastAPI |
| ML models | XGBoost, LSTM/GRU (PyTorch) — see "Future AI Reference" below |
| On-device ML | TensorFlow Lite / ML Kit |
| Chatbot | Gemini (`GeminiChatbotClient`), behind a thin backend proxy (`ChatbotController`) — chosen over Dialogflow during Phase 5b |
| Maps | Google Maps SDK |
| Database (backend) | PostgreSQL |
| Cache (backend) | Redis — wired since Phase 6: distributed rate limiting for chatbot (`ChatbotRateLimiter`) and predictions (`PredictionRateLimiter`), both backed by a shared `RedisRateLimiter` |
| Market data | **CoinGecko** (Demo plan — see Phase 4). Historical stock-era candidates (Finnhub, Alpha Vantage, Twelve Data, Yahoo Finance) are no longer applicable — CoinGecko was chosen specifically for crypto coverage after live verification during the migration. |
| Charts | MPAndroidChart or Compose-native charting (e.g. Vico) — still not wired; Crypto Detail's price chart remains the Phase 1 Canvas placeholder |
| Deployment | Docker (host TBD — OCI Always Free is blocked on payment verification, AWS has been cancelled; local `docker compose up` for dev/submission in the meantime; see Phase 6's STATUS block) |

**Rule:** Kotlin/Java app and backend code never touch ML model code.
Java = application logic and orchestration only. Python = all AI/ML.

## Future AI Reference (Phase 5 — complete; kept as historical model-source evaluation)

[Stock-Market-Probabilities-Deep-Learning by OliverEdholm](https://github.com/OliverEdholm/Stock-Market-Probabilities-Deep-Learning)
— probability-of-move approach on *stock* data; the probability-vs-
single-confidence design question it raises still applies to crypto
predictions, but this specific repo's dataset assumptions are stock-era
and should not be assumed compatible with crypto OHLCV data without
review when Phase 5 actually starts.

Also flagged, not yet reviewed: https://share.google/HWHY4mNdmFRGXecJ9

When Phase 5 starts, re-evaluate the model-source shortlist against
crypto compatibility specifically (license, activity, runnable
training code, retraining support, time-series leakage risk, and
whether the dataset assumptions are stock-only or general OHLCV) rather
than assuming a stock-focused repo transfers cleanly — crypto's 24/7,
no-market-holiday trading pattern differs from equities in ways that can
affect feature engineering (e.g. no weekend gaps to handle specially).

## Design System — Claymorphism (White Minimalist)

Light, minimalist claymorphism — soft, puffy, matte "clay" surfaces on a
white/off-white base.

| Token | Hex | Suggested use |
|---|---|---|
| `bg` | `#F4F3F1` | App background base (warm off-white) |
| `clay-base` | `#FFFFFF` | Card/surface fill |
| `accent-primary` | `#6C63FF` (placeholder — confirm brand accent) | Primary buttons, active states, links |
| `accent-mint` | `#4CAF8C` | Gains / positive indicators |
| `accent-coral` | `#E2685A` | Losses / negative indicators |
| `text-primary` | `#2B2B2E` | Primary text |
| `text-secondary` | `#8A8A8E` | Secondary/labels |

Claymorphism rules:
- Flat `bg` background, no gradients.
- Clay surfaces: solid `clay-base` fill, dual shadow (soft light
  top-left + soft dark bottom-right) for an embossed puffy look. No
  blur/transparency.
- Corner radius 20–28dp everywhere, consistently.
- No hard 1px borders — the dual shadow is the border. Hairline only if
  unavoidable, at `rgba(0,0,0,0.04)`.
- Pressable state: inset "pushed in" shadow on tap.
- Text: `text-primary`/`text-secondary` only — no light-on-dark anywhere.
- Gains `accent-mint`, losses `accent-coral`.
- Motion: scale/press transitions, 100–200ms.
- Icons: rounded, filled.

Design tokens should live in one place code can reference —
`ui/theme/ClayTheme.kt` (Compose `Color`/`Shape`/`Dp` objects) or
`res/values/{colors.xml,dimens.xml}` if using XML Views — never
hardcoded hex/dp values inside individual composables/layouts.

---

## Phase 1 — UI Only

**STATUS: COMPLETE.** Built against the original stock spec below, then
carried through the crypto migration — every file/class name mentioned
in this section has since been renamed to its crypto equivalent
(`Stock`→`Coin`, `StockListTile`→`CoinListTile`, `stockdetail/`→
`cryptodetail/`, `MockStocks`→`MockCoins`, etc.); the *architecture*
(ViewModel/UiState seam, component library, navigation shape) is
unchanged from what's described here.

**Goal:** every screen fully tappable end-to-end with mock data. No
network, no DB, no Firebase.

**Why this phase matters:** everything downstream (Phase 2 onward) plugs
new data sources *behind* the ViewModels built here without touching the
composables. If the screen/ViewModel boundary is sloppy now, every later
phase turns into a rewrite instead of a swap. Treat Phase 1 as "build the
real app's skin and skeleton with a fake nervous system," not a
throwaway prototype.

**Package/folder structure to create:**
```
app/src/main/java/com/stockpredictor/app/
  MainActivity.kt
  navigation/
    AppNavHost.kt          // Navigation Compose graph, 5 top-level destinations
    Destinations.kt        // sealed class of routes
  ui/theme/
    ClayTheme.kt            // colors, typography, shapes as Compose objects
    ClayColor.kt
    ClayShapes.kt
  ui/components/
    ClayCard.kt
    ClayButton.kt
    ClayTextField.kt
    ClayAppBar.kt
    ClayBottomNav.kt
    PriceChangeChip.kt
    PredictionConfidenceBar.kt
    CoinListTile.kt
    LoadingState.kt
    EmptyState.kt
    ErrorState.kt
  ui/screens/
    onboarding/OnboardingScreen.kt
    auth/LoginScreen.kt
    auth/SignupScreen.kt
    auth/ForgotPasswordScreen.kt
    home/HomeScreen.kt
    cryptodetail/CryptoDetailScreen.kt
    watchlist/WatchlistScreen.kt
    portfolio/PortfolioScreen.kt
    search/SearchScreen.kt
    predictions/PredictionsScreen.kt
    notifications/NotificationsScreen.kt
    settings/SettingsScreen.kt
  mock/
    MockCoins.kt           // sample tickers, prices, history matching future API shape
    MockPredictions.kt
    MockPortfolio.kt
    MockNotifications.kt
  model/
    Coin.kt                 // data classes shaped like the eventual API response
    Prediction.kt
    PortfolioHolding.kt
    WatchlistItem.kt
    NotificationItem.kt
```

**Ordered tasks:**
1. Set up `ClayTheme.kt` with the full token table above as Compose
   `Color`/`Shape`/`Dp` constants — every later screen imports from here.
2. Build the `ui/components/` clay library first, in isolation (each
   previewable via `@Preview`), before any screen uses them.
3. Define `model/` data classes shaped exactly like the future Spring
   Boot API responses (so Phase 4's swap is a drop-in replacement) —
   e.g. `Coin(id, symbol, name, currentPrice, priceChangePercentage24h,
   history: List<PricePoint>)`.
4. Populate `mock/` with realistic sample data (10–15 tickers, plausible
   price history, plausible prediction confidences).
5. Build screens in this order: onboarding → auth (UI-only, local
   validation, `NavHost.navigate()` on submit, no real auth) → home →
   crypto detail → watchlist → portfolio → search → predictions →
   notifications → settings.
6. Wire `AppNavHost.kt` with all destinations; bottom nav bar shows Home
   / Watchlist / Predictions / Portfolio / Settings.
7. Add loading/empty/error composables and a mock toggle (e.g. a debug
   settings switch) to preview each state per screen without a real
   network call.
8. Manual pass: tap through every screen and every state at least once.

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — `ClayTheme.kt`:*
- Expose the token table as three objects: `ClayColor` (all seven hex
  values as `Color`, plus derived states like a 12%-alpha overlay of
  `accent-primary` for pressed/disabled backgrounds), `ClayShapes`
  (`RoundedCornerShape` presets at 20dp/24dp/28dp so components pick a
  consistent radius instead of inventing their own), `ClaySpacing` (a
  4dp-based scale: 4/8/12/16/24/32dp) even though spacing wasn't in the
  original token table — components need a shared spacing scale or
  padding will drift screen to screen.
- Define `ClayElevation` helpers for the dual-shadow effect (a
  light-source shadow + a dark offset shadow) as a reusable `Modifier`
  extension, e.g. `Modifier.clayShadow()`, so every component gets the
  embossed look from one function instead of copy-pasted `drawBehind`
  code.
- Wrap the whole app in a single `ClayTheme { content() }` composable at
  the `MainActivity` root so no screen ever reaches for a raw hex value.

*Task 2 — component library:*
- `ClayButton`: support at least `Primary`, `Secondary`, and `Text`
  variants plus `enabled`, `loading` (inline spinner replacing the
  label), and `disabled` (reduced-opacity, non-interactive) states —
  later screens (auth submit, watchlist add) need the loading state from
  day one even though the network call is fake.
- `ClayTextField`: needs an error state (red-tinted bottom border/label +
  helper text) because Login/Signup do local validation in this phase.
- `ClayAppBar`: title, optional back button, optional trailing action
  icon slot (Settings icon on Home, filter icon on Predictions).
- `ClayBottomNav`: 5 fixed destinations, active-item indicator using
  `accent-primary`, hidden entirely on Onboarding/Login/Signup/
  ForgotPassword routes (see navigation notes below).
- `PriceChangeChip`: takes a signed percentage, renders `accent-mint`
  with an up-arrow for ≥0 and `accent-coral` with a down-arrow for
  negative — this exact color rule is reused everywhere a price appears,
  so get it right once here.
- `PredictionConfidenceBar`: a horizontal bar 0–100 with a color ramp
  (e.g. coral <40, a neutral/amber mid-band, mint >70) — even though
  colors are only formally defined for gains/losses, define a sensible
  mid-band color now rather than leaving it undefined and having later
  phases guess.
- `LoadingState` / `EmptyState` / `ErrorState`: each takes a message and
  `EmptyState`/`ErrorState` should accept an optional retry callback —
  wire the callback as a no-op in Phase 1 previews, but design the API
  now so Phase 4 just passes a real retry lambda.

*Task 3 — `model/` classes:*
- **As originally spec'd (stock era):** `Stock(symbol: String, name:
  String, exchange: String, price: Double, change: Double,
  changePercent: Double, history: List<PricePoint>, lastUpdated: Long)`.
- **As it actually exists today**, post-migration: `Coin(id: String,
  symbol: String, name: String, image: String?, currentPrice: Double,
  marketCap: Long?, marketCapRank: Int?, totalVolume: Double?, high24h:
  Double?, low24h: Double?, priceChange24h: Double,
  priceChangePercentage24h: Double, circulatingSupply/totalSupply/
  maxSupply: Double?, ath/athChangePercentage/atl/atlChangePercentage:
  Double?, sparkline7d: List<Double>?, history: List<PricePoint>,
  description: String?, lastUpdated: Long)` — `id` is the CoinGecko coin
  id (e.g. `"bitcoin"`) and is the only safe lookup key; `symbol` (e.g.
  `"BTC"`) is display-only since multiple coins can share a symbol. The
  stock-era `exchange` field was dropped entirely (no clean crypto
  analog — CoinGecko coins aren't tied to one exchange); CoinGecko-shaped
  fields (`marketCap`, `ath`/`atl`, `sparkline7d`, etc.) were added.
- `Prediction(symbol: String, confidence: Float, direction:
  PredictionDirection, targetPrice: Double?, horizon: String,
  generatedAt: Long)` where `PredictionDirection` is a sealed
  enum/class (`Up`, `Down`, `Flat`) — this anticipates the Phase 5
  probability-vs-single-confidence decision without committing to it.
- `PortfolioHolding(symbol, quantity, avgBuyPrice, currentPrice)` with a
  derived (not stored) `gainLossPercent` computed property.
- `WatchlistItem(symbol, addedAt, sortOrder)` — include `sortOrder` now
  because Phase 2's DAO needs a persisted order for the reorder feature.
- `NotificationItem(id, title, body, timestamp, isRead, relatedSymbol:
  String?)` — `relatedSymbol` enables the Phase 5c deep-link tap target.

*Task 4 — mock data:*
- **As it actually exists today:** `MockCoins.kt` uses real, verified
  CoinGecko coin ids (bitcoin, ethereum, solana, binancecoin, ripple,
  cardano, dogecoin, polkadot, chainlink, litecoin, avalanche-2, tron) so
  real API calls in later Phase 4 steps and the mock data stay
  consistent — never invent a plausible-looking id that doesn't actually
  resolve on CoinGecko.
- Include at least one ticker with a flat/near-zero change (tests the
  neutral state of `PriceChangeChip`), one with a long history array
  (tests chart scroll/zoom), and one with a missing/null `targetPrice`
  in its prediction (tests `PredictionConfidenceBar` and detail screen
  null-handling).
- Predictions should span the full confidence range (some <40, some in
  the 40–70 band, some >70) to exercise every color in the confidence
  bar.

*Task 5 — screen build order and per-screen scope:*
- **Onboarding:** 2–4 swipeable slides + "Skip" and "Get Started";
  `Get Started`/`Skip` both route to Login. Persisted "don't show again"
  is out of scope until Phase 2 (SettingsDao) — note that explicitly in
  a TODO comment rather than half-implementing persistence here.
- **Login / Signup / ForgotPassword:** local-only field validation
  (non-empty, email format, password length) with inline `ClayTextField`
  error states; submit button uses `ClayButton`'s loading state for ~600ms
  fake delay before navigating, so the loading affordance is visually
  verified before any real network exists.
- **Home:** watchlist summary (horizontal scroll of `CoinListTile`/
  chip cards), a "movers" section (top gainers/losers from mock data),
  and an entry point into Search. Empty state if the mock watchlist is
  emptied via the debug toggle.
- **Crypto Detail:** header with symbol/name/price/`PriceChangeChip`, a
  placeholder chart area (even a simple Compose `Canvas` line plot of
  `history` is enough — do not wire a real charting library yet, that's
  explicitly deferred to whichever phase adds MPAndroidChart/Vico),
  `PredictionConfidenceBar` + direction, and an add/remove-watchlist
  toggle button.
- **Watchlist:** reorderable list (drag handle or up/down affordance)
  backed by in-memory state in Phase 1, swipe-to-remove, empty state
  when list is empty.
- **Portfolio:** holdings list with per-holding gain/loss chip and a
  running total value/gain header; empty state for no holdings.
- **Search:** text field with debounced (client-side, against the mock
  list) filtering, recent-searches chips row, results list reusing
  `CoinListTile`; empty state for "no matches."
- **Predictions:** list of tickers with `PredictionConfidenceBar`,
  sortable/filterable by confidence or direction (simple client-side
  filter chip row is enough for Phase 1).
- **Notifications:** list of mock alert items, read/unread visual state,
  tap marks as read (in-memory only).
- **Settings:** grouped list (Account, Notifications toggle, About,
  Logout-to-Login mock action) plus the debug mock-state toggle from
  Task 7.

*Task 6 — navigation:*
- Single-`Activity` + Compose Navigation, not multiple Activities.
- `Destinations.kt` as a sealed class/interface with typed routes (avoid
  raw string route concatenation scattered across screens); crypto
  detail's route takes a `coinId` argument (originally `symbol` in the
  stock-era spec — changed during the migration since a ticker symbol
  isn't a safe CoinGecko lookup key).
- Bottom nav is only shown on the 5 top-level destinations (Home,
  Watchlist, Predictions, Portfolio, Settings); Onboarding/Auth/
  CryptoDetail/Search/Notifications are pushed on top without bottom nav
  (use a `Scaffold` wrapper that conditionally shows `ClayBottomNav`
  based on the current back-stack entry's route).
- Back button from any top-level destination should exit the app (or
  return to Home first, then exit) rather than looping through nav
  history — decide and document the exact behavior in a code comment
  since it affects every later phase's manual QA.

*Task 7 — state per screen:*
- Give every screen a `ViewModel` (even though it just reads `mock/`
  data in Phase 1) exposing a single `UiState` sealed class/data class
  via `StateFlow` (`Loading` / `Success(data)` / `Empty` / `Error`).
  This is the seam Phase 2 and Phase 4 plug into — the composable never
  changes, only what the ViewModel's constructor injects changes.
  Skipping ViewModels now to "save time" is the single most likely
  cause of a Phase 4 rewrite.
- The debug mock-toggle (Task 7) should let Settings force any screen's
  `UiState` into `Loading`/`Empty`/`Error` regardless of the underlying
  mock data, so all four states are visually verifiable without writing
  throwaway test harnesses.

*Task 8 — manual QA pass:*
- Tap through all 11 screens, in both portrait orientation and after a
  configuration change (rotate), confirming navigation state and any
  in-memory list edits (watchlist reorder, notification read state)
  survive recomposition (they do not need to survive process death yet
  — that's Phase 2).
- Verify every `ClayButton` loading state actually shows before
  navigating (no accidental instant-navigate that skips the affordance).

**Definition of Done:**
- Every screen listed above exists, renders, and navigates correctly.
- No `http`/Retrofit/SQLite/Firebase import anywhere in the codebase yet.
- All colors/shapes/spacing come from `ClayTheme.kt`, zero hardcoded
  hex/dp in screen or component files.
- App builds and runs on an emulator/device with only mock data.

**Additional acceptance criteria:**
- Every screen's ViewModel exposes a `StateFlow<UiState>` and the
  composable renders purely from that state (no direct `mock/` reads
  inside a composable body).
- `LoadingState`/`EmptyState`/`ErrorState` have each been visually
  triggered at least once per screen via the debug toggle.
- Rotating the device on every screen does not lose navigation position
  or in-memory edits.
- All 11 `@Preview`-annotated component previews render without
  crashing in Android Studio's preview pane.

**Common pitfalls for Claude Code to avoid in this phase:**
- Reaching into `mock/` data directly from a composable "just for now" —
  this is exactly the shortcut that breaks the Phase 2/4 swap.
- Hardcoding a hex color or dp value the first time a new visual case
  comes up (e.g. a disabled-button gray) instead of adding it to
  `ClayTheme.kt` — every color/dimension decision belongs in the theme
  file, even one-offs.
- Wiring a real charting library in Crypto Detail — explicitly deferred;
  a simple Canvas line is sufficient and keeps this phase dependency-free.
- Adding Room, Retrofit, or Firebase Gradle dependencies "to save a step
  later" — the Definition of Done explicitly forbids these imports in
  Phase 1.

---

## Phase 2 — Local Persistence (SQLite)

**STATUS: COMPLETE**, and extended during the crypto migration: the
`watchlist` table gained `coin_id`/`name`/`image_url` columns (a real
`ALTER TABLE` migration, `DB_VERSION` 2→3 — not the recreate-tradeoff
described below, since real local data existed by then) and Phase 4
added two more tables (`cached_coins`, `cached_price_history`) following
the exact DAO/entity/`DbContract` pattern established here.

**Goal:** watchlist, recent searches, settings, and cached predictions
survive app restarts, via raw SQLite (no Room). Full CRUD.

**Why this phase matters:** this is the first phase where UI state has to
survive process death, and it establishes the DAO pattern (explicit
methods, no raw queries outside DAOs) that Phase 2.5's Firestore sync and
Phase 5b's chat history table both extend. Get the DAO contract right
here and every later table addition is mechanical.

**Files to add:**
```
app/src/main/java/com/stockpredictor/app/
  data/local/
    AppDatabaseHelper.kt     // extends SQLiteOpenHelper, onCreate/onUpgrade DDL
    dao/
      WatchlistDao.kt        // insert/query/update/delete against watchlist table
      RecentSearchDao.kt
      SettingsDao.kt         // key-value table, get/set
      CachedPredictionDao.kt
    entity/
      WatchlistEntity.kt
      RecentSearchEntity.kt
      CachedPredictionEntity.kt
    DbContract.kt            // table/column name constants, single source of truth
```

**Ordered tasks:**
1. `DbContract.kt`: define table names and column constants for all four
   tables so no raw string literals appear in DAO code.
2. `AppDatabaseHelper.kt`: `CREATE TABLE` DDL for each table in
   `onCreate`, and an explicit `onUpgrade` migration strategy (even if
   it's just version-bump + recreate for now — document the tradeoff in
   a comment).
3. One DAO class per table, each with explicit methods: `insert(...)`,
   `getAll()`, `getById(id)`, `update(...)`, `delete(id)` — never a raw
   `rawQuery` call from a screen or ViewModel.
4. Wire DAOs into the corresponding screens' ViewModels, replacing the
   Phase 1 mock in-memory lists:
    - Watchlist add/remove/reorder → `WatchlistDao`
    - Search screen "recent searches" chips → `RecentSearchDao`
    - Settings screen toggles → `SettingsDao`
    - Crypto detail's cached prediction (for offline viewing) →
      `CachedPredictionDao`
5. Confirm data survives an app restart (kill + relaunch, check
   watchlist/settings persist).

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — `DbContract.kt`:*
- Nested objects per table (`object WatchlistTable { const val NAME =
  "watchlist"; const val COL_SYMBOL = "symbol"; ... }`) rather than one
  flat namespace — mirrors the DAO-per-table split and keeps
  autocomplete useful as the schema grows in later phases.
- Reserve an `_id` primary key (`INTEGER PRIMARY KEY AUTOINCREMENT`) on
  every table even where `symbol` looks like a natural key — Phase 2.5's
  Firestore sync needs a stable local row identity independent of any
  remote document ID.

*Task 2 — schema and migration:*
- Suggested columns beyond the obvious symbol/timestamp fields:
    - `watchlist`: `_id, symbol, added_at, sort_order` (matches
      `WatchlistItem.sortOrder` from Phase 1's `model/`).
    - `recent_searches`: `_id, query, searched_at` — cap retrieval to the
      most recent N (e.g. 10) in the DAO's `getAll()`, not by deleting
      rows, so history isn't silently lost if the cap changes later.
    - `settings`: `_id, key, value` as a generic key-value table (matches
      the "SettingsDao — key-value table, get/set" note in Files to add)
      — store booleans/strings as text and let `SettingsDao` handle
      typed get/set wrappers (`getBoolean(key, default)`,
      `setBoolean(key, value)`) so callers never parse strings themselves.
    - `cached_predictions`: `_id, symbol, confidence, direction,
    target_price, horizon, generated_at, cached_at` — `cached_at` is
      distinct from `generated_at` so the UI can show "prediction may be
      stale" if `cached_at` is old, ahead of Phase 5's real predictions.
- `onUpgrade`: for this project's scope, a version-bump-and-recreate
  (`DROP TABLE IF EXISTS` + re-run `onCreate`) is an acceptable
  documented tradeoff — write the comment explaining that this drops
  user data on schema change and is fine pre-launch, but must be
  replaced with real `ALTER TABLE` migrations before Phase 6's
  production hardening if the schema changes again after real users
  exist.

*Task 3 — DAO pattern:*
- Every DAO method takes/returns the Phase 1 `model/` types (or the new
  `entity/` types plus a mapper function), never a raw `Cursor` outside
  the DAO — put `Cursor`-to-entity mapping in a private `fromCursor()`
  helper inside each DAO.
- `insert` methods should use `ContentValues` and `SQLiteDatabase.insert`
  with `SQLiteDatabase.CONFLICT_REPLACE` (or an explicit upsert via
  `insertWithOnConflict`) for tables like `settings` and
  `cached_predictions` where re-inserting the same key/symbol is a
  normal update path, not an error.
- Wrap multi-row writes (e.g. reordering the whole watchlist) in a
  single `beginTransaction()`/`setTransactionSuccessful()`/`endTransaction()`
  block so a reorder is atomic.
- DAOs should run on a background dispatcher (`Dispatchers.IO` via
  `withContext`) and expose `suspend fun`s — never block the main
  thread on SQLite calls, even though SQLite operations are typically
  fast.

*Task 4 — wiring into ViewModels:*
- This is where Phase 1's ViewModel/UiState seam pays off: each
  ViewModel's constructor swaps an in-memory `mock/` read for a DAO
  call, and the composable code does not change at all. If a composable
  needs to change to support this task, that is a signal Phase 1's
  separation wasn't clean — fix the seam rather than special-casing
  Phase 2.
- Watchlist reorder in the UI should call a single
  `WatchlistDao.updateSortOrders(orderedIds: List<Long>)` rather than N
  individual update calls, to keep the transaction atomic (see Task 3).
- Recent searches: record a search on submit (Search screen), not on
  every keystroke; de-duplicate by re-timestamping an existing identical
  query instead of inserting a duplicate row.

*Task 5 — persistence verification:*
- Manual test matrix: add 2+ watchlist items, reorder them, perform 2+
  searches, toggle a setting, view a crypto detail (populating the
  prediction cache) — force-stop the app (not just background it) and
  relaunch, confirming all four survive in the correct state (including
  watchlist order).

**Definition of Done:**
- All four tables exist with full CRUD exercised through the real UI
  (not just unit-tested in isolation).
- Data persists across app restarts.
- No Room annotations/dependencies added.

**Additional acceptance criteria:**
- No `rawQuery` or inline SQL string outside a DAO class anywhere in the
  codebase.
- All DAO calls happen off the main thread (verify with Android
  Studio's "Strict Mode" or by inspecting for `Dispatchers.IO`/`suspend`
  usage).
- The `onUpgrade` tradeoff (recreate vs. migrate) is documented with a
  comment in `AppDatabaseHelper.kt`.
- Reordering the watchlist under rapid repeated taps does not corrupt
  `sort_order` values (test by reordering 5+ times quickly).

**Common pitfalls for Claude Code to avoid in this phase:**
- Introducing Room "just for the annotations" — the project explicitly
  requires raw `SQLiteOpenHelper`; Room dependencies must not appear.
- Doing SQLite reads/writes on the main thread inside a Composable's
  `LaunchedEffect` without dispatching to `Dispatchers.IO`.
- Letting a ViewModel query the DAO directly for filtering/sorting logic
  that belongs in SQL (`ORDER BY sort_order`) or vice versa — keep query
  shaping in the DAO, presentation-only logic in the ViewModel.

---

## Phase 2.5 — Firebase Sync & Auth

**STATUS: COMPLETE**, and updated during the crypto migration:
`FirestoreSyncRepository`'s watchlist documents are now keyed by
CoinGecko coin id instead of ticker symbol (`users/{uid}/watchlist/
{coinId}`), carrying `coin_id`/`symbol`/`name`/`image_url` fields —
same last-write-wins/server-timestamp design described below, just a
different document key and a couple more fields.

**Goal:** real authentication + cross-device sync of watchlist/portfolio,
replacing Phase 1's mock auth screens. FCM channel established for later
use in Phase 5c.

**Why this phase matters:** this is the first phase with a real backend
dependency and the first place SQLite (local cache) and a remote source
of truth (Firestore) can disagree — the conflict-resolution strategy
chosen here (last-write-wins) needs to be applied consistently, or
watchlist items will appear to "flicker" between devices.

**Files to add:**
```
app/src/main/java/com/stockpredictor/app/
  data/remote/firebase/
    FirebaseAuthRepository.kt   // signUp, signIn, signOut, currentUser
    FirestoreSyncRepository.kt  // push/pull watchlist & portfolio, listener setup
    FcmTokenManager.kt          // register/refresh device token
  service/
    StockPredictorFcmService.kt // extends FirebaseMessagingService
```

**Ordered tasks:**
1. Add Firebase to the project (`google-services.json`, Firebase BoM,
   `firebase-auth`, `firebase-firestore`, `firebase-messaging`
   dependencies).
2. `FirebaseAuthRepository.kt`: wire real sign-up/login/forgot-password
   into the existing Phase 1 auth screens, replacing local-only
   validation with real Firebase Auth calls. Keep the same screen
   composables — only the ViewModel's data source changes.
3. Firestore schema: `users/{uid}/watchlist/{itemId}`,
   `users/{uid}/portfolio/{holdingId}` — mirror the SQLite entity shapes.
4. `FirestoreSyncRepository.kt`: on login, pull remote → merge into
   local SQLite; on local CRUD (Phase 2 DAOs), push the change to
   Firestore too. SQLite stays the offline cache; Firestore is the
   source of truth once online. Use a `SyncStatus` field or timestamp to
   resolve conflicts simply (last-write-wins is fine for this project).
5. Register FCM token on login (`FcmTokenManager`), store it under
   `users/{uid}/fcmToken` in Firestore. Implement
   `StockPredictorFcmService` to receive pushes — no alert-sending logic
   yet, just receive-and-display (full alert rules come in Phase 5c).

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — adding Firebase:*
- `google-services.json` must never be committed if the project is
  public; treat it the same as any secret per Working Conventions even
  though Firebase's own docs sometimes suggest committing it — confirm
  the repo's visibility with the user before deciding.
- Use the Firebase BoM (`platform("com.google.firebase:firebase-bom:...")`)
  so individual Firebase library versions stay compatible; do not pin
  `firebase-auth`/`firebase-firestore`/`firebase-messaging` versions
  independently.

*Task 2 — `FirebaseAuthRepository.kt`:*
- Expose `suspend fun signUp(email, password): Result<FirebaseUser>`,
  `signIn(...)`, `signOut()`, and a `currentUser: FirebaseUser?` /
  `authStateFlow(): Flow<FirebaseUser?>` — the Flow variant lets
  `AppNavHost` reactively redirect to Login if a session expires,
  instead of every screen polling `currentUser`.
- Map Firebase's `FirebaseAuthException` error codes
  (`ERROR_INVALID_EMAIL`, `ERROR_WEAK_PASSWORD`,
  `ERROR_EMAIL_ALREADY_IN_USE`, `ERROR_USER_NOT_FOUND`,
  `ERROR_WRONG_PASSWORD`, etc.) to the same `ClayTextField` inline-error
  presentation Phase 1 already built for local validation — reuse the
  UI, don't invent a new error-display pattern.
- ForgotPassword should call `sendPasswordResetEmail` and show a
  confirmation state (not silently succeed) since there is nothing else
  to visually confirm.

*Task 3 — Firestore schema:*
- Store `symbol`, `added_at`/`sort_order` (watchlist) and
  `symbol, quantity, avg_buy_price` (portfolio) fields identically named
  to the SQLite columns from Phase 2 — identical field names make the
  mapper between `entity/` and Firestore documents mechanical instead of
  a second translation layer to maintain.
- Add a `updated_at` (server timestamp, `FieldValue.serverTimestamp()`)
  field to every synced document — this is the field last-write-wins
  conflict resolution compares, not client clocks (client clocks can be
  wrong or skewed between devices).

*Task 4 — `FirestoreSyncRepository.kt`:*
- On login: pull all remote watchlist/portfolio documents, for each one
  compare `updated_at` against the local SQLite row's own `updated_at`
  (add this column to the Phase 2 `WatchlistEntity`/portfolio table if
  not already present) and keep whichever is newer, writing the winner
  back to both stores so they converge.
- On local CRUD: after a successful local SQLite write (Phase 2 DAO
  call), immediately fire an async push to the matching Firestore
  document — do this from the repository layer that wraps both DAOs and
  Firestore, not from inside the DAO itself (DAOs stay SQLite-only per
  Phase 2's contract) and not from the ViewModel (keep sync
  orchestration out of UI-facing code).
- Attach a real-time Firestore listener (`addSnapshotListener`) scoped
  to the current user's watchlist/portfolio collections while
  authenticated, so a change made on a second device updates this
  device's SQLite (and therefore UI) without a manual refresh — remove
  the listener on sign-out to avoid leaking a listener tied to a
  now-stale user.
- Queue local writes made while offline (Firestore's SDK has built-in
  offline persistence — enable it explicitly and document that this is
  what provides offline queueing, rather than building a custom queue).

*Task 5 — FCM registration:*
- Request notification permission (Android 13+ requires the runtime
  `POST_NOTIFICATIONS` permission) at a sensible point in the flow —
  right after first successful login, with a brief rationale, not on
  cold app start before the user has context.
- `StockPredictorFcmService.onNewToken` should re-register the token
  (tokens can rotate), and `onMessageReceived` should just display a
  system notification with the message payload for now — no in-app
  Notifications-tab item creation or rule evaluation yet, since that's
  explicitly Phase 5c's `AlertRuleService`.

**Definition of Done:**
- Real account creation/login works against Firebase Auth.
- Adding/removing a watchlist item on one device appears on another
  device signed into the same account.
- FCM token is registered and a manually-sent test push (from Firebase
  console) is received and shown.

**Additional acceptance criteria:**
- Signing out clears the active Firestore listener and does not leave
  the previous user's data visible if a different account signs in on
  the same device.
- Turning on airplane mode, editing the watchlist, then turning it back
  on results in the offline edits reaching Firestore without data loss.
- Firebase Auth error codes surface as the same inline `ClayTextField`
  error style used for local validation in Phase 1 — no raw exception
  text or a separate error UI pattern.
- `google-services.json` is present locally but is listed in
  `.gitignore` (or the user has explicitly confirmed it should be
  committed).

**Common pitfalls for Claude Code to avoid in this phase:**
- Comparing `updated_at` using device-local clocks instead of Firestore
  server timestamps — clock skew between two phones will silently break
  last-write-wins.
- Leaving a Firestore snapshot listener attached across sign-out/sign-in,
  causing data from the previous account to leak into the new session.
- Putting sync push/pull calls inside the Phase 2 DAOs — DAOs must stay
  SQLite-only; sync orchestration belongs in `FirestoreSyncRepository`.
- Skipping the `POST_NOTIFICATIONS` runtime permission request on
  Android 13+, which silently makes Phase 2.5's FCM test push
  undeliverable with no obvious error.

---

## Phase 3 — Backend Skeleton (Spring Boot)

**STATUS: COMPLETE, and UNTOUCHED by the crypto migration** — the
backend still runs exactly as built here (`com.stockpredictor.backend`
package, `StockPredictorApplication.java`, Firebase-token auth,
watchlist/portfolio/user endpoints against Postgres). Phase 4 was
implemented as Android calling CoinGecko directly instead of through
this backend (see Phase 4's "Current status"), so nothing here needed to
change yet. `watchlist`/`portfolio_holdings`' generic `symbol`/
`quantity`/`avg_buy_price` columns are crypto-compatible as-is if/when
Android is later wired to this backend — but note Android's SQLite/
Firestore watchlist is now keyed by `coin_id`, not `symbol` (Phase 2.5),
so this backend's schema would need the same treatment before that
wiring happens, or a symbol-vs-coin-id mismatch will surface then.

**Goal:** a running Spring Boot service Android can eventually call,
verifying Firebase ID tokens rather than issuing its own — avoids two
competing auth systems.

**Why this phase matters:** this is the only phase whose Definition of
Done explicitly does *not* require wiring the Android app to it — its
job is to exist correctly and be provably secure in isolation, so Phase 4
can be a pure "point Retrofit at this" exercise instead of also
debugging backend auth for the first time.

**Suggested backend repo/module structure** (separate Gradle/Maven
project or module, not inside the Android app module):
```
backend/
  src/main/java/com/stockpredictor/backend/
    StockPredictorApplication.java
    config/SecurityConfig.java        // verifies Firebase ID tokens
    user/UserController.java
    user/UserService.java
    watchlist/WatchlistController.java
    watchlist/WatchlistService.java
    portfolio/PortfolioController.java
    portfolio/PortfolioService.java
    common/dto/                       // response DTOs matching Android's model/ classes
  src/main/resources/
    application.yml
    db/migration/V1__init_schema.sql  // Flyway
```

**Ordered tasks:**
1. Scaffold Spring Boot project (`user`, `auth`, `watchlist`,
   `portfolio` modules/packages).
2. `SecurityConfig.java`: Spring Security filter that verifies incoming
   Firebase ID tokens (via Firebase Admin SDK) on protected endpoints —
   no separate JWT issuance.
3. PostgreSQL schema via Flyway migration (`V1__init_schema.sql`):
   `users`, `watchlist`, `portfolio_holdings` tables.
4. CRUD REST endpoints for watchlist/portfolio mirroring what Firestore
   already syncs — this backend becomes the eventual source of truth
   once Phase 4/5 add real market data and predictions; until then it
   can stay unused by the Android app.
5. DTOs shaped identically to Android's `model/` package so Phase 4's
   Retrofit integration is a straight mapping.

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — scaffolding:*
- Package by feature (`user/`, `watchlist/`, `portfolio/`, `common/`),
  not by layer (`controllers/`, `services/`, `repositories/`) — this
  keeps each vertical slice self-contained and matches how Phase 5b's
  `chatbot/` and Phase 5c's `alerts/` packages get added later without
  needing to touch unrelated folders.
- Use Spring Boot's standard `@SpringBootApplication` entry point
  (`StockPredictorApplication.java`) and `application.yml` (not
  `.properties`) for config, since later phases add nested config
  sections (market-data provider keys, Redis connection, alert
  thresholds) that read more cleanly as YAML.

*Task 2 — `SecurityConfig.java`:*
- Use the Firebase Admin SDK's `FirebaseAuth.getInstance().verifyIdToken(token)`
  inside a custom `OncePerRequestFilter`, not Spring Security's built-in
  OAuth2 resource-server support configured for a generic JWT issuer —
  Firebase ID tokens have Firebase-specific claims and revocation
  semantics the generic path doesn't check by default.
- On successful verification, populate the `SecurityContext` with an
  `Authentication` whose principal is the Firebase `uid` — every
  controller method that needs the current user should read it from
  `SecurityContextHolder`, never trust a `uid` passed as a request
  parameter (a parameter could be spoofed to access another user's
  data).
- Explicitly define which endpoints are public (health check) vs.
  protected (everything under `/api/**`) rather than defaulting
  everything to one or the other — an accidentally-public data endpoint
  is a silent security bug.
- Return `401 Unauthorized` with a clear JSON error body for
  missing/invalid/expired tokens — this is what the Definition of Done's
  "invalid/missing token is rejected" criterion checks, and a clear body
  makes Phase 4's Retrofit error handling straightforward.

*Task 3 — PostgreSQL schema:*
- `users`: `id (uuid, matches Firebase uid), email, display_name,
  created_at`.
- `watchlist`: `id, user_id (FK → users.id), symbol, added_at,
  sort_order` — same field shape as the Firestore/SQLite versions from
  Phase 2/2.5, so the eventual three-way sync (SQLite ↔ Firestore ↔
  Postgres) in later phases maps cleanly.
- `portfolio_holdings`: `id, user_id (FK), symbol, quantity,
  avg_buy_price, created_at, updated_at`.
- Every migration lives in `db/migration/` following Flyway's
  `V{n}__description.sql` naming so future schema changes are additive
  migrations (`V2__...`), never edits to `V1__init_schema.sql` once it
  has run anywhere.

*Task 4 — CRUD endpoints:*
- `GET/POST /api/watchlist`, `DELETE /api/watchlist/{id}`,
  `PUT /api/watchlist/reorder` and the equivalent for
  `/api/portfolio` — mirror exactly the operations the Phase 2 DAOs and
  Phase 2.5 Firestore repository already support, since Android's
  eventual `CoinRepository` (Phase 4) needs the same operation set
  across all three data sources.
- Return `404` for a watchlist/portfolio item that exists but belongs to
  a different `user_id`, not `403` — this avoids confirming to a caller
  that a given resource ID exists at all under another account.

*Task 5 — DTOs:*
- Keep `common/dto/` field names and types byte-for-byte matched to
  Android's `model/` package (same field names, compatible JSON
  number/string types) — per Working Conventions, when one changes,
  check the other; a mismatch here is exactly the kind of bug that only
  surfaces at Phase 4 integration time if not disciplined about it now.
- Add basic request validation annotations (`@NotBlank`, `@Positive`,
  etc. from `jakarta.validation`) on write DTOs so malformed requests
  fail with a `400` and a field-level error body, not a `500`.

**Definition of Done:**
- Backend runs locally, connects to Postgres, migrations apply cleanly.
- A valid Firebase ID token authenticates against a protected test
  endpoint; an invalid/missing token is rejected.
- Android app is NOT yet wired to this backend (that's Phase 4).

**Additional acceptance criteria:**
- `./mvnw flyway:migrate` (or gradle equivalent) applies `V1` cleanly to
  a fresh Postgres instance with no manual intervention.
- At least one integration test (Spring's `@SpringBootTest` +
  Testcontainers or an in-memory Postgres) exercises a protected
  endpoint with a valid token, an expired token, and no token.
- Watchlist/portfolio DTOs are diffed against Android's `model/` package
  field-by-field and confirmed to match.
- No endpoint returns raw Postgres/Hibernate error text to the client on
  failure — all errors go through a consistent error-response shape.

**Common pitfalls for Claude Code to avoid in this phase:**
- Issuing a separate application JWT "for simplicity" — the explicit
  design goal of this phase is a single auth system (Firebase), and a
  second token type reintroduces the exact problem it avoids.
- Trusting a `user_id`/`uid` sent in the request body/params instead of
  the one derived from the verified token in `SecurityContextHolder`.
- Wiring Android to this backend early "to test it end-to-end" — the
  Definition of Done explicitly defers that to Phase 4; keep the phases
  decoupled so backend bugs and Android integration bugs aren't debugged
  simultaneously.

---

## Phase 4 — Market Data Integration (CoinGecko)

**STATUS: Steps 1–3 COMPLETE.** This phase's original design (a market
data provider fronted by the Spring Boot backend, provider TBD between
Finnhub/Alpha Vantage/Twelve Data, NSE/BSE stock coverage) was
superseded mid-phase by the stock→crypto product pivot. Everything below
describes what was **actually built** for CoinGecko, not the original
stock-provider plan. The three sub-steps below map to the migration's
own internal step numbering (kept for traceability, not because a "Step
4" exists elsewhere in this document).

**Goal:** real coin prices/history flow from CoinGecko into the Android
app, replacing Phase 1 mock data.

**Why this phase matters:** this is the first phase with an external
third-party dependency (rate-limited, sometimes-down market data APIs)
in the critical path, so it's also the first phase where the
loading/empty/error UI states built in Phase 1 stop being decorative and
start being load-bearing.

**Provider decision (superseding the original Finnhub/Alpha
Vantage/Twelve Data plan):** Twelve Data was evaluated first (per an
earlier direction) and rejected after live verification — its free/Demo
tier excludes full India data entirely (a moot point once the pivot to
crypto happened, but the same live-verification discipline carried
through to picking CoinGecko). **CoinGecko, Demo plan**, was chosen and
verified end-to-end against the real API: base URL
`https://api.coingecko.com/api/v3/`, auth header `x-cg-demo-api-key`,
free tier ≈30 calls/min. Endpoints actually used: `/coins/markets`,
`/coins/{id}`, `/coins/{id}/market_chart`, `/search`,
`/search/trending`. **`/coins/top_gainers_losers` does not work on the
Demo tier — confirmed live, returns `401` even with a real key** — so
gainers/losers are derived client-side by sorting the `/coins/markets`
batch by `priceChangePercentage24h`, never fetched from that endpoint.

**Files actually added (Android):**
```
app/src/main/java/com/stockpredictor/app/
  data/remote/api/
    CoinGeckoApiService.kt       // Retrofit interface: markets, coin detail,
                                  // market_chart, search, trending
    RetrofitClient.kt            // OkHttp + Retrofit, key header interceptor,
                                  // debug-only BASIC-level logging (key header
                                  // explicitly redacted — see Task 3 below)
    CoinDataSource.kt            // interface — abstracts "where market data
                                  // comes from" so a future backend-proxied
                                  // source can replace direct CoinGecko calls
    CoinGeckoDirectDataSource.kt // the only impl today: calls CoinGecko directly
    dto/CoinGeckoDtos.kt         // response DTOs, field-verified against the
                                  // live API (kotlinx.serialization, snake_case
                                  // via @SerialName, ignoreUnknownKeys = true)
    mapper/CoinGeckoMappers.kt   // DTO -> model/ mapping (Coin, CoinSearchResult,
                                  // TrendingCoin) — ViewModels/composables never
                                  // see a DTO
  data/repository/
    CoinRepository.kt            // singleton (getInstance, matching
                                  // FirestoreSyncRepository's pattern) —
                                  // cache-then-network against cached_coins/
                                  // cached_price_history (SQLite, Phase 2),
                                  // TTL-gated, stale-fallback-on-failure,
                                  // Mutex-guarded per operation to prevent
                                  // overlapping calls from racing
    CoinSearchRepository.kt      // wraps /search only — the one place a coin
                                  // id is ever resolved from user-typed text,
                                  // deliberately uncached (query-specific)
    CoinDataException.kt         // CoinNotFoundException (404) vs.
                                  // CoinDataUnavailableException (network/5xx/
                                  // 429) — CancellationException always
                                  // rethrown, never treated as a real failure
```
**Backend: nothing added.** Android calls CoinGecko directly (see the
Target Architecture section's "as actually implemented today" diagram)
— no `crypto/CryptoController.java` or Redis layer exists. This was a
deliberate, explicit decision for this phase (not an oversight): keep
the backend decoupled from market data until the direct-from-Android
path is proven, using `CoinDataSource` as the seam a future
backend-proxied implementation slots into without touching
`CoinRepository`, `CoinSearchRepository`, or any ViewModel.

**Ordered tasks (as executed):**
1. Android: Retrofit + OkHttp + kotlinx.serialization setup;
   `BuildConfig.COINGECKO_API_KEY` sourced from `local.properties`
   (gitignored) via a `Properties()` read in `app/build.gradle.kts`;
   `buildFeatures.buildConfig = true` enabled.
2. `CoinRepository`/`CoinSearchRepository`: cache-then-network reads
   replacing `mock/MockCoins`/`mock/MockPredictions` reads in
   Home/Search/Watchlist/Crypto Detail ViewModels — Predictions and
   Portfolio ViewModels were deliberately left on mock data (Predictions
   stays mock until Phase 5; Portfolio has no live-data phase yet).
3. Real loading/empty/error states: `ui/state/DebugAwareState.kt` gained
   a second `debugAwareUiState(realState: Flow<UiState<T>>)` overload
   (the original only supported debug-forced errors, not real ones —
   this was a genuine gap in the Phase 1 seam that Phase 4 had to close,
   not something Phase 1 anticipated correctly) so real network
   Loading/Success/Empty/Error+retry can flow through the same seam
   Settings' debug toggle already used.

**Detailed implementation guidance (what was actually built, and why):**

*Provider key handling — an intentional deviation from the general
secrets rule below:* the CoinGecko Demo key is compiled into the APK via
`BuildConfig.COINGECKO_API_KEY`, **not** kept server-side-only. This is
a deliberate exception to Working Conventions' "never in the Android app
for server-side-only keys" rule, made explicitly because CoinGecko's
Demo tier is designed for client-side use (low privilege, ~30 calls/min,
easily rotated) — unlike the market-data keys that rule was originally
written for. The key still goes through the same `local.properties` /
`BuildConfig` seam as every other secret (never hardcoded, never
committed, never logged — `RetrofitClient`'s debug logging interceptor
is capped at `Level.BASIC` and explicitly calls `redactHeader()` on the
key header as defense in depth). If/when a backend proxy is built (see
"Backend: nothing added" above), route the key through the backend's
environment config instead and this client-side exception goes away.

*Caching design actually implemented:*
- SQLite (`cached_coins`, `cached_price_history` — Phase 2's tables,
  added during migration) is the durable offline cache; `CoinRepository`
  checks freshness before ever calling network.
- TTLs (constants in `CoinRepository.kt`, not hardcoded inline): markets
  list 60s, coin detail 120s, price history 6h, trending 5 minutes
  (trending has no SQLite table — it's ephemeral discovery content
  cached only in-memory on the `CoinRepository` singleton).
- On network failure, serve the SQLite/in-memory cache past its TTL
  rather than propagating the failure, with an `isStale: Boolean` flag
  threaded into each screen's UI-data class (e.g. `HomeUiData.isStale`)
  so the UI can show "showing recently cached data" instead of silently
  lying about freshness — same design intent as the original
  Redis-`isStale`-field plan, just implemented client-side since there's
  no backend layer yet.
- **Both the general market list and per-id batched lookups (e.g. the
  watchlist's `ids=` query) are TTL-gated the same way** — this was a
  defect found in an early audit (only the general list was gated
  originally, so watchlist/trending re-fetched on every screen
  re-entry) and fixed: `getMarkets()` now checks freshness for both
  `ids == null` and `ids != null` calls before deciding network is
  needed.
- `CoinRepository` and its markets/detail/trending operations are each
  guarded by a `Mutex` (coarse-grained, one per operation type) so
  overlapping calls (e.g. a ViewModel's init-triggered load racing its
  screen's first `LaunchedEffect`, or Home's concurrent
  watchlist/general-list/trending fetches) coalesce instead of racing
  each other or corrupting the in-memory trending cache. Every
  `catch (e: Exception)` in this data path explicitly rethrows
  `CancellationException` first — a cancelled (superseded) fetch must
  never be treated as "the network call failed."
- Each Home-tab/Watchlist-tab ViewModel keeps a `refreshJob: Job?` and
  cancels the previous one before launching a new `refresh()` — combined
  with removing the redundant `init { refresh() }` call (the screen's
  own `LaunchedEffect(Unit)` already triggers the initial load; calling
  it from both `init` and the first composition used to double-fire on
  cold start).

*Search (`CoinSearchRepository`):* `/search` is the only endpoint a
raw user-typed query is ever sent to, per the hard rule "never guess a
coin id from a ticker symbol" — search results (`CoinSearchResult`: id,
symbol, name, image, market cap rank) intentionally carry no price data
(CoinGecko's `/search` doesn't return any), so `SearchScreen` uses a
dedicated lightweight `CoinRankTile` component instead of the
price-carrying `CoinListTile`. Debounced 300ms via `collectLatest`
(cancels a superseded in-flight search rather than racing it).

*Error handling verified against the live API:* `/simple/price`/
`/search` with an unmatched query returns `200` with an empty result
(`{"coins":[]}`), not an HTTP error — this is treated as `UiState.Empty`,
never `UiState.Error`. An unknown coin id on `/coins/{id}` returns `404`
→ `CoinNotFoundException` → `UiState.Empty` (not a whole-screen error,
since "this specific coin doesn't exist" isn't the same failure mode as
"CoinGecko is unreachable"). `429`/`5xx`/timeout all map to
`CoinDataUnavailableException` with a user-facing message and a retry
callback wired to the ViewModel's own refresh function.

**Definition of Done:**
- Home, Search, Watchlist, and Crypto Detail show real, live CoinGecko
  data — confirmed working end-to-end.
- Loading/empty/error states trigger from real conditions, not a debug
  switch — confirmed (real `UiState.Error` with retry, real `Empty` for
  unmatched search/unknown coin id).
- No `mock/` data reachable from Home/Search/Watchlist/Crypto Detail
  anymore (`mock/` stays for Predictions/Portfolio, which have no
  live-data phase yet).

**Additional acceptance criteria:**
- Turning off the device's network mid-session and reopening a
  previously-viewed coin shows the last cached price, marked stale —
  implemented, code-reviewed; not device-verified (no emulator/device
  available in the environment this was built in — flag this as an open
  manual-QA item, not a completed check).
- Exhausting CoinGecko's rate limit does not cascade into a crash or
  unhandled error — implemented via the exception hierarchy above;
  same device-verification caveat as above.
- Repeated Home/Watchlist re-entry within the TTL window does not
  re-hit the network — implemented and traced through the code by hand
  (Home→Detail→Back→Home, Watchlist tab re-entry); same
  device-verification caveat.
- Search-as-you-type is debounced (300ms) — implemented.

**Common pitfalls for Claude Code to avoid in this phase (updated for
what was actually learned building this):**
- Assuming a market-data provider's free/Demo tier supports every
  endpoint you plan to use — verify live, per-endpoint, before
  designing around it. `/coins/top_gainers_losers` looked plausible from
  documentation alone but 401'd on Demo tier in practice.
- Building a cache-then-network TTL check for only the "obvious" call
  (a general/unfiltered list) and assuming batched/filtered variants of
  the same underlying data don't need the same treatment — they do, and
  skipping it silently reintroduces the exact redundant-refetch problem
  the TTL was built to prevent.
- Catching a bare `Exception` in a coroutine that might be cancelled
  (e.g. by `Job.cancel()` or `collectLatest` superseding it) without
  rethrowing `CancellationException` first — this lets a cancelled,
  superseded operation still mutate shared UI state after a newer one
  has already produced the correct result.
- Letting DTO types leak into ViewModels or composables — the mapper
  layer exists specifically so Phase 1's screens don't need to change
  when the data source changes.
- Treating "search returned zero results" the same as "search request
  failed" in the UI layer.

---

## Phase 5 — AI/ML Service

**STATUS: COMPLETE.** The single-confidence design (Task 1's open question)
was resolved in favor of `Prediction`'s original `PredictionDirection`
shape, not the probability-of-move split. `ai-service/` has a real trained
XGBoost model (`training/train_xgboost.py`, `training/build_dataset.py`,
pooled multi-coin training with a genuine time-based train/validation
split — 7/7 backend tests passing, an honest 0.39 Macro F1), a working
`POST /predict` (verified live, real inference), and `PredictionController`/
`PredictionService`/`PredictionRateLimiter` on the backend. Android's
`PredictionRepository` calls the real backend endpoint (`PredictionsScreen`
and `CryptoDetailScreen` both use it, with a `CachedPredictionDao`
read-through fallback on failure); `mock/PredictionMocks.kt` has no other
references left anywhere in the codebase.

**Goal:** real AI predictions replace Phase 1 mock confidence/trend
values.

**Why this phase matters:** this is the phase the "Rule" in Tech Stack
exists for — Java/Kotlin code must never contain model logic, and Python
must never contain application orchestration. Keeping that boundary
sharp here is what lets the model be retrained/swapped later without
touching Spring Boot or Android at all.

**Files to add (separate Python service):**
```
ai-service/
  main.py                     // FastAPI app
  models/xgboost_model.py
  models/lstm_model.py
  features/feature_engineering.py
  api/prediction_router.py    // POST /predict
  requirements.txt
```

**Ordered tasks:**
1. Decide (with the user) whether to evaluate the probability-of-move
   approach from the "Future AI Reference" repo before locking the model
   interface — this affects the response shape (`confidence: float` vs
   `probabilities: {up: float, down: float, flat: float}`).
2. Build feature engineering + a first model (start with XGBoost —
   faster to get end-to-end working than LSTM/GRU).
3. `POST /predict` endpoint: symbol + history in, prediction out.
4. Spring Boot calls this service and aggregates the result into the
   existing `/api/coins/{id}` response (add a `prediction` field).
5. Android: `CoinRepository` picks up the new field; Predictions tab
   and Crypto Detail's prediction card switch from `mock/` to real data.

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — response shape decision:*
- This decision gates everything downstream in this phase — Phase 1's
  `Prediction` model already anticipated it with a `PredictionDirection`
  sealed type plus a single `confidence: Float`, which maps naturally to
  a single-confidence design; adopting the probability-of-move approach
  instead means extending `Prediction` to carry per-direction
  probabilities and updating `PredictionConfidenceBar` to render three
  segments instead of one bar — surface this concretely to the user as
  "single bar" vs. "three-way split" before building either, since it's
  a visible UI difference, not just a backend detail.
- Whichever shape is chosen, lock it in `common/dto/` (backend) and
  `model/Prediction.kt` (Android) together, per the DTO/model matching
  convention, before writing the FastAPI response model.

*Task 2 — feature engineering and first model:*
- `feature_engineering.py`: compute standard technical indicators from
  price history (e.g. moving averages, RSI, momentum/rate-of-change,
  volume trend if volume is available from the market-data provider) —
  keep this as pure functions taking a price-history array and returning
  a feature vector/frame, so both the XGBoost and LSTM/GRU paths in
  `models/` can share it.
- `xgboost_model.py`: train on the engineered features with a simple,
  documented train/validation split by time (not random shuffling —
  shuffling price history randomly leaks future information into
  training, which would make offline accuracy look better than it will
  ever be live). Persist the trained model artifact (e.g. via `joblib`)
  under a path the FastAPI app loads at startup, not retrained per
  request.
- `lstm_model.py` exists as a stub/interface in this task (per "start
  with XGBoost — faster to get end-to-end working") — do not fully
  train/wire the LSTM/GRU path yet; the goal of this task is one working
  model end-to-end, not two.

*Task 3 — `/predict` endpoint:*
- `prediction_router.py`: `POST /predict` takes `{symbol, history:
  List[PricePoint]}` (matching the shape Spring Boot already has from
  Phase 4's history endpoint — no separate history-fetching in Python)
  and returns the prediction shape locked in Task 1.
- Validate the incoming `history` has enough points for the feature
  window the model needs (e.g. reject/short-circuit with a clear error
  if fewer than N days of history are provided, rather than letting
  feature engineering silently produce NaNs).
- Load the trained model once at FastAPI startup (module-level or via a
  dependency-injected singleton), not per-request, for latency.

*Task 4 — Spring Boot integration:*
- Add a thin `PredictionClient` in the backend that calls the FastAPI
  `/predict` endpoint with the history it already fetched from the
  market-data provider for that symbol (Phase 4), so Python never talks
  to the market-data provider directly — this keeps the "Java =
  orchestration, Python = AI/ML" rule intact and avoids two independent
  integrations with the same external provider.
- If the AI service is down or times out, `/api/coins/{id}` should
  still return the quote data with `prediction: null` rather than
  failing the whole request — predictions are additive, not
  load-bearing, for the core quote-viewing experience.

*Task 5 — Android wiring:*
- `Prediction` field on `Coin`/the crypto-detail response becomes
  nullable-aware in the UI: Crypto Detail's `PredictionConfidenceBar`
  section should show its own small empty/error state (not the whole
  screen's `ErrorState`) when `prediction` is null, consistent with
  Task 4's backend behavior.
- Predictions tab switches its list source from `mock/PredictionMocks`
  to `CoinRepository`, reusing the same cache-then-network pattern from
  Phase 4 if predictions are also cached (recommended, since model
  inference is more expensive than a quote lookup — consider caching
  predictions in Redis on the backend with a longer TTL than quotes).

**Definition of Done:**
- Predictions tab and Crypto Detail prediction card show real model
  output for at least one ticker end-to-end (Android → Spring Boot →
  FastAPI → back).
- `mock/PredictionMocks.kt` no longer referenced anywhere in shipped
  screens (fine to keep for Compose `@Preview`s).

**Additional acceptance criteria:**
- The train/validation split for the first model is time-based, and the
  split methodology is documented in a comment or short README in
  `ai-service/`.
- Killing the AI service and requesting `/api/coins/{id}` from
  Spring Boot still returns a `200` with quote data and `prediction:
  null`, not a `5xx`.
- The FastAPI service loads its model artifact once at startup
  (confirm via a log line or startup timing, not measurable per-request
  latency for model loading).
- No feature-engineering or model-inference code exists anywhere in the
  Kotlin/Java codebases.

**Common pitfalls for Claude Code to avoid in this phase:**
- Random train/test splitting on time-series price data — this is the
  single most common way this kind of model silently looks better in
  offline evaluation than it performs live.
- Putting any indicator/feature computation in Spring Boot "since it's
  simple math" — per the Tech Stack rule, all AI/ML logic (including
  feature engineering) stays in Python.
- Making the `/predict` call synchronously block the main
  `/api/coins/{id}` response with no timeout — always bound how
  long Spring Boot waits on the AI service before falling back to
  `prediction: null`.

---

## Phase 5b — On-Device ML + Chatbot

**STATUS: COMPLETE.** Toolkit choice (Task 1) resolved to a custom TFLite
model, not ML Kit's pre-built classifier: `OnDeviceMomentumClassifier.kt`
runs a real, bundled `momentum_model.tflite` fully offline (no network
call). Its own doc comment is honest about weak performance
(`training/train_momentum_tflite.py`'s report: macro F1 0.30 vs. a 0.25
naive-baseline, 0% recall on UP moves) — shipped as a best-effort local
signal alongside, never replacing, the server-side AI Prediction card, not
as a polished feature. Chatbot: `ChatbotScreen`/`ChatbotViewModel`
(Android), `ChatbotController`/`ChatbotService`/`GeminiChatbotClient`/
`ChatbotRateLimiter` (backend — Gemini, not Dialogflow), `ChatMessageDao`
for local history — all wired into `AppNavHost` from Settings.

**Goal:** a lightweight on-device ML feature, plus an "Ask AI" assistant.

**Why this phase matters:** it introduces two very different execution
models in the same phase — fully offline, on-device inference, and a
fully online, backend-proxied chat — and it's easy to blur the line
between them (e.g. accidentally routing the on-device feature's data
through the network, or leaking the chatbot's API key client-side).
Keep the two threads of this phase mentally separate while implementing.

**Files to add:**
```
app/src/main/java/com/stockpredictor/app/
  ml/OnDeviceSentimentClassifier.kt   // TF Lite or ML Kit wrapper
  ui/screens/chatbot/ChatbotScreen.kt
  data/remote/api/ChatbotApiService.kt // Retrofit call to backend proxy endpoint
  data/local/dao/ChatMessageDao.kt     // local chat history table (extend Phase 2's DB)
```
```
backend (Spring Boot):
  chatbot/ChatbotController.java   // proxies to Dialogflow/LLM, hides the API key
```

**Ordered tasks:**
1. Pick TF Lite or ML Kit (confirm with user based on what the on-device
   feature actually is — e.g. ML Kit's built-in text classification if
   it's headline sentiment, custom TF Lite model if it's something
   bespoke).
2. Backend: `ChatbotController` proxying to Dialogflow or an LLM API —
   the API key lives only on the backend, never in the Android app.
3. Android: chat UI (`ChatbotScreen.kt`, `LazyColumn` message list, clay
   message bubbles), calling the proxy endpoint via Retrofit.
4. Extend the Phase 2 SQLite DB with a `chat_messages` table
   (`ChatMessageDao`) so conversation history persists locally.

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — on-device model choice:*
- Confirm the exact feature with the user before picking a toolkit —
  "headline sentiment on cached news" points at ML Kit's pre-built text
  classification (no training/conversion needed); a bespoke feature
  (e.g. a small on-device confidence adjustment model) points at a
  custom TF Lite model requiring conversion/quantization.
- `OnDeviceSentimentClassifier.kt` should expose a single suspend
  function (`classify(text: String): SentimentResult`) that internally
  handles model loading (once, lazily, cached for the classifier's
  lifetime) — never reload the model per call.
- This feature must work with the device in airplane mode — that's the
  entire point of "on-device" — so write the manual test as "airplane
  mode on, feature still returns a result" rather than just "feature
  returns a result."

*Task 2 — backend chatbot proxy:*
- `ChatbotController.java`: a single `POST /api/chatbot/message`
  endpoint taking `{conversationId, message}` and returning
  `{reply, conversationId}` — the backend, not Android, holds the
  Dialogflow/LLM API key in its environment config (per Working
  Conventions' secrets rule) and the backend, not Android, is
  responsible for constructing whatever provider-specific request shape
  Dialogflow/the chosen LLM API needs.
- Keep the controller thin — provider-specific request building belongs
  in a `ChatbotService`, so swapping Dialogflow for an LLM API later (or
  vice versa) doesn't touch the controller's request/response contract
  with Android.
- Rate-limit or otherwise bound this endpoint (even a simple per-user
  request cap) since it's proxying a metered/paid third-party API —
  an unbounded proxy is a cost-control risk, not just a security one.

*Task 3 — chat UI:*
- `ChatbotScreen.kt`: `LazyColumn` with two message-bubble styles (user
  vs. assistant), both built from the existing `ClayCard`/theme tokens
  rather than new one-off styling — user bubbles could use
  `accent-primary`-tinted background, assistant bubbles plain
  `clay-base`, keeping with the "text-primary/secondary only, no
  light-on-dark" rule from the Design System.
- Show a typing/loading indicator (reuse `ClayButton`'s loading pattern
  or a dedicated small `LoadingState` variant) while awaiting the
  backend's reply, and route request failures through the existing
  `ErrorState` pattern with a retry-last-message action.

*Task 4 — chat history persistence:*
- `ChatMessageDao` follows the exact same DAO contract established in
  Phase 2 (`insert`, `getAll`/`getBySession`, `delete`) — this is
  additive to the Phase 2 database (new table via a schema-version bump
  in `AppDatabaseHelper`'s `onCreate`/`onUpgrade`, not a new database
  file).
- Store `conversationId, role (user/assistant), content, timestamp` per
  message row; `ChatbotScreen` loads history for the active
  `conversationId` on open and appends new messages as they're
  sent/received.

**Definition of Done:**
- On-device classifier returns a result with no network call.
- Chatbot screen holds a real conversation through the backend proxy,
  and history survives app restart.

**Additional acceptance criteria:**
- The on-device classifier is verified to work with the device in
  airplane mode (not just "no network call observed" in normal
  conditions).
- The chatbot LLM/Dialogflow API key does not appear anywhere in the
  Android app's compiled APK, decompiled resources, or source —
  confirm it is read only from the backend's environment config.
- Reopening the app after a chat session shows the prior conversation
  history loaded from `ChatMessageDao`, in correct chronological order.
- The chatbot proxy endpoint has a documented rate/cost-control measure
  (even a simple fixed cap) rather than being fully unbounded.

**Common pitfalls for Claude Code to avoid in this phase:**
- Calling an LLM/Dialogflow API key directly from Android "to move
  faster" — this directly violates the Working Conventions secrets rule
  and the explicit goal of this phase ("the API key lives only on the
  backend").
- Re-loading the TF Lite/ML Kit model on every classification call
  instead of once per classifier lifetime — this is a common source of
  janky on-device latency.
- Creating a second SQLite database file for chat history instead of
  extending the Phase 2 `AppDatabaseHelper` schema.

---

## Phase 5c — Maps, Multimedia & AI Notifications

**STATUS: COMPLETE — re-scoped exactly along the lines this section
originally proposed.** The stock-era premise below (`ExchangeData`'s
`openLocalTime`/`closeLocalTime` + a live open/closed badge) doesn't apply
to 24/7 crypto markets, as originally flagged here. The re-scoping decision
this section asked for was made: `ExchangeData.kt`'s `ExchangeLocation` has
**no timezone/open-closed fields at all** — live-verified CoinGecko
exchange ids, registered-jurisdiction location data, and live
`tradeVolume24hBtc`/`trustScore` (fetched by `ExchangeRepository`, joined
at read time) replace the open/closed badge entirely — the "regional
trading-volume distribution instead of open/closed status" alternative
this section listed as a plausible option. `ExchangeMapScreen`/
`ExchangeMapViewModel` are wired into `AppNavHost` from Settings.
`MarketBriefingSpeaker.kt` (TTS) exists. The backend `alerts/` package is
fully built: `AlertRuleService` (scheduled), `AlertEvaluator`,
`CoinGeckoMarketDataClient`, `FirestoreWatchlistReader`,
`FirebaseMessagingPushSender`, and `AlertCooldownService`/
`AlertCooldownEntity`/`AlertCooldownRepository` (dedup/cooldown, per Task
5's requirement below). Android's `StockPredictorFcmService`/
`FcmTokenManager` handle delivery. The task list below (Tasks 2–6) should
be read with `ExchangeData`'s open/closed-specific guidance understood as
superseded by the above, not as still-open instructions.

**Goal:** Global Exchanges Map (re-scoped — see STATUS above), audio
market briefing, AI-triggered push notifications — using the FCM channel
from Phase 2.5.

**Why this phase matters:** this phase closes the loop opened in Phase
2.5 — the FCM channel that was only "receive and display" now gets real
server-driven rules behind it, and this is the phase most likely to
touch device permissions (location isn't needed for the map since
exchanges are static data, but notifications and TTS both have their own
platform quirks) and real-world timezone/DST correctness (if the
open/closed concept survives re-scoping in some form).

**Files to add:**
```
app/src/main/java/com/stockpredictor/app/
  ui/screens/exchangemap/ExchangeMapScreen.kt
  data/ExchangeData.kt                 // static list: exchange name, lat/lng, timezone
  audio/MarketBriefingSpeaker.kt       // wraps android.speech.tts.TextToSpeech
  service/AlertRuleEvaluator.kt        // checks prediction confidence / price-move thresholds
```
```
backend (Spring Boot):
  alerts/AlertRuleService.java   // evaluates rules server-side, sends via FCM Admin SDK
```

**Ordered tasks:**
1. Add Google Maps SDK, API key in `local.properties` (never committed).
2. `ExchangeData.kt`: static data for major exchanges (NYSE, NASDAQ,
   LSE, NSE, BSE, TSE, etc.) with timezone, used to compute live
   open/closed status client-side.
3. `ExchangeMapScreen.kt`: markers per exchange, tap → bottom sheet with
   local time + open/closed + (if available) index snapshot.
4. `MarketBriefingSpeaker.kt`: TTS read-out of today's top
   predictions/watchlist movers — a "play briefing" button on Home.
5. Backend `AlertRuleService`: evaluates simple rules (prediction
   confidence crosses threshold, watchlist coin moves >X% intraday) on
   a schedule, sends FCM pushes to registered tokens.
6. Android: `StockPredictorFcmService` (from Phase 2.5 — real class name,
   kept as-is per this doc's "Project History" note) now has real
   alerts to display, tapping one deep-links to that coin's detail
   screen.

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — Maps SDK setup:*
- API key restricted (Android app + package name + SHA-1 restriction in
  the Google Cloud Console) at creation time, not added unrestricted "to
  get it working first" — an unrestricted Maps key committed or leaked
  is a billable liability.
- Confirm the key is read via `local.properties`/`BuildConfig`, not
  hardcoded in the manifest, matching the same secrets rule already
  applied to Firebase/market-data/chatbot keys.

*Task 2 — `ExchangeData.kt`:*
- Each entry: `name, code (e.g. "NYSE"), city, latitude, longitude,
  timezoneId (IANA zone, e.g. "America/New_York"), openLocalTime,
  closeLocalTime` and a weekday-only assumption noted explicitly (no
  market holiday calendar in this phase — call that out as a known
  limitation in a comment rather than silently getting holidays wrong).
- Compute "open/closed" using `java.time` (`ZonedDateTime.now(ZoneId.of(timezoneId))`
  compared against the exchange's local open/close times) — never do
  manual UTC-offset arithmetic, which breaks across daylight saving
  transitions; this is exactly the kind of bug that only shows up twice
  a year and is easy to miss in one QA pass.

*Task 3 — `ExchangeMapScreen.kt`:*
- One marker per exchange from `ExchangeData`, color-coded (e.g.
  `accent-mint` marker tint for currently-open, muted/`text-secondary`
  for closed) so status is visible before tapping.
- Bottom sheet on tap: exchange name, current local time (live, not a
  snapshot taken at screen-open — update at least once a minute while
  the sheet is visible), open/closed badge, and an index snapshot only
  "if available" per the task description — treat this as optional and
  gracefully omit it rather than blocking the whole feature on finding
  an index-quote data source.

*Task 4 — `MarketBriefingSpeaker.kt`:*
- Wrap `android.speech.tts.TextToSpeech`, handling its async
  initialization callback (`TextToSpeech.OnInitListener`) before
  allowing `speak()` calls — calling `speak()` before `onInit` fires is
  a common source of silently-dropped audio.
- Compose the briefing text server-side-free, purely from already-loaded
  watchlist + prediction data (no new network call needed for the
  briefing itself) — e.g. "Your watchlist: Bitcoin up two percent,
  Ethereum down one percent. Top prediction: Solana, seventy percent
  confidence upward." Keep numbers rounded and sentence structure simple
  for TTS clarity.
- Expose `stop()` alongside `speak()` and call it on screen
  navigate-away/`onDispose`, so leaving Home mid-briefing doesn't leave
  audio playing over the next screen.

*Task 5 — `AlertRuleService.java`:*
- Run on a fixed schedule (Spring's `@Scheduled`, e.g. every few
  minutes during market hours only — use the same open/closed logic
  concept as `ExchangeData`, server-side, to avoid evaluating rules
  (and spending FCM sends) outside trading hours).
- Rule set for this phase, explicitly scoped simple per the task:
  prediction confidence crosses a configurable threshold, or a
  watchlist coin's intraday change exceeds a configurable percentage —
  both thresholds should be config values (`application.yml`), not
  hardcoded, so they can be "manually lowered temporarily" per the
  Definition of Done's test instruction without a code change.
- De-duplicate: don't re-send the same alert for the same
  user/symbol/rule within a cooldown window (e.g. once per hour) — an
  unthrottled rule evaluated every few minutes would otherwise spam the
  same crossed-threshold alert repeatedly.
- Send via the Firebase Admin SDK's messaging API to each user's
  registered token(s) from Phase 2.5's `users/{uid}/fcmToken`, including
  a data payload (not just a display notification) carrying the target
  `coinId` so Android can deep-link (not `symbol` — matches the coin-id
  keying used throughout the rest of the app since the migration).

*Task 6 — Android alert handling:*
- Extend `StockPredictorFcmService.onMessageReceived` to read the
  `coinId` from the data payload and construct a deep-linking
  `PendingIntent` that navigates directly to
  `CryptoDetailScreen(coinId)` via `AppNavHost`'s typed route (Phase 1),
  rather than just opening `MainActivity` with no destination.
- Handle both foreground (app open — show as an in-app
  `NotificationItem` via the existing Notifications tab / `mock/`-turned
  -real notifications data) and background/killed (system tray
  notification, per standard FCM behavior) delivery paths.

**Definition of Done:**
- Exchange map shows correct live open/closed status for at least 3
  exchanges in different timezones.
- Briefing button reads out real watchlist/prediction data via TTS.
- A manually-triggered rule (e.g. lower the threshold temporarily)
  produces a real push notification that deep-links correctly.

**Additional acceptance criteria:**
- Open/closed status for at least one exchange is manually verified
  correct across a daylight-saving transition date (or by temporarily
  forcing the device/test clock across one), not just "looks right
  today."
- The briefing correctly announces a fully-empty watchlist (a "no
  movers to report" fallback line) rather than reading nothing or
  crashing.
- Rapidly triggering the same alert condition twice within the cooldown
  window results in only one push being sent.
- Tapping a received alert notification while the app is fully killed
  (not just backgrounded) still lands on the correct `CryptoDetailScreen`.

**Common pitfalls for Claude Code to avoid in this phase:**
- Manual UTC-offset math for exchange open/closed status instead of
  `java.time` + IANA zone IDs — breaks on DST transitions.
- Calling `TextToSpeech.speak()` before its `onInit` callback has fired.
- Evaluating/sending alerts around the clock instead of scoped to each
  exchange's trading hours, wasting FCM sends and risking rate limits.
- Sending a display-only FCM notification with no data payload, which
  makes the deep-link requirement in Task 6 impossible to satisfy.

---

## Phase 6 — Production Hardening

**STATUS: Tasks 1–5 (Android hardening) and the Docker/Compose/CI portions
of Task 6 COMPLETE; final hosting target deliberately undecided.** This
phase's original design (below) targeted AWS (ECS/Fargate vs.
EC2+RDS+ElastiCache, left undecided). That target changed twice after this
section was first written:

1. **AWS → OCI Always Free** (an approved pivot, not reflected in the task
   list below): a revised plan targeted a single OCI Ampere A1 VM (2
   OCPU/12GB, Always Free), Docker Compose (nginx + Spring Boot + FastAPI +
   PostgreSQL + Redis) as the entire production environment, OCI Object
   Storage for backups/model artifacts, and GitHub Actions CI/CD building
   ARM64 images on free native-ARM64 runners. Most of this was actually
   built and live-verified: Redis-backed distributed rate limiting
   (`ChatbotRateLimiter`, `PredictionRateLimiter`), structured JSON logging
   (backend + ai-service), the 5-service `docker-compose.yml` topology
   (verified end-to-end locally — Flyway migrations, Postgres/Redis
   connectivity, Nginx TLS termination/routing, a real authenticated
   backend→ai-service→backend prediction request, restart/persistence),
   and both `docker/backend.Dockerfile`/`docker/ai-service.Dockerfile`
   verified to build and run correctly on real `linux/arm64` (and
   `linux/amd64`). `infra/` holds Terraform implementing the OCI
   VCN/instance/Reserved-IP/Object-Storage provisioning — written and
   reviewed, **never applied** (no OCI credentials were ever available in
   the environment this was built in).
2. **OCI → blocked, AWS → cancelled** (current state): the OCI tenancy is
   blocked on payment verification, and AWS has been cancelled outright.
   Decision: develop and run via local `docker compose up` for now; the
   final host (a college server, or a paid VPS) is chosen at submission
   time, not before. `infra/`'s Terraform is kept untouched as a future OCI
   path in case that block resolves — it is not deleted or repurposed for
   any other provider.

Also complete, independent of any hosting decision: Task 1 (R8/ProGuard,
verified via a real signed `assembleRelease`), Task 2 (real keystore
signing), Task 3 (Crashlytics wired, test-crash path verified), Task 4
(`PrivacyPolicyScreen`, linked from Settings), Task 5 (`PredictionDisclaimer`
on both the Crypto Detail AI Prediction card and every Predictions-tab
row). The app itself was kept host-portable throughout this churn without
any code changes being needed: `BACKEND_BASE_URL` is a `BuildConfig` field
(`app/build.gradle.kts`, sourced from `local.properties`, defaulting to the
emulator-to-host alias `http://10.0.2.2:8080/`), not a hardcoded string,
and both Dockerfiles are architecture-agnostic (no arch-specific branches).

**Goal:** ship-ready release build.

**Why this phase matters:** every corner cut "for now" in Phases 1–5c
(the Firestore-vs-Room decision, the recreate-not-migrate SQLite
upgrade, the unbounded-looking chatbot proxy, hardcoded thresholds) gets
revisited here — this phase is as much an audit of earlier phases'
documented tradeoffs as it is new work.

**Ordered tasks:**
1. ProGuard/R8 rules for release build; verify obfuscation doesn't break
   Retrofit DTOs, Firestore models, or TF Lite model loading.
2. Signed release build config (keystore — never commit it or its
   credentials).
3. Firebase Crashlytics wired in.
4. Privacy policy screen — required given Firebase + Maps + on-device
   ML + chatbot data handling; link it from Settings and (if applicable)
   the Play Store listing.
5. "Not investment advice" disclaimer surfaced in the UI wherever
   predictions are shown (Crypto Detail, Predictions tab) — an ethics/
   legal necessity given the AI prediction and chatbot features.
6. Dockerize backend services (Spring Boot, FastAPI); docker-compose for
   local dev; deploy target originally AWS (ECS/Fargate or EC2 + RDS +
   ElastiCache) — since superseded twice; see the STATUS block above for
   what actually happened and the current host-TBD state.

**Detailed implementation guidance (elaboration on the tasks above):**

*Task 1 — ProGuard/R8:*
- Add `-keep` rules for: Retrofit's response DTO classes (reflection-
  based Gson/Moshi parsing breaks silently under obfuscation if fields
  are renamed), Firestore's `@PropertyName`-mapped model classes, and
  any TF Lite model-loading reflection paths (ML Kit/TF Lite interpreter
  classes commonly need explicit keep rules) — verify by running a full
  release build and exercising login, watchlist sync, a crypto detail
  view, and the on-device classifier, not just by checking the build
  succeeds (obfuscation bugs are runtime failures, not compile
  failures).
- Re-run the Phase 5's model-loading manual test (airplane mode, on-
  device classification still works) specifically against the release
  build, since this is exactly the kind of path R8 can silently break.

*Task 2 — signing:*
- Keystore file and its passwords go in a non-committed
  `keystore.properties` (or equivalent), read by `build.gradle.kts` at
  build time — never inline the passwords in the Gradle file itself,
  even though it's tempting for a "just get it building" release
  config.
- Document the keystore backup/recovery plan somewhere outside the repo
  (e.g. in whatever secrets manager the team already uses) — losing a
  release keystore means losing the ability to update the app under the
  same listing.

*Task 3 — Crashlytics:*
- Add the Crashlytics Gradle plugin + dependency, confirm a forced test
  crash (`Crashlytics` has a documented test-crash method) appears in
  the Firebase console within a few minutes.
- Attach non-PII breadcrumb logging (e.g. "entered CryptoDetailScreen for
  symbol X", "sync conflict resolved for watchlist item Y") at a few key
  points across the phases already built, so a real crash report has
  useful context without logging anything privacy-sensitive.

*Task 4 — privacy policy:*
- The policy needs to actually cover, in plain language, every data
  category the app collects across all prior phases: Firebase Auth
  (email), Firestore-synced watchlist/portfolio, FCM tokens, Maps usage,
  on-device ML (clarify this stays on-device and isn't uploaded), and
  chatbot messages (which do leave the device, via the backend proxy to
  Dialogflow/an LLM) — treat this as a checklist against the whole
  document above, not boilerplate text.
- Link from Settings (already scaffolded in Phase 1) and from the Play
  Store listing's privacy policy URL field if/when publishing.

*Task 5 — disclaimer:*
- Persistent, clearly-legible placement (not a one-time dismissible
  dialog) wherever a `PredictionConfidenceBar`/prediction value is shown
  — Crypto Detail's prediction card and every row in the Predictions tab
  — short, plain text (e.g. "Predictions are for informational purposes
  only and are not investment advice") styled with `text-secondary`,
  consistent with the Design System rather than an alarming red banner.

*Task 6 — Dockerize and deploy:*
- Separate `Dockerfile`s for the Spring Boot backend and the FastAPI AI
  service (different base images/runtimes); a `docker-compose.yml` for
  local dev wiring both plus Postgres and Redis together with the same
  env-var names the deployed environment will use, so local dev and
  deployment configs don't drift into two different shapes.
- **Host is deliberately undecided until submission** (see the STATUS
  block above — AWS cancelled, OCI blocked on payment verification) — do
  not provision or assume a specific target prematurely. `docker-compose.yml`
  and both Dockerfiles are already host-agnostic (verified on both
  `linux/amd64` and `linux/arm64`), so this decision doesn't block any
  engineering work in the meantime.
- Carry forward every secret established across earlier phases
  (Firebase Admin SDK service account, market-data provider key,
  chatbot/LLM API key, Maps key, Postgres/Redis credentials) into the
  deployment environment's secret/config management — do not bake any
  of them into the Docker images themselves.

**Definition of Done:**
- Signed release APK/AAB builds and runs with no debug-only code paths
  active.
- Crashlytics receives a test crash.
- Disclaimer and privacy policy are visible in the shipped UI.

**Additional acceptance criteria:**
- A full manual regression pass of the Phase 1–5c feature list (auth,
  watchlist sync, real prices, real predictions, chatbot, on-device
  classifier, exchange map, briefing, alerts) is executed against the
  signed release build specifically, not just a debug build.
- No secret (Firebase config, API keys, keystore credentials) appears in
  the Docker images, the committed repo, or the release APK/AAB.
- `docker-compose up` brings up backend + AI service + Postgres + Redis
  locally with no manual post-start steps beyond running migrations.
- The "not investment advice" disclaimer and privacy-policy link are
  reachable within two taps from both Crypto Detail and Predictions tab.

**Common pitfalls for Claude Code to avoid in this phase:**
- Treating this phase as "just add ProGuard and sign it" — the bulk of
  the real work is re-validating everything built in Phases 1–5c under
  release/obfuscated conditions, which routinely surfaces bugs that
  never appeared in debug builds.
- Writing the privacy policy generically instead of against the actual
  data flows implemented in this specific codebase.
- Assuming a specific final host (OCI, a college server, a VPS) before the
  user actually decides at submission time — keep `docker-compose.yml`,
  both Dockerfiles, and Android's `BACKEND_BASE_URL` build-config seam
  host-agnostic in the meantime, per the STATUS block above.

---

## Working Conventions

- Work strictly one phase at a time — do not scaffold a later phase's
  files (Firebase, Spring Boot, Python, Maps, etc.) until the user
  explicitly says to start that phase.
- Every new screen must use the shared clay component library — extend
  `ui/components/`, never duplicate shadow/radius/press-state code
  inline.
- Keep mock data (while still in use) realistic and shaped exactly like
  the eventual API response, so later phases are a drop-in swap, not a
  rewrite.
- Prefer composable decomposition over giant single-file screens: one
  file per component, one file per screen, matching the folder structure
  above.
- Kotlin is primary; use Java only where interop or an existing
  library/module requires it.
- SQLite access goes through dedicated DAO classes per table — never a
  raw query scattered through UI/ViewModel code.
- Any third-party API key (Maps, Firebase, LLM/chatbot, market data)
  goes in `local.properties` / a non-committed secrets file or the
  backend's environment config — never hardcoded, never in the Android
  app for server-side-only keys (chatbot/LLM, market data provider).
  **Documented exception:** the CoinGecko Demo-tier market-data key
  (Phase 4) is deliberately compiled into the Android app via
  `BuildConfig.COINGECKO_API_KEY`, still sourced only from
  `local.properties` — CoinGecko's Demo tier is designed for client-side
  use (low privilege, easily rotated, ~30 calls/min), unlike the
  server-side-only keys this rule otherwise targets. See Phase 4's
  "Provider key handling" note for the full reasoning and the condition
  under which this exception should be revisited (a future backend proxy).
- Backend DTOs and Android `model/` classes should be kept in matching
  shape — when one changes, check the other.

## Lab Practical Coverage

| Practical | Topic | Covered in |
|---|---|---|
| 1 | Basic UI, input controls | Phase 1 |
| 2 | Multi-screen navigation | Phase 1 — bottom nav, back-stack |
| 3 | Local DB (SQLite/Room) | Phase 2 — raw SQLite, full CRUD |
| 4 | Firebase data sync | Phase 2.5 |/
| 5 | REST APIs (Retrofit/Volley) | Phase 4 |
| 6 | Google Maps | Phase 5c — Global Exchanges Map |
| 7 | Network + multimedia + GPS combined | Phase 5c — audio briefing + map + network |
| 8 | AI chatbot | Phase 5b — "Ask AI" assistant |
| 9 | ML-based features | Phase 5 (server-side) + Phase 5b (on-device) |
| 10 | AI-based alerts/notifications | Phase 5c — FCM + rule engine |