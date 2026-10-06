const express = require('express');
const router = express.Router();
const TrackingSession = require('../../models/TrackingSession');
const { authenticateJWT, authorizeRoles } = require('../../utils/auth');

router.get('/active', authenticateJWT, async (req, res) => {
    try {
        const sessions = await TrackingSession.find({ status: 'active' }).populate('driver', 'name vehicle');
        res.json({ sessions });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

router.get('/driver/status', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
    try {
        const activeSession = await TrackingSession.findOne({ driver: req.user.id, status: 'active' });
        res.json({ activeSession });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/driver/start', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
    try {
        const { routeInfo } = req.body;
        const existingSession = await TrackingSession.findOne({ driver: req.user.id, status: 'active' });
        if (existingSession) {
            return res.status(400).json({ error: 'Driver already has an active session' });
        }
        
        const roomId = `room-${req.user.id}-${Date.now()}`;
        const session = new TrackingSession({
            driver: req.user.id,
            routeInfo: routeInfo || req.user.routeInfo,
            socketRoomId: roomId,
            status: 'active'
        });
        await session.save();
        
        const sessions = req.app.get('sessions');
        sessions[roomId] = { mobile: session.routeInfo, active: true };
        
        res.json({ session });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/driver/stop', authenticateJWT, authorizeRoles('driver'), async (req, res) => {
    try {
        const session = await TrackingSession.findOne({ driver: req.user.id, status: 'active' });
        if (!session) {
            return res.status(404).json({ error: 'No active session found' });
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
