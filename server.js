const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Enable CORS for admin panels and app requests
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
    res.header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    next();
});

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
const supabase = createClient(supabaseUrl, supabaseKey);

// 1. Admin Panel Route at /admin
app.get('/admin', async (req, res) => {
    res.send(`
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Smart Captain Admin Panel</title>
            <style>
                body { background: #0B0E14; color: #C9D1D9; font-family: Arial, sans-serif; padding: 20px; }
                h2 { color: #D29922; }
                table { width: 100%; border-collapse: collapse; margin-top: 20px; background: #161B22; }
                th, td { padding: 12px; border: 1px solid #30363D; text-align: left; }
                th { background: #21262D; color: #8B949E; }
                button { background: #238636; color: white; border: none; padding: 6px 12px; cursor: pointer; border-radius: 4px; font-weight: bold; }
                button:hover { background: #2ea043; }
                .active { color: #3FB950; font-weight: bold; }
                .pending { color: #D29922; font-weight: bold; }
            </style>
        </head>
        <body>
            <h2>Smart Captain – Supabase Admin Panel</h2>
            <p>Verify UTR numbers from your GPay app, then click approve below:</p>
            <button onclick="loadRequests()">Refresh Payment Requests</button>
            <table>
                <thead>
                    <tr>
                        <th>Phone</th>
                        <th>Plan</th>
                        <th>UTR Number</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody id="tableBody">
                    <tr><td colspan="5" style="text-align:center;">Loading requests from Supabase...</td></tr>
                </tbody>
            </table>

            <script>
                async function loadRequests() {
                    try {
                        const res = await fetch('/api/admin/requests');
                        const data = await res.json();
                        const tbody = document.getElementById('tableBody');
                        tbody.innerHTML = '';
                        if(data.length === 0) {
                            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No requests found.</td></tr>';
                            return;
                        }
                        data.forEach(row => {
                            const tr = document.createElement('tr');
                            tr.innerHTML = \`
                                <td>\${row.phone}</td>
                                <td>\${row.plan || 'N/A'}</td>
                                <td>\${row.utr || 'N/A'}</td>
                                <td class="\${row.status === 'Active' ? 'active' : 'pending'}">\${row.status}</td>
                                <td>\${row.status === 'Active' ? 'Approved' : \`<button onclick="approve('\${row.phone}')">Approve</button>\`}</td>
                            \`;
                            tbody.appendChild(tr);
                        });
                    } catch(e) {
                        console.error(e);
                    }
                }

                async function approve(phone) {
                    const res = await fetch('/api/admin/approve', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ phone })
                    });
                    const result = await res.json();
                    if(result.success) {
                        loadRequests();
                    } else {
                        alert('Error approving driver');
                    }
                }

                loadRequests();
            </script>
        </body>
        </html>
    `);
});

// 2. Strict Activation Endpoint called by Android app
app.post('/api/activate', async (req, res) => {
    try {
        const { phone, plan, utr, minTrip, maxPickup, minFare, goTo } = req.body;
        
        if (!phone || !utr) {
            return res.status(400).json({ success: false, error: 'Missing required fields: phone or utr' });
        }

        const { data, error } = await supabase
            .from('drivers')
            .upsert([
                {
                    phone,
                    plan: plan || '1 Day Pass (₹9)',
                    utr,
                    status: 'Pending',
                    min_trip: minTrip || '',
                    max_pickup: maxPickup || '',
                    min_fare: minFare || '',
                    go_to: goTo || ''
                }
            ], { onConflict: ['phone'] });

        if (error) {
            console.error('Supabase Error:', error);
            return res.status(500).json({ success: false, error: error.message });
        }

        res.status(200).json({ success: true, message: 'Activation request saved securely in database' });
    } catch (err) {
        console.error('Server Exception:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// 3. Endpoint for Admin Panel to fetch all driver requests
app.get('/api/admin/requests', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('drivers')
            .select('*')
            .order('phone', { ascending: false });

        if (error) throw error;
        res.status(200).json(data);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: err.message });
    }
});

// 4. Endpoint for Admin Panel to approve a driver pass
app.post('/api/admin/approve', async (req, res) => {
    try {
        const { phone } = req.body;
        const { data, error } = await supabase
            .from('drivers')
            .update({ status: 'Active' })
            .eq('phone', phone);

        if (error) throw error;
        res.status(200).json({ success: true, message: `Driver ${phone} approved!` });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, error: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
