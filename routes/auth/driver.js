const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const { generateToken } = require('../../utils/auth');

// GET signup page for drivers
router.get('/signup', (req, res) => {
  res.render('auth/driver-signup');
});

// POST signup – driver fields: name, email, password, phone, vehicle, routeInfo
router.post('/signup', async (req, res) => {
  const { name, email, password, phone, vehicle, routeInfo } = req.body;
  try {
    const existing = await User.findOne({ email });
    if (existing) return res.render('auth/driver-signup', { error: 'Email already in use' });
    const user = new User({ name, email, password, role: 'driver', phone, vehicle, routeInfo });
    await user.save();
    const token = generateToken(user);
    res.cookie('token', token, { httpOnly: true, sameSite: 'strict' }).redirect('/driver/dashboard');
  } catch (err) {
    console.error(err);
    res.render('auth/driver-signup', { error: 'Server error' });
  }
});

// GET login page for drivers
router.get('/login', (req, res) => {
  res.render('auth/driver-login');
});

// POST login – driver
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const user = await User.findOne({ email, role: 'driver' });
    if (!user) return res.render('auth/driver-login', { error: 'Invalid credentials' });
    const valid = await user.comparePassword(password);
    if (!valid) return res.render('auth/driver-login', { error: 'Invalid credentials' });
    const token = generateToken(user);
    res.cookie('token', token, { httpOnly: true, sameSite: 'strict' }).redirect('/driver/dashboard');
  } catch (err) {
    console.error(err);
    res.render('auth/driver-login', { error: 'Server error' });
  }
});

module.exports = router;
