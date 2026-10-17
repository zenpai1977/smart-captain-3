const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const DB_FILE = path.join(__dirname, 'drivers_db.json');
const MERCHANT_UPI = process.env.UPI_ID || 'kkumae221@okhdfcbank';
const MERCHANT_NAME = 'Smart Captain';

// Ensure drivers_db.json exists with a valid structure
function loadDatabase() {
  try {
    if (!fs.existsSync(DB_FILE)) {
      const initialData = { drivers: {}, usedUtrs: [] };
      fs.writeFileSync(DB_FILE, JSON.stringify(initialData, null, 2));
      return initialData;
    }
    const raw = fs.readFileSync(DB_FILE, 'utf8');
    const parsed = JSON.parse(raw);
    parsed.drivers = parsed.drivers || {};
    parsed.usedUtrs = parsed.usedUtrs || [];
    return parsed;
  } catch (err) {
    return { drivers: {}, usedUtrs: [] };
  }
}

function saveDatabase(data) {
  fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2));
}

const htmlPage = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>smart captain — Apni pasand ki ride</title>
  <style>
    body {
      background-color: #0d1017;
      color: #ffffff;
      font-family: Arial, sans-serif;
      margin: 0;
      padding: 16px;
      display: flex;
      justify-content: center;
    }
    .container {
      max-width: 600px;
      width: 100%;
    }
    .card {
      background-color: #161b22;
      border: 1px solid #d4af37;
      border-radius: 12px;
      padding: 18px;
      margin-bottom: 18px;
    }
    .version-tag {
      font-size: 12px;
      color: #c9d1d9;
      margin-bottom: 6px;
    }
    h1 {
      font-size: 24px;
      margin: 4px 0 10px 0;
    }
    h2 {
      font-size: 18px;
      margin-top: 0;
      margin-bottom: 14px;
      color: #f0f6fc;
    }
    p.sub {
      font-size: 13px;
      color: #b1bac4;
      margin: 0;
    }
    .row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 0;
      border-bottom: 1px solid #30363d;
      font-size: 14px;
    }
    .row input[type="number"], .row input[type="text"] {
      background: #0d1117;
      border: 1px solid #8b949e;
      color: #fff;
      padding: 6px 10px;
      border-radius: 6px;
      width: 90px;
      text-align: center;
    }
    .row input.wide-input {
      width: 170px;
      color: #4caf50;
      border-color: #4caf50;
    }
    .field-label {
      display: block;
      font-size: 13px;
      color: #c9d1d9;
      margin-top: 12px;
      margin-bottom: 4px;
    }
    .full-input, select {
      width: 100%;
      box-sizing: border-box;
      padding: 10px;
      border-radius: 8px;
      border: 1px solid #8b949e;
      background: #0d1117;
      color: #fff;
      font-size: 14px;
    }
    .gpay-btn {
      display: block;
      text-align: center;
      background: #1a73e8;
      color: #ffffff;
      padding: 13px;
      border-radius: 8px;
      text-decoration: none;
      font-weight: bold;
      margin-top: 14px;
      font-size: 15px;
    }
    .qr-box {
      text-align: center;
      background: #0d1117;
      border: 1px dashed #30363d;
      border-radius: 8px;
      padding: 12px;
      margin-top: 12px;
    }
    .qr-box img {
      width: 150px;
      height: 150px;
      border-radius: 8px;
      background: #fff;
      padding: 6px;
    }
    .upi-copy {
      font-size: 13px;
      color: #58a6ff;
      margin-top: 6px;
      word-break: break-all;
    }
    .verify-btn {
      width: 100%;
      background: #238636;
      color: #ffffff;
      padding: 13px;
      border: none;
      border-radius: 8px;
      font-weight: bold;
      font-size: 15px;
      cursor: pointer;
      margin-top: 14px;
    }
    .status-msg {
      margin-top: 12px;
      font-size: 14px;
      font-weight: bold;
      text-align: center;
    }
  </style>
</head>
<body>
  <div class="container">
    <!-- Top Banner -->
    <div class="card">
      <div class="version-tag">Version 5.26.1 • Pehle 10 rides bilkul free</div>
      <h1>smart captain — Apni pasand ki ride. Apne rules par.</h1>
      <p class="sub">Ghaate wali rides chhodo, kamai wali uthao. Rapido, Uber aur Porter ki bookings apne rules par filter karo.</p>
    </div>

    <!-- 1. Ride Filter Rules -->
    <div class="card">
      <h2>1. Aapke Ride Filter Rules (Demo Control)</h2>
      <div class="row">
        <span>Bike / Auto / Cab / Parcel Switch</span>
        <input type="checkbox" id="serviceSwitch" checked>
      </div>
      <div class="row">
        <span>Minimum Trip Doori (km)</span>
        <input type="number" id="minTripKm" value="5" step="0.5">
      </div>
      <div class="row">
        <span>Max Pickup Doori (0.5 - 12 km)</span>
        <input type="number" id="maxPickupKm" value="2" step="0.5">
      </div>
      <div class="row">
        <span>Minimum Kamai (₹ Fare)</span>
        <input type="number" id="minFare" value="60">
      </div>
      <div class="row">
        <span>Go To (Ghar / Ilaaka)</span>
        <input type="text" id="goToArea" class="wide-input" placeholder="e.g. Connaught Place">
      </div>
    </div>

    <!-- 2. Direct GPay Payment & Safety Filter -->
    <div class="card">
      <h2>2. Direct GPay Pass & Rule Activation</h2>

      <label class="field-label">Driver Mobile Number (10 Digits):</label>
      <input type="text" id="driverPhone" class="full-input" placeholder="e.g. 9876543210" maxlength="10">

      <label class="field-label">Select Pass Plan:</label>
      <select id="planSelect" onchange="updatePaymentUI()">
        <option value="9.00" data-plan="1 Day Pass (₹9)" data-days="1">1 Day Pass — ₹9 (Unlimited Rides)</option>
        <option value="49.00" data-plan="7 Days Pass (₹49)" data-days="7">7 Days Pass — ₹49 (Unlimited Rides)</option>
        <option value="199.00" data-plan="30 Days Pass (₹199)" data-days="30">30 Days Pro Pass — ₹199 (Unlimited Rides)</option>
        <option value="0.00" data-plan="Free Trial (10 Rides)" data-days="1">Free Trial — ₹0 (First 10 Rides Free)</option>
      </select>

      <div id="paymentSection">
        <a id="gpayLink" class="gpay-btn" href="upi://pay?pa=${MERCHANT_UPI}&pn=Smart%20Captain&am=9.00&cu=INR&tn=Smart%20Captain%20Pass">
          Tap to Pay ₹9 Directly on GPay / UPI App
        </a>

        <div class="qr-box">
          <div style="font-size: 12px; color: #8b949e; margin-bottom: 8px;">Or Scan QR from GPay / PhonePe / Paytm:</div>
          <img id="upiQrImage" src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=upi%3A%2F%2Fpay%3Fpa%3D${encodeURIComponent(MERCHANT_UPI)}%26pn%3DSmart%2520Captain%26am%3D9.00%26cu%3DINR" alt="UPI QR Code">
          <div class="upi-copy">UPI ID: <strong>${MERCHANT_UPI}</strong></div>
        </div>

        <label class="field-label">Safety Check — Enter 12-Digit UPI Ref / UTR No. from GPay:</label>
        <input type="text" id="utrInput" class="full-input" placeholder="12-digit UTR number (e.g. 429112345678)" maxlength="12">
      </div>

      <button class="verify-btn" onclick="activateDriverPass()">Save Rules & Activate Pass</button>
      <div id="statusMsg" class="status-msg"></div>
    </div>
  </div>

  <script>
    const UPI_ID = "${MERCHANT_UPI}";

    function updatePaymentUI() {
      const select = document.getElementById('planSelect');
      const amount = select.value;
      const paymentSection = document.getElementById('paymentSection');

      if (amount === "0.00") {
        paymentSection.style.display = "none";
        return;
      }

      paymentSection.style.display = "block";
      const upiUri = \`upi://pay?pa=\${UPI_ID}&pn=Smart%20Captain&am=\${amount}&cu=INR&tn=Smart%20Captain%20Pass\`;
      const gpayLink = document.getElementById('gpayLink');
      gpayLink.href = upiUri;
      gpayLink.innerText = \`Tap to Pay ₹\${parseInt(amount)} Directly on GPay / UPI App\`;

      const qrImg = document.getElementById('upiQrImage');
      qrImg.src = \`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=\${encodeURIComponent(upiUri)}\`;
    }

    async function activateDriverPass() {
      const phone = document.getElementById('driverPhone').value.trim();
      const select = document.getElementById('planSelect');
      const amount = select.value;
      const planName = select.options[select.selectedIndex].getAttribute('data-plan');
      const days = parseInt(select.options[select.selectedIndex].getAttribute('data-days'), 10);
      const utr = document.getElementById('utrInput').value.trim();
      const statusEl = document.getElementById('statusMsg');

      const rules = {
        activeSwitch: document.getElementById('serviceSwitch').checked,
        minTripKm: parseFloat(document.getElementById('minTripKm').value) || 5,
        maxPickupKm: parseFloat(document.getElementById('maxPickupKm').value) || 2,
        minFare: parseInt(document.getElementById('minFare').value, 10) || 60,
        goToArea: document.getElementById('goToArea').value.trim()
      };

      if (!/^[6-9]\\d{9}$/.test(phone)) {
        statusEl.style.color = "#ff6b6b";
        statusEl.innerText = "Error: Enter a valid 10-digit Indian mobile number.";
        return;
      }

      if (amount !== "0.00" && !/^\\d{12}$/.test(utr)) {
        statusEl.style.color = "#ff6b6b";
        statusEl.innerText = "Safety Filter: Enter a valid 12-digit GPay UPI Ref / UTR number.";
        return;
      }

      statusEl.style.color = "#58a6ff";
      statusEl.innerText = "Verifying and saving...";

      try {
        const response = await fetch('/api/activate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone, amount, planName, days, utr, rules })
        });
        const result = await response.json();
        statusEl.style.color = result.success ? "#4caf50" : "#ff6b6b";
        statusEl.innerText = result.message;
      } catch (err) {
        statusEl.style.color = "#ff6b6b";
        statusEl.innerText = "Network error. Please try again.";
      }
    }
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  // Enable CORS so the Android app can fetch rules from mobile data
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    return res.end();
  }

  // 1. Serve Main Dashboard
  if (req.method === 'GET' && (req.url === '/' || req.url === '/index.html')) {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    return res.end(htmlPage);
  }

  // 2. Verify GPay UTR & Save Driver Rules
  if (req.method === 'POST' && req.url === '/api/activate') {
    let body = '';
    req.on('data', chunk => { body += chunk.toString(); });
    req.on('end', () => {
      try {
        const { phone, amount, planName, days, utr, rules } = JSON.parse(body);

        if (!/^[6-9]\d{9}$/.test(phone)) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ success: false, message: 'Invalid 10-digit mobile number.' }));
        }

        const db = loadDatabase();
        const isPaidPlan = amount !== "0.00";

        if (isPaidPlan) {
          // Strict 12-digit UTR check & anti-replay check
          if (!/^\d{12}$/.test(utr) \vert{}\vert{} /^(\d)\1{11}$/.test(utr)) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify({ success: false, message: 'Invalid 12-digit UTR number.' }));
          }
          if (db.usedUtrs.includes(utr)) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify({ success: false, message: 'This UTR number has already been used!' }));
          }
          db.usedUtrs.push(utr);
        } else {
          // Prevent infinite free trials on the same phone number
          if (db.drivers[phone] && db.drivers[phone].trialUsed) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify({ success: false, message: 'Free trial already used on this number. Please select a ₹9 or ₹49 pass.' }));
          }
        }

        const validTillDate = new Date(Date.now() + (days || 1) * 24 * 60 * 60 * 1000).toLocaleDateString('en-IN');

        db.drivers[phone] = {
          phone: phone,
          plan: `${planName} - Valid till ${validTillDate}`,
          freeRides: isPaidPlan ? 'Unlimited' : 10,
          trialUsed: true,
          utr: isPaidPlan ? utr : 'FREE_TRIAL',
          rules: rules,
          updatedAt: new Date().toISOString()
        };

        saveDatabase(db);

        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          success: true,
          message: `Active! ${planName} (Valid till ${validTillDate}) & Ride Rules Saved.`
        }));
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: false, message: 'Server error saving driver data.' }));
      }
    });
    return;
  }

  // 3. Android App Sync Endpoint (GET /api/driver?phone=9876543210)
  if (req.method === 'GET' && req.url.startsWith('/api/driver')) {
    const urlObj = new URL(req.url, `http://${req.headers.host}`);
    const phone = urlObj.searchParams.get('phone');
    const db = loadDatabase();

    if (!phone || !db.drivers[phone]) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ active: false, message: 'Driver not found or no active plan.' }));
    }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ active: true, driver: db.drivers[phone] }));
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`SMART CAPTAIN CLOUD SERVER IS LIVE ON PORT ${PORT}!`);
});