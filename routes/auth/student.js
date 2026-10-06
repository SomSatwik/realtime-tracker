const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const { generateToken } = require('../../utils/auth');

// GET signup page
router.get('/signup', (req, res) => {
  res.render('auth/student-signup');
});

// POST signup
router.post('/signup', async (req, res) => {
  const { name, email, password, rollNumber } = req.body;
  try {
    const existing = await User.findOne({ email });
    if (existing) return res.render('auth/student-signup', { error: 'Email already in use' });
    const user = new User({ name, email, password, role: 'student', rollNumber });
    await user.save();
    const token = generateToken(user);
    res.cookie('token', token, { httpOnly: true, sameSite: 'strict' }).redirect('/student/dashboard');
  } catch (err) {
    console.error(err);
    res.render('auth/student-signup', { error: 'Server error' });
  }
});

// GET login page
router.get('/login', (req, res) => {
  res.render('auth/student-login');
});

// POST login
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const user = await User.findOne({ email, role: 'student' });
    if (!user) return res.render('auth/student-login', { error: 'Invalid credentials' });
    const valid = await user.comparePassword(password);
    if (!valid) return res.render('auth/student-login', { error: 'Invalid credentials' });
    const token = generateToken(user);
    res.cookie('token', token, { httpOnly: true, sameSite: 'strict' }).redirect('/student/dashboard');
  } catch (err) {
    console.error(err);
    res.render('auth/student-login', { error: 'Server error' });
  }
});

module.exports = router;
