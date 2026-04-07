require('dotenv').config();
const express = require('express');
const app = express();
const path = require("path");
const http = require("http");
const socketio = require("socket.io");
const { v4: uuidv4 } = require('uuid');

// Use in-memory store for demo simplicity. In production, use Redis/MongoDB.
const sessions = {};
<<<<<<< HEAD
const socketSessionMap = {}; // Maps socket.id -> sessionId
=======
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb

const server = http.createServer(app);
const io = socketio(server);

app.set("view engine", "ejs");
app.use(express.static(path.join(__dirname, "public")));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// --- Routes ---

// 1. Landing Page
app.get("/", function (req, res) {
  res.render("index");
});

// 2. Create Session
app.post("/api/session", (req, res) => {
  const mobile = req.body.mobile;
<<<<<<< HEAD
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
=======
  // In a real app, we'd verify the mobile number or send an SMS.
  // Here, we just generate a session ID.
  const sessionId = uuidv4();
  sessions[sessionId] = { mobile, active: false };
  res.json({ sessionId });
});

// 3. Sharer Interface (Mobile)
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
app.get("/share/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("share", { sessionId });
});

<<<<<<< HEAD
// 4. Viewer Interface (Map) — EXISTING, DO NOT REMOVE
=======
// 4. Viewer Interface (Map)
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
app.get("/track/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("track", { sessionId });
});

<<<<<<< HEAD
// 5. Driver Broadcasting Page — NEW
app.get("/driver/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("driver", { sessionId, busName: sessions[sessionId].mobile });
});

// 6. Student Tracking Page — NEW
app.get("/student/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("student", { sessionId, busName: sessions[sessionId].mobile });
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

=======
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
// --- Socket.io ---

io.on("connection", function (socket) {

  // User joins a session room
  socket.on("join-session", (sessionId) => {
    socket.join(sessionId);
<<<<<<< HEAD
    socketSessionMap[socket.id] = sessionId;
    console.log(`Socket ${socket.id} joined session ${sessionId}`);
  });

  // Sharer/Driver sends location
=======
    console.log(`Socket ${socket.id} joined session ${sessionId}`);
  });

  // Sharer sends location
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
  socket.on("send-location", function (data) {
    const { sessionId, latitude, longitude } = data;

    if (sessions[sessionId]) {
      sessions[sessionId].active = true;
      sessions[sessionId].lastLocation = { latitude, longitude };
    }

<<<<<<< HEAD
=======
    // Broadcast to everyone in the room (including the sender, though usually sender doesn't need it)
    // Better: Broadcast to others in the room
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
    socket.to(sessionId).emit("receive-location", { id: socket.id, ...data });
  });

  socket.on("stop-sharing", function (sessionId) {
    console.log(`Session ${sessionId} stopped sharing`);
    socket.to(sessionId).emit("sharing-stopped");
    if (sessions[sessionId]) sessions[sessionId].active = false;
  });

  socket.on("disconnect", function () {
<<<<<<< HEAD
    console.log("User disconnected:", socket.id);
    const sessionId = socketSessionMap[socket.id];

    if (sessionId) {
      delete socketSessionMap[socket.id];
      socket.to(sessionId).emit("user-disconnected", socket.id);
    }
=======
    console.log("User disconnected");
    // Could handle clean up/notify disconnect if we mapped socket.id to session
>>>>>>> 05490fea98ab5925d5436410e1761ffbf758c3bb
  });
});

const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
