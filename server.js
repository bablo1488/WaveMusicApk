import express from "express";

const app = express();
const port = process.env.PORT || 3000;
const clientId = process.env.SOUNDCLOUD_CLIENT_ID;
const clientSecret = process.env.SOUNDCLOUD_CLIENT_SECRET;
let token = null;
let tokenExpiresAt = 0;

async function getToken() {
  if (!clientId || !clientSecret) throw new Error("SoundCloud API credentials are not configured on the server.");
  if (token && Date.now() < tokenExpiresAt - 60_000) return token;

  const basic = Buffer.from(`${clientId}:${clientSecret}`).toString("base64");
  const response = await fetch("https://secure.soundcloud.com/oauth/token", {
    method: "POST",
    headers: {
      "accept": "application/json; charset=utf-8",
      "content-type": "application/x-www-form-urlencoded",
      "authorization": `Basic ${basic}`
    },
    body: new URLSearchParams({ grant_type: "client_credentials" })
  });
  if (!response.ok) throw new Error(`SoundCloud token request failed (${response.status})`);
  const data = await response.json();
  token = data.access_token;
  tokenExpiresAt = Date.now() + (data.expires_in || 3600) * 1000;
  return token;
}

app.get("/soundcloud/search", async (req, res) => {
  const q = String(req.query.q || "").trim();
  if (!q) return res.status(400).json({ error: "Query parameter q is required." });
  if (q.length > 120) return res.status(400).json({ error: "Search query is too long." });
  try {
    const accessToken = await getToken();
    const params = new URLSearchParams({ q, access: "playable", limit: "20", linked_partitioning: "true" });
    const upstream = await fetch(`https://api.soundcloud.com/tracks?${params}`, {
      headers: { "accept": "application/json; charset=utf-8", "authorization": `OAuth ${accessToken}` }
    });
    const body = await upstream.text();
    res.status(upstream.status).type("application/json").send(body);
  } catch (error) {
    res.status(502).json({ error: error.message || "SoundCloud API error" });
  }
});

app.get("/health", (_req, res) => res.json({ ok: true }));
app.listen(port, () => console.log(`Wave Music SoundCloud proxy listening on ${port}`));
