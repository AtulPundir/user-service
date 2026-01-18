const crypto = require('crypto');

// Same secret as in application.yml (must be at least 32 chars)
const secret = 'your-super-secret-jwt-key-min-32-chars';

// Create header
const header = Buffer.from(JSON.stringify({
  alg: 'HS256',
  typ: 'JWT'
})).toString('base64url');

// Create payload
const payload = Buffer.from(JSON.stringify({
  userId: 'test-user-123',
  phone: '+1234567890',
  role: 'ADMIN',
  iat: Math.floor(Date.now() / 1000),
  exp: Math.floor(Date.now() / 1000) + 86400 // 24 hours
})).toString('base64url');

// Create signature
const signature = crypto
  .createHmac('sha256', secret)
  .update(header + '.' + payload)
  .digest('base64url');

const token = header + '.' + payload + '.' + signature;
console.log('JWT Token:\n');
console.log(token);
