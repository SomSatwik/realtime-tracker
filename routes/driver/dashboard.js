const express = require('express');
const router = express.Router();
const { authenticateJWT, authorizeRoles } = require('../../utils/auth');
const TrackingSession = require('../../models/TrackingSession');

// GET driver dashboard – show active session and controls
router.get('/dashboard', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
  const active = await TrackingSession.findOne({ driver: req.user.id, status: 'active' });
  res.render('driver/dashboard', { user: req.user, activeSession: active });
});

// POST start sharing – creates a new tracking session
router.post('/start', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
  try {
    const driverId = req.user.id;
    const { routeInfo } = req.body;
    const roomId = `room-${driverId}-${Date.now()}`;
    const session = new TrackingSession({ driver: driverId, routeInfo, socketRoomId: roomId });
    await session.save();
    // Keep compatibility with existing socket.io logic by adding to in‑memory store
    const sessions = req.app.get('sessions') || {};
    sessions[roomId] = { driverId, mobile: routeInfo || 'Driver Bus', active: false, lastLocation: null };
    res.redirect(`/driver/${roomId}`);
  } catch (err) {
    console.error('Error starting session:', err);
    res.redirect('/driver/dashboard');
  }
});

// POST stop sharing – ends the active session and notifies viewers
router.post('/stop', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
  const driverId = req.user.id;
  const session = await TrackingSession.findOneAndUpdate(
    { driver: driverId, status: 'active' },
    { status: 'ended', endedAt: new Date() },
    { new: true }
  );
  if (session) {
    const io = req.app.get('io');
    io.to(session.socketRoomId).emit('sharing-stopped');
    const sessions = req.app.get('sessions') || {};
    if (sessions[session.socketRoomId]) {
      sessions[session.socketRoomId].active = false;
    }
  }
  res.redirect('/driver/dashboard');
});

module.exports = router;
