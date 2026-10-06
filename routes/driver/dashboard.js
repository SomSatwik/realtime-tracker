const express = require('express');
const router = express.Router();
const { authenticateJWT, authorizeRoles } = require('../../utils/auth');
const TrackingSession = require('../../models/TrackingSession');

// Driver dashboard – list current active session (if any)
router.get('/dashboard', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
  const active = await TrackingSession.findOne({ driver: req.user.id, status: 'active' });
  res.render('driver/dashboard', { user: req.user, activeSession: active });
});

// Start sharing – creates a new tracking session and returns room ID (JSON for client)
router.post('/start', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
  const driverId = req.user.id;
  const { routeInfo } = req.body;
  const roomId = `room-${driverId}-${Date.now()}`; // unique room name
  const session = new TrackingSession({
    driver: driverId,
    routeInfo,
    socketRoomId: roomId,
  });
  await session.save();
  res.json({ sessionId: session._id, roomId });
});

// Stop sharing – mark session ended
router.post('/stop', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
  const driverId = req.user.id;
  const session = await TrackingSession.findOneAndUpdate(
    { driver: driverId, status: 'active' },
    { status: 'ended', endedAt: new Date() },
    { new: true }
  );
  if (!session) return res.status(404).send('No active session');
  // Notify viewers via Socket.IO
  const io = req.app.get('io');
  io.to(session.socketRoomId).emit('sharing-stopped');
  res.redirect('/driver/dashboard');
});

module.exports = router;
