const mongoose = require('mongoose');

const trackingSessionSchema = new mongoose.Schema({
  driver: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  routeInfo: String,
  status: { type: String, enum: ['active', 'ended'], default: 'active' },
  socketRoomId: { type: String, required: true, unique: true },
  createdAt: { type: Date, default: Date.now },
  endedAt: Date,
});

module.exports = mongoose.model('TrackingSession', trackingSessionSchema);
