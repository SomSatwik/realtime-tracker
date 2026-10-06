// scripts/seedAdmin.js
/**
 * Simple script to create an admin user in MongoDB.
 * Run with: `node scripts/seedAdmin.js`
 * Make sure the .env file contains MONGODB_URI and JWT_SECRET.
 */
require('dotenv').config();
const mongoose = require('mongoose');
const User = require('../models/User');

async function createAdmin() {
  const uri = process.env.MONGODB_URI || process.env.MONGO_URL;
  if (!uri) {
    console.error('MONGODB_URI (or MONGO_URL) not defined in .env');
    process.exit(1);
  }
  await mongoose.connect(uri);
  const existing = await User.findOne({ email: 'admin@example.com', role: 'admin' });
  if (existing) {
    console.log('Admin user already exists:', existing.email);
    process.exit(0);
  }
  const admin = new User({
    name: 'Admin User',
    email: 'admin@example.com',
    password: 'ChangeMe123!', // will be hashed by pre‑save hook
    role: 'admin'
  });
  await admin.save();
  console.log('Admin user created with email admin@example.com and password ChangeMe123!');
  process.exit(0);
}

createAdmin().catch(err => {
  console.error('Error creating admin:', err);
  process.exit(1);
});
