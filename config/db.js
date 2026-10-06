const mongoose = require('mongoose');

module.exports = async function connectDB() {
  const uri = process.env.MONGODB_URI || process.env.MONGO_URL;
  if (!uri) {
    throw new Error('MONGODB_URI (or MONGO_URL) not defined in environment');
  }
  await mongoose.connect(uri);
  console.log('🗄️  Connected to MongoDB');
};
