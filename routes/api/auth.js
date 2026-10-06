const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const { generateToken } = require('../../utils/auth');

router.post('/login', async (req, res) => {
    try {
        const { email, password, role } = req.body;
        const user = await User.findOne({ email, role });
        
        if (!user || !(await user.comparePassword(password))) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }
        
        const token = generateToken(user);
        const userData = {
            id: user._id,
            name: user.name,
            email: user.email,
            role: user.role,
            rollNumber: user.rollNumber,
            phone: user.phone,
            vehicle: user.vehicle,
            routeInfo: user.routeInfo
        };
        
        res.json({ token, user: userData });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/signup', async (req, res) => {
    try {
        const { name, email, password, role, rollNumber, phone, vehicle, routeInfo } = req.body;
        
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(409).json({ error: 'Email already in use' });
        }
        
        const user = new User({
            name, email, password, role, rollNumber, phone, vehicle, routeInfo
        });
        await user.save();
        
        const token = generateToken(user);
        const userData = {
            id: user._id,
            name: user.name,
            email: user.email,
            role: user.role,
            rollNumber: user.rollNumber,
            phone: user.phone,
            vehicle: user.vehicle,
            routeInfo: user.routeInfo
        };
        
        res.json({ token, user: userData });
    } catch (err) {
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
