const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const TrackingSession = require('../../models/TrackingSession');
const { authenticateJWT, authorizeRoles } = require('../../utils/auth');

router.get('/dashboard', authenticateJWT, authorizeRoles('admin'), async (req, res) => {
    try {
        const users = await User.find().select('-password');
        const sessions = await TrackingSession.find().populate('driver', 'name vehicle');
        res.json({ users, sessions });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/users/:id/delete', authenticateJWT, authorizeRoles('admin'), async (req, res) => {
    try {
        await User.findByIdAndDelete(req.params.id);
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/sessions/:id/end', authenticateJWT, authorizeRoles('admin'), async (req, res) => {
    try {
        const session = await TrackingSession.findById(req.params.id);
        if (!session) {
            return res.status(404).json({ error: 'Session not found' });
        }
        
        session.status = 'ended';
        session.endedAt = new Date();
        await session.save();
        
        const io = req.app.get('io');
        io.to(session.socketRoomId).emit('sharing-stopped');
        
        const sessions = req.app.get('sessions');
        if (sessions[session.socketRoomId]) {
            sessions[session.socketRoomId].active = false;
        }
        
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
