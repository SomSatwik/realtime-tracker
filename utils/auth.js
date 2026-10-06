const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'fallback_secret_change_me';

function generateToken(user) {
  const payload = { id: user._id, role: user.role };
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '7d' });
}

function authenticateJWT(req, res, next) {
  const token = req.cookies?.token || req.headers.authorization?.split(' ')[1];
  if (!token) {
    // Determine which role login to redirect to based on requested path
    const path = req.path;
    let loginPath = '/auth/student/login'; // default
    if (path.startsWith('/driver')) loginPath = '/auth/driver/login';
    else if (path.startsWith('/admin')) loginPath = '/auth/admin/login';
    else if (path.startsWith('/student')) loginPath = '/auth/student/login';
    return res.redirect(loginPath);
  }
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.user = payload; // attach to request
    next();
  } catch (err) {
    console.error('JWT verification error', err);
    // Clear invalid token and redirect to appropriate login
    const path = req.path;
    let loginPath = '/auth/student/login';
    if (path.startsWith('/driver')) loginPath = '/auth/driver/login';
    else if (path.startsWith('/admin')) loginPath = '/auth/admin/login';
    else if (path.startsWith('/student')) loginPath = '/auth/student/login';
    return res.clearCookie('token').redirect(loginPath);
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
