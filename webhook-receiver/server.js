const express = require('express');
const bodyParser = require('body-parser');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 9000;

// Explicitly use body-parser for JSON
app.use(bodyParser.json());

// Store received webhooks in memory (for viewing later)
const receivedWebhooks = [];

// Middleware to parse JSON
app.use(express.json());

// Endpoint to receive webhooks
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-payflow-signature'] || req.headers['stripe-signature'] || 'none';
  const timestamp = req.headers['x-payflow-timestamp'] || 'none';

  const webhook = {
    id: `wh_${Date.now()}`,
    receivedAt: new Date().toISOString(),
    headers: {
      signature,
      timestamp,
      contentType: req.headers['content-type'],
    },
    body: req.body,
  };

  receivedWebhooks.unshift(webhook);

  // Keep only last 100 webhooks
  if (receivedWebhooks.length > 100) {
    receivedWebhooks.pop();
  }

  // Log to console with nice formatting
  console.log('\n═══════════════════════════════════════════════════');
  console.log('📥 WEBHOOK RECEIVED');
  console.log('═══════════════════════════════════════════════════');
  console.log(`Time: ${webhook.receivedAt}`);
  console.log(`Signature: ${signature}`);
  console.log(`Timestamp: ${timestamp}`);
  console.log('\nPayload:');
  console.log(JSON.stringify(req.body, null, 2));
  console.log('═══════════════════════════════════════════════════\n');

  // Always return 200 OK to indicate successful receipt
  res.status(200).json({ received: true });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', receivedCount: receivedWebhooks.length });
});

// View all received webhooks
app.get('/webhooks', (req, res) => {
  res.json({ webhooks: receivedWebhooks });
});

// View latest webhook
app.get('/webhook/latest', (req, res) => {
  if (receivedWebhooks.length === 0) {
    return res.status(404).json({ error: 'No webhooks received yet' });
  }
  res.json(receivedWebhooks[0]);
});

// Clear all webhooks
app.delete('/webhooks', (req, res) => {
  receivedWebhooks.length = 0;
  res.json({ cleared: true });
});

app.listen(PORT, () => {
  console.log(`🚀 Webhook receiver listening on http://localhost:${PORT}`);
  console.log(`   - POST /webhook   → Receive webhooks`);
  console.log(`   - GET  /webhooks  → View all received`);
  console.log(`   - GET  /webhook/latest → View latest`);
  console.log(`   - DEL /webhooks   → Clear all`);
});