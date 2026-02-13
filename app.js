require('dotenv').config();
const express = require('express');
const app = express();
const path = require("path");
const http = require("http");
const socketio = require("socket.io");
const { v4: uuidv4 } = require('uuid');

// Use in-memory store for demo simplicity. In production, use Redis/MongoDB.
const sessions = {};

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
  // In a real app, we'd verify the mobile number or send an SMS.
  // Here, we just generate a session ID.
  const sessionId = uuidv4();
  sessions[sessionId] = { mobile, active: false };
  res.json({ sessionId });
});

// 3. Sharer Interface (Mobile)
app.get("/share/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("share", { sessionId });
});

// 4. Viewer Interface (Map)
app.get("/track/:id", (req, res) => {
  const sessionId = req.params.id;
  if (!sessions[sessionId]) {
    return res.status(404).send("Session not found");
  }
  res.render("track", { sessionId });
});

// --- Socket.io ---

io.on("connection", function (socket) {

  // User joins a session room
  socket.on("join-session", (sessionId) => {
    socket.join(sessionId);
    console.log(`Socket ${socket.id} joined session ${sessionId}`);
  });

  // Sharer sends location
  socket.on("send-location", function (data) {
    const { sessionId, latitude, longitude } = data;

    if (sessions[sessionId]) {
      sessions[sessionId].active = true;
      sessions[sessionId].lastLocation = { latitude, longitude };
    }

    // Broadcast to everyone in the room (including the sender, though usually sender doesn't need it)
    // Better: Broadcast to others in the room
    socket.to(sessionId).emit("receive-location", { id: socket.id, ...data });
  });

  socket.on("stop-sharing", function (sessionId) {
    console.log(`Session ${sessionId} stopped sharing`);
    socket.to(sessionId).emit("sharing-stopped");
    if (sessions[sessionId]) sessions[sessionId].active = false;
  });

  socket.on("disconnect", function () {
    console.log("User disconnected");
    // Could handle clean up/notify disconnect if we mapped socket.id to session
  });
});

const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
