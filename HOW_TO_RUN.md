# How to Run — AI Crypto Predictor

## 1. Login credentials for the demo

No password can be recovered or reset here (Firebase never stores/returns
plaintext passwords, and resetting one is a live-account mutation this
session is blocked from doing unattended). **Fastest option: just sign up
fresh** — the Signup screen is real, working Firebase Auth:

1. Launch the app (see §3).
2. Tap **Sign Up** on the login screen.
3. Any email format + password ≥6 chars, e.g. `demo@example.com` / `Demo1234`.
4. You're in — watchlist/portfolio/settings all persist to that new account.

## 2. Start the backend stack (Docker)

Requires Docker Desktop running.

```powershell
cd C:\ADL\appli
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

- The `docker-compose.dev.yml` overlay is **required**, not optional — it republishes the
  backend's port 8080 on `localhost` so the Android emulator (which talks to `10.0.2.2:8080`)
  can reach it. Plain `docker compose up -d` alone will NOT work for emulator testing.
- First run builds images (a few minutes); after that it's seconds.
- Verify all 5 services are healthy:
  ```powershell
  docker ps --format "{{.Names}}\t{{.Status}}"
  ```
  Expect: `backend`, `nginx`, `postgres`, `redis`, `ai-service` all `Up ... (healthy)`.
- Logs if something looks wrong: `docker logs ai-crypto-predictor-backend-1`

To stop everything: `docker compose -f docker-compose.yml -f docker-compose.dev.yml down`
(add `-v` only if you also want to wipe the Postgres/Redis data volumes).

**Required first-time setup:** `.env` (repo root, gitignored) must contain
`POSTGRES_PASSWORD`, `GEMINI_API_KEY`, `COINGECKO_API_KEY`, and
`firebase-service-account.json` must exist at the repo root — both are already
in place if you're picking this up from prior sessions.

## 3. Run the Android app (Android Studio)

1. Open Android Studio → **Open** → select `C:\ADL\appli` (the root, not `app/`).
2. Let Gradle sync finish (first sync can take a few minutes).
3. **Required:** `local.properties` (repo root, gitignored) must contain
   `COINGECKO_API_KEY=<your key>`. `BACKEND_BASE_URL` defaults to
   `http://10.0.2.2:8080/`, which is correct for the emulator talking to the
   Docker backend from step 2 — no change needed unless using a physical device.
4. Select a device: an emulator (Pixel 8 / API 34+ recommended, with Google
   Play services) or a physical device on the same network.
5. Click **Run ▶** (or Shift+F10). First build installs and launches the app.

### Command-line alternative (no Android Studio UI)
```powershell
cd C:\ADL\appli
.\gradlew.bat installDebug
adb shell am start -n com.stockpredictor.app/.MainActivity
```

## 4. Order that actually works end-to-end

1. `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d` (backend stack up first)
2. Launch the emulator (Android Studio's Device Manager, or `emulator -avd Pixel_8`)
3. Run the app from Android Studio (or `gradlew installDebug` + `adb shell am start`)
4. Sign up fresh (§1) → explore Home / Watchlist / Predictions / Portfolio / Settings →
   Chatbot ("Ask AI" in Settings) → Exchange Map (Settings) → Play Briefing (Home, TTS)

## Notes
- Predictions/chatbot require the backend (step 2) to be up and healthy — without it
  those screens show a real error state with retry, not mock data (by design).
- Live crypto prices come directly from CoinGecko (no backend needed for Home/Watchlist/Search).
- Exchange Map currently requires a Google Maps API key (`MAPS_API_KEY` in
  `local.properties`) that is **not configured** — this screen won't render tiles
  until that's set. (A MapLibre/OpenFreeMap replacement has been evaluated but not
  yet implemented — see conversation history / CLAUDE.md Phase 5c.)
