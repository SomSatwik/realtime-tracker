const express = require('express');
const router = express.Router();
const { authenticateJWT, authorizeRoles } = require('../../utils/auth');
const User = require('../../models/User');
const TrackingSession = require('../../models/TrackingSession');

// Admin dashboard – list users and active sessions
router.get('/dashboard', authenticateJWT, authorizeRoles('admin'), async (req, res) => {
  const users = await User.find().select('-password'); // omit passwords
  const sessions = await TrackingSession.find().populate('driver', 'name email role');
  res.render('admin/dashboard', { admin: req.user, users, sessions });
});

// Deactivate (delete) a user – POST for safety
router.post('/users/:id/delete', authenticateJWT, authorizeRoles('admin'), async (req, res) => {
  const { id } = req.params;
  await User.findByIdAndDelete(id);
  res.redirect('/admin/dashboard');
});

// End a tracking session early
router.post('/sessions/:id/end', authenticateJWT, authorizeRoles('admin'), async (req, res) => {
  const { id } = req.params;
  const session = await TrackingSession.findByIdAndUpdate(id, { status: 'ended', endedAt: new Date() }, { new: true });
  // Notify any viewers via Socket.IO
  const io = req.app.get('io');
  if (session) io.to(session.socketRoomId).emit('sharing-stopped');
  res.redirect('/admin/dashboard');
});

module.exports = router;
