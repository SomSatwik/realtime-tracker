const mongoose = require('mongoose');

module.exports = async function connectDB() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    throw new Error('MONGODB_URI not defined in environment');
  }
  await // AFTER (Fixed)
mongoose.connect(process.env.MONGODB_URI);
  console.log('🗄️  Connected to MongoDB');
};
