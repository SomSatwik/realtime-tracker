const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'fallback_secret_change_me';

function generateToken(user) {
  const payload = { id: user._id, role: user.role };
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '7d' });
}

function authenticateJWT(req, res, next) {
  const token = req.cookies?.token || req.headers.authorization?.split(' ')[1];
  if (!token) {
    // redirect to a generic login page – each role can also have its own
    return res.redirect('/auth/login');
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.user = payload; // attach to request
    next();
  } catch (err) {
    console.error('JWT verification error', err);
    return res.clearCookie('token').redirect('/auth/login');
  }
}

function authorizeRoles(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      return res.status(403).render('error', { message: 'Forbidden' });
    }
    next();
  };
}

module.exports = { generateToken, authenticateJWT, authorizeRoles };
