require('dotenv').config();
const express = require('express');
const app = express();
const path = require("path");
const http = require("http");
const socketio = require("socket.io");
const { v4: uuidv4 } = require('uuid');
// New imports
const connectDB = require('./config/db');
const cookieParser = require('cookie-parser');
const { authenticateJWT, authorizeRoles } = require('./utils/auth');
const User = require('./models/User');
const TrackingSession = require('./models/TrackingSession');

// Use in-memory store for demo simplicity. In production, use Redis/MongoDB.
const sessions = {};
const socketSessionMap = {}; // Maps socket.id -> sessionId
// Initialise MongoDB connection
connectDB().catch(err => {
  console.error('MongoDB connection error:', err);
  process.exit(1);
});
app.use(cookieParser());
// expose sessions object for routes
app.set('sessions', sessions);

const server = http.createServer(app);
const io = socketio(server);

app.set("view engine", "ejs");
// Middleware
app.use(express.static(path.join(__dirname, "public")));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

// Make io accessible to routes
app.set('io', io);

// Register auth routes
app.use('/auth/student', require('./routes/auth/student'));
app.use('/auth/driver', require('./routes/auth/driver'));
app.use('/auth/admin', require('./routes/auth/admin'));
// Register dashboards
app.use('/student', require('./routes/student/dashboard'));
app.use('/driver', require('./routes/driver/dashboard'));
app.use('/admin', require('./routes/admin/dashboard'));



// --- Routes ---

// 1. Landing Page
app.get("/", function (req, res) {
  // Render role selection page
  res.render("role-select");
});

// 2. Create Session
app.post("/api/session", (req, res) => {
  const mobile = req.body.mobile;
  const sessionId = uuidv4();
  sessions[sessionId] = { mobile, active: false };

  // 1-hour session TTL cleanup
  setTimeout(() => {
    if (sessions[sessionId]) {
      io.to(sessionId).emit("sharing-stopped");
      delete sessions[sessionId];
      console.log(`Session ${sessionId} expired and cleaned up`);
    }
  }, 60 * 60 * 1000);

  res.json({
    sessionId,
    driverUrl: `/driver/${sessionId}`,
    studentUrl: `/student/${sessionId}`
  });
});

// 3. Sharer Interface (Mobile) — EXISTING, DO NOT REMOVE
app.get("/share/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("share", { sessionId });
});

// 4. Viewer Interface (Map) — Supports both MongoDB TrackingSession and in-memory
app.get("/track/:id", async (req, res) => {
  const sessionId = req.params.id;
  // First check in-memory
  if (sessions[sessionId]) {
    return res.render("track", { sessionId });
  }
  // Check MongoDB
  try {
    const dbSession = await TrackingSession.findOne({ socketRoomId: sessionId }).populate('driver');
    if (dbSession) {
      // Ensure in-memory sessions dictionary has a reference for socket tracking
      sessions[sessionId] = sessions[sessionId] || {
        mobile: dbSession.routeInfo || (dbSession.driver && dbSession.driver.name) || 'Bus',
        active: dbSession.status === 'active'
      };
      return res.render("track", { sessionId });
    }
  } catch (err) {
    console.error("Error looking up session in DB:", err);
  }
  return res.status(404).send("Session not found");
});

// 5. Driver Broadcasting Page — Supports both MongoDB and in-memory
app.get("/driver/:id", async (req, res) => {
  const sessionId = req.params.id;
  if (sessions[sessionId]) {
    return res.render("driver", { sessionId, busName: sessions[sessionId].mobile });
  }
  try {
    const dbSession = await TrackingSession.findOne({ socketRoomId: sessionId }).populate('driver');
    if (dbSession) {
      const busName = dbSession.routeInfo || (dbSession.driver && dbSession.driver.name) || 'Driver Bus';
      sessions[sessionId] = { mobile: busName, active: dbSession.status === 'active' };
      return res.render("driver", { sessionId, busName });
    }
  } catch (err) {
    console.error("Error looking up session for driver:", err);
  }
  return res.status(404).send("Session not found");
});

// 6. Student Tracking Page — Supports both MongoDB and in-memory
app.get("/student/:id", async (req, res) => {
  const sessionId = req.params.id;
  if (sessions[sessionId]) {
    return res.render("student", { sessionId, busName: sessions[sessionId].mobile });
  }
  try {
    const dbSession = await TrackingSession.findOne({ socketRoomId: sessionId }).populate('driver');
    if (dbSession) {
      const busName = dbSession.routeInfo || (dbSession.driver && dbSession.driver.name) || 'Student Bus';
      sessions[sessionId] = { mobile: busName, active: dbSession.status === 'active' };
      return res.render("student", { sessionId, busName });
    }
  } catch (err) {
    console.error("Error looking up session for student:", err);
  }
  return res.status(404).send("Session not found");
});

// 7. Admin Dashboard — NEW
app.get("/admin", (req, res) => {
  if (req.query.pass !== "giet2025") {
    return res.redirect("/");
  }
  const buses = Object.entries(sessions).map(([id, data]) => ({ id, ...data }));
  res.render("admin", { buses });
});

// 8. Admin API: Get all sessions — NEW
app.get("/api/admin/sessions", (req, res) => {
  if (req.query.pass !== "giet2025") {
    return res.status(403).json({ error: "Forbidden" });
  }
  const buses = Object.entries(sessions).map(([id, data]) => ({ id, ...data }));
  res.json(buses);
});

// --- Socket.io ---

io.on("connection", function (socket) {

  // User joins a session room
  socket.on("join-session", (sessionId) => {
    socket.join(sessionId);
    socketSessionMap[socket.id] = sessionId;
    console.log(`Socket ${socket.id} joined session ${sessionId}`);
  });

  // Sharer/Driver sends location
  socket.on("send-location", function (data) {
    const { sessionId, latitude, longitude } = data;

    if (sessions[sessionId]) {
      sessions[sessionId].active = true;
      sessions[sessionId].lastLocation = { latitude, longitude };
    }

    socket.to(sessionId).emit("receive-location", { id: socket.id, ...data });
  });

  socket.on("stop-sharing", function (sessionId) {
    console.log(`Session ${sessionId} stopped sharing`);
    socket.to(sessionId).emit("sharing-stopped");
    if (sessions[sessionId]) sessions[sessionId].active = false;
  });

  socket.on("disconnect", function () {
    console.log("User disconnected:", socket.id);
    const sessionId = socketSessionMap[socket.id];

    if (sessionId) {
      delete socketSessionMap[socket.id];
      socket.to(sessionId).emit("user-disconnected", socket.id);
    }
  });
});

const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
