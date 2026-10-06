# Real-Time Tracker

A simple real-time location-sharing web app with a companion Android demo. Start a share session from the web, send live GPS updates from a phone (or the Android demo), and view a moving marker and ETA on a map in real time.

✅ Tech: Node.js + Express + Socket.IO (web) • Leaflet + OSRM (map & routing) • Android (Kotlin, Google Maps, FusedLocationProvider)

---

## Demo / Features

- Create a short-lived sharing session from the landing page.
- Sharer (mobile) sends periodic GPS updates via WebSocket.
- Viewer sees a live marker, travel history polyline, and ETA to a destination (click map to set destination).
- Android demo app shows a moving marker on Google Maps, draws a polyline to a hardcoded destination and computes ETA.

---

## Quick start (web server)

Prerequisites:
- Node.js (14+)
- npm

1. Install dependencies

   npm install

2. Run the server

   node app.js  (or `npm start`)

3. Open in a browser

   http://localhost:4000

- Enter a mobile number → a session link is created.
- Open the session link on a mobile/browser at `/share/:sessionId` to start sharing.
- Open `/track/:sessionId` on any device to view live location.

Environment variables (.env):
- PORT (optional, default 4000)

Note: sessions are stored in-memory for demo purposes. For production, persist sessions in Redis or a database and secure links.

---

## Android demo (Kotlin)

Location demo located in `app/java/com/example/realtimetracker`.

Requirements:
- Android Studio
- A device or emulator with Google Play services
- Google Maps API key

Steps:
1. Open the project in Android Studio.
2. Replace the placeholder API key in `app/manifests/AndroidManifest.xml`:

   <meta-data android:name="com.google.android.geo.API_KEY" android:value="YOUR_API_KEY_HERE" />

3. Build & run on a device/emulator. Grant location permission when prompted.

Behavior:
- Uses FusedLocationProviderClient for updates.
- Shows a moving marker, updates a polyline to a hardcoded destination (change in `RouteHelper.kt`), and shows ETA.

---

## Project structure (important files)

- `app.js` — Express + Socket.IO server and routes
- `public/` — client-side JS, CSS (Leaflet map + OSRM routing)
- `views/` — EJS templates (`index`, `share`, `track`)
- `app/` — Android demo (Kotlin + manifests + resources)
- `package.json` — Node dependencies & start script

---

## How the real-time flow works

1. User creates a session (`POST /api/session`) → server returns `sessionId`.
2. Sharer visits `/share/:sessionId` — client uses Geolocation to emit `send-location` events via Socket.IO.
3. Viewer visits `/track/:sessionId` — joins the same Socket.IO room and receives `receive-location` events.

---

## Production Deployment Guide

### Option 1: Deploy on Render (Recommended)

1. **Push your code to GitHub**:
   Ensure your latest commits are pushed to your remote repository.

2. **Database (MongoDB Atlas)**:
   - Create a free cluster at [mongodb.com/atlas](https://www.mongodb.com/atlas).
   - Under **Database Access**, create a database user and password.
   - Under **Network Access**, add `0.0.0.0/0` (allow connections from anywhere) so Render can reach the cluster.
   - Click **Connect** → **Drivers** (Node.js) and copy your connection string (`mongodb+srv://...`).

3. **Deploy with Blueprint / Render Web Service**:
   - Go to [dashboard.render.com](https://dashboard.render.com) and click **New +** → **Blueprint** (or **Web Service**).
   - Connect this repository.
   - If using **Web Service** manually:
     - **Runtime**: `Node`
     - **Build Command**: `npm install`
     - **Start Command**: `npm start`
   - Add the following **Environment Variables**:
     - `PORT`: `10000` (or leave default assigned by Render)
     - `MONGODB_URI`: Your MongoDB Atlas connection URI
     - `JWT_SECRET`: A long random secret string (e.g. `openssl rand -hex 32`)

4. **Seed the Initial Admin Account**:
   - In Render, navigate to your web service and open the **Shell** tab.
   - Run:
     ```bash
     npm run seed:admin
     ```
   - Default admin credentials:
     - **Email**: `admin@example.com`
     - **Password**: `ChangeMe123!`

---

### Option 2: Deploy on Railway

1. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo**.
2. Add a **MongoDB** plugin directly inside Railway, or set `MONGODB_URI` pointing to Atlas in Variables.
3. Add `JWT_SECRET` in **Variables**.
4. Railway will automatically detect `npm start` and run the application.

---

## Environment Variables Reference

| Variable | Description | Example / Default |
|---|---|---|
| `PORT` | Listening port for Express & Socket.IO | `4000` (Local) / `10000` (Render) |
| `MONGODB_URI` | MongoDB connection string (Atlas or local) | `mongodb+srv://user:pass@cluster.mongodb.net/tracker?retryWrites=true&w=majority` |
| `JWT_SECRET` | Secret key used to sign authentication cookies | `your_secret_random_token_key` |

---

## Contributing

- Create issues or PRs. Keep the scope small and add tests where appropriate.

---

## License

This repository is provided as-is. Add a LICENSE file if you want to set an explicit license.

---

Included in this repo:
- `.env.example` — example environment variables
- `render.yaml` — Render infrastructure blueprint
- `.gitignore` — ignores node_modules, Android/IDE files, and secrets
- `CONTRIBUTING.md` — short contribution guidelines

The web server runs with `npm start` (defaults to port `4000` locally, or dynamically assigns `PORT` on cloud platforms). 
