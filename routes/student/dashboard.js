const express = require('express');
const router = express.Router();
const { authenticateJWT, authorizeRoles } = require('../../utils/auth');
const TrackingSession = require('../../models/TrackingSession');

// Student dashboard – list all active driver sessions
router.get('/dashboard', authenticateJWT, authorizeRoles('student'), async (req, res) => {
  const activeSessions = await TrackingSession.find({ status: 'active' }).populate('driver', 'name vehicle');
  res.render('student/dashboard', { user: req.user, sessions: activeSessions });
});

module.exports = router;
