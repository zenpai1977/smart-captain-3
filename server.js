const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const DB_FILE = path.join(__dirname, 'drivers_db.json');

// Initialize local database file if it doesn't exist
if (!fs.existsSync(DB_FILE)) {
  fs.writeFileSync(DB_FILE, JSON.stringify({ drivers: {} }, null, 2));
}

function getDB() {
  return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
}

function saveDB(data) {
  fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2));
}

const htmlPage = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>smart captain - Apni Pasand Ki Ride, Apne Rules Par</title>
  <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
  <style>
    body { font-family: Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
    .container { max-width: 800px; margin: auto; }
    .hero { background: #1e293b; padding: 24px; border-radius: 12px; border: 1px solid #334155; margin-bottom: 20px; }
    h1 { color: #38bdf8; margin-top: 0; }
    .badge { background: #22c55e; color: #000; padding: 4px 10px; border-radius: 20px; font-weight: bold; font-size: 13px; }
    .card { background: #1e293b; padding: 20px; border-radius: 12px; border: 1px solid #334155; margin-bottom: 20px; }
    .plans { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
    .plan { background: #0f172a; padding: 18px; border-radius: 10px; border: 2px solid #38bdf8; text-align: center; }
    .price { font-size: 32px; font-weight: bold; color: #4ade80; margin: 10px 0; }
    button { background: #38bdf8; color: #0f172a; border: none; padding: 10px 18px; font-weight: bold; border-radius: 8px; cursor: pointer; width: 100%; font-size: 15px; }
    button:hover { background: #0284c7; color: #fff; }
    input, select { padding: 10px; border-radius: 6px; border: 1px solid #475569; background: #0f172a; color: #fff; width: 100%; box-sizing: border-box; margin-bottom: 12px; }
    .rule-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #334155; }
    #statusBox { margin-top: 12px; padding: 12px; border-radius: 8px; background: #064e3b; display: none; }
  </style>
</head>
<body>
  <div class="container">
    <div class="hero">
      <span class="badge">Version 5.28.1 · Pehle 10 rides bilkul free</span>
      <h1>smart captain — Apni pasand ki ride. Apne rules par.</h1>
      <p>Ghaate wali rides chhodo, kamai wali uthao. Rapido, Uber aur Porter ki bookings apne rules par filter karo.</p>
    </div>

    <div class="card">
      <h2>1. Aapke Ride Filter Rules (Demo Control)</h2>
      <div class="rule-row"><span>Bike / Auto / Cab / Parcel Switch</span><input type="checkbox" checked style="width:auto;"></div>
      <div class="rule-row"><span>Minimum Trip Doori (km)</span><input type="number" value="5" style="width:80px; margin:0;"></div>
      <div class="rule-row"><span>Max Pickup Doori (0.5 - 12 km)</span><input type="number" value="2" style="width:80px; margin:0;"></div>
      <div class="rule-row"><span>Minimum Kamai (₹ Fare)</span><input type="number" value="60" style="width:80px; margin:0;"></div>
      <div class="rule-row"><span>Go To (Ghar / Ilaaka)</span><input type="text" placeholder="e.g. Connaught Place" style="width:180px; margin:0;"></div>
    </div>

    <div class="card">
      <h2>2. Driver Login & Cheaper Subscription Plans</h2>
      <label>Apna Mobile Number Daalein:</label>
      <input type="text" id="phone" placeholder="9876543210">
      <button onclick="checkStatus()" style="margin-bottom:15px; background:#64748b; color:#fff;">Check My Plan / Free Trial</button>
      <div id="statusBox"></div>

      <div class="plans" style="margin-top:15px;">
        <div class="plan">
          <h3>1 Day Pass</h3>
          <div class="price">₹9</div>
          <p>1 Day Validity</p>
          <button onclick="buyPlan('1 Day Pass', 9, 1)">Buy ₹9 Plan</button>
        </div>
        <div class="plan">
          <h3>1 Week Popular</h3>
          <div class="price">₹49</div>
          <p>7 Days Validity</p>
          <button onclick="buyPlan('1 Week Popular', 49, 7)">Buy ₹49 Plan</button>
        </div>
        <div class="plan">
          <h3>1 Month Pro</h3>
          <div class="price">₹199</div>
          <p>30 Days Validity</p>
          <button onclick="buyPlan('1 Month Pro', 199, 30)">Buy ₹199 Plan</button>
        </div>
      </div>
    </div>
  </div>

  <script>
    async function checkStatus() {
      const phone = document.getElementById('phone').value.trim();
      if (!phone) return alert('Pehle mobile number daalein!');
      const res = await fetch('/api/status?phone=' + phone);
      const data = await res.json();
      const box = document.getElementById('statusBox');
      box.style.display = 'block';
      box.innerHTML = '<b>Phone:</b> ' + data.phone + ' | <b>Plan:</b> ' + data.plan + ' | <b>Free Rides Left:</b> ' + data.freeRides;
    }

    async function buyPlan(planName, amount, days) {
      const phone = document.getElementById('phone').value.trim();
      if (!phone) return alert('Pehle apna mobile number upar box me daalein!');

      // Simulated Instant UPI / Gateway Checkout (Replace key with live Razorpay key_id later)
      const confirmPay = confirm('Pay ₹' + amount + ' via UPI / Payment Gateway for ' + planName + ' on number ' + phone + '?');
      if (!confirmPay) return;

      const res = await fetch('/api/activate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, planName, amount, days })
      });
      const data = await res.json();
      alert('Payment Successful! ' + data.message);
      checkStatus();
    }
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(htmlPage);
  } else if (req.method === 'GET' && req.url.startsWith('/api/status')) {
    const urlObj = new URL(req.url, `http://${req.headers.host}`);
    const phone = urlObj.searchParams.get('phone');
    const db = getDB();
    if (!db.drivers[phone]) {
      db.drivers[phone] = { phone, plan: 'Free Trial (Active)', freeRides: 10, expiresAt: null };
      saveDB(db);
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(db.drivers[phone]));
  } else if (req.method === 'POST' && req.url === '/api/activate') {
    let body = '';
    req.on('data', chunk => body += chunk.toString());
    req.on('end', () => {
      const { phone, planName, amount, days } = JSON.parse(body);
      const db = getDB();
      const expiry = new Date(Date.now() + days * 86400000).toLocaleDateString();
      db.drivers[phone] = {
        phone,
        plan: `${planName} (₹${amount}) - Valid till ${expiry}`,
        freeRides: 'Unlimited',
        expiresAt: expiry
      };
      saveDB(db);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, message: `${planName} activated till ${expiry}!` }));
    });
  } else {
    res.writeHead(404);
    res.end('Not Found');
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('SMART CAPTAIN CLOUD SERVER IS LIVE!');
});