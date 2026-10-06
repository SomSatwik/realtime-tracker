const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const { generateToken } = require('../../utils/auth');

// Admin login page (no public signup)
router.get('/login', (req, res) => {
  res.render('auth/admin-login');
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const admin = await User.findOne({ email, role: 'admin' });
    if (!admin) return res.render('auth/admin-login', { error: 'Invalid credentials' });
    const valid = await admin.comparePassword(password);
    if (!valid) return res.render('auth/admin-login', { error: 'Invalid credentials' });
    const token = generateToken(admin);
    res.cookie('token', token, { httpOnly: true, sameSite: 'strict' }).redirect('/admin/dashboard');
  } catch (err) {
    console.error(err);
    res.render('auth/admin-login', { error: 'Server error' });
  }
});

module.exports = router;
