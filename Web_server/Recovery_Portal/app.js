// ============================================================
// Pixel Chaser — Victim Decryption Portal (Client Logic)
// Read-only passive interface: monitors payment verification & private key release
// ============================================================

const state = {
  recoveryId: null,
  session: null,
  autoCloseStarted: false
};

const els = {
  connectionStatus:  document.getElementById("connectionStatus"),
  noSession:         document.getElementById("noSession"),
  sessionPanel:      document.getElementById("sessionPanel"),
  recoveryId:        document.getElementById("recoveryId"),
  instanceId:        document.getElementById("instanceId"),
  sessionStatus:     document.getElementById("sessionStatus"),
  paymentStatus:     document.getElementById("paymentStatus"),
  stepPending:       document.getElementById("stepPending"),
  stepConfirmed:     document.getElementById("stepConfirmed"),
  stepAvailable:     document.getElementById("stepAvailable"),
  paymentIndicator:  document.getElementById("paymentIndicator"),
  indicatorIcon:     document.getElementById("indicatorIcon"),
  indicatorTitle:    document.getElementById("indicatorTitle"),
  indicatorDesc:     document.getElementById("indicatorDesc"),
  manualRidInput:    document.getElementById("manualRidInput"),
  lookupBtn:         document.getElementById("lookupBtn"),
  keyReleaseCard:    document.getElementById("keyReleaseCard"),
  privateKeyText:    document.getElementById("privateKeyText"),
  copyKeyBtn:        document.getElementById("copyKeyBtn"),
  copyFeedback:      document.getElementById("copyFeedback")
};

// Check URL query parameters for ?rid=RCV-...
function initFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const rid = params.get("rid");
  if (rid) {
    els.manualRidInput.value = rid.trim();
    fetchSessionById(rid.trim());
  } else {
    fetchLatestSession();
  }
}

async function fetchLatestSession() {
  try {
    const res = await fetch("/api/session/latest", { cache: "no-store" });
    if (!res.ok) throw new Error("HTTP " + res.status);
    const data = await res.json();
    setOnline(true);
    if (data.found) {
      if (!els.manualRidInput.value) {
        els.manualRidInput.value = data.recoveryId;
      }
      renderSession(data);
    }
  } catch (e) {
    setOnline(false);
  }
}

async function fetchSessionById(rid) {
  if (!rid) return;
  try {
    const res = await fetch("/api/session/" + encodeURIComponent(rid), { cache: "no-store" });
    if (!res.ok) {
      if (res.status === 404) {
        els.noSession.classList.remove("hidden");
        els.sessionPanel.classList.add("hidden");
        els.noSession.querySelector("h2").textContent = "Session Not Found";
        els.noSession.querySelector("p").textContent = "No session matching Recovery ID '" + rid + "' exists.";
        return;
      }
      throw new Error("HTTP " + res.status);
    }
    const data = await res.json();
    setOnline(true);
    if (data.found) {
      renderSession(data);
    }
  } catch (e) {
    setOnline(false);
  }
}

function renderSession(session) {
  state.session = session;
  state.recoveryId = session.recoveryId;

  els.noSession.classList.add("hidden");
  els.sessionPanel.classList.remove("hidden");

  els.recoveryId.textContent = session.recoveryId;
  els.instanceId.textContent = session.instanceId;
  els.sessionStatus.textContent = session.status;
  els.paymentStatus.textContent = session.paymentStatus;

  updateTimeline(session.status, session.paymentStatus);
  updateIndicator(session.status, session.paymentStatus);

  // If status is RECOVERY_AVAILABLE or payment is CONFIRMED, fetch & reveal Private Key for victim!
  if (session.status === "RECOVERY_AVAILABLE" || session.status === "COMPLETED" || session.paymentStatus === "CONFIRMED") {
    fetchPrivateKey(session.recoveryId);
  } else {
    els.keyReleaseCard.classList.add("hidden");
  }

  // Trigger 3s auto-close countdown when session is COMPLETED (decryption successful)
  if (session.status === "COMPLETED" && !state.autoCloseStarted) {
    triggerAutoClose();
  }
}

function updateTimeline(status, payment) {
  [els.stepPending, els.stepConfirmed, els.stepAvailable].forEach(s => {
    s.classList.remove("active", "completed");
  });

  if (payment === "PENDING") {
    els.stepPending.classList.add("active");
    return;
  }
  if (payment === "CONFIRMED" && status !== "RECOVERY_AVAILABLE" && status !== "COMPLETED") {
    els.stepPending.classList.add("completed");
    els.stepConfirmed.classList.add("active");
    return;
  }
  if (status === "RECOVERY_AVAILABLE" || status === "COMPLETED") {
    els.stepPending.classList.add("completed");
    els.stepConfirmed.classList.add("completed");
    els.stepAvailable.classList.add("active");
  }
}

// Passive status indicator for Victim: no hover actions, read-only
function updateIndicator(status, payment) {
  if (status === "COMPLETED") {
    els.paymentIndicator.className = "victim-indicator-box indicator-confirmed";
    els.indicatorIcon.textContent = "✔";
    els.indicatorTitle.textContent = "System Decrypted & Incident Resolved";
    els.indicatorDesc.textContent = "All files in Lab_sandbox have been restored and verified with original SHA-256 hashes.";
    return;
  }

  if (status === "RECOVERY_AVAILABLE" || payment === "CONFIRMED") {
    els.paymentIndicator.className = "victim-indicator-box indicator-confirmed";
    els.indicatorIcon.textContent = "✔";
    els.indicatorTitle.textContent = "Payment Verified by Attacker — Private Key Dispatched";
    els.indicatorDesc.textContent = "The attacker has confirmed payment receipt and released your RSA-4096 Private Key below.";
    return;
  }

  // PENDING state
  els.paymentIndicator.className = "victim-indicator-box indicator-pending";
  els.indicatorIcon.textContent = "⏳";
  els.indicatorTitle.textContent = "Awaiting Attacker Payment Confirmation ($300 USD)";
  els.indicatorDesc.textContent = "Ransom payment is currently awaiting verification by the attacker C2 server. Please stand by...";
}

async function fetchPrivateKey(rid) {
  try {
    const res = await fetch("/api/recovery/key/" + encodeURIComponent(rid));
    if (!res.ok) {
      els.privateKeyText.value = "Awaiting key release from attacker server...";
      return;
    }
    const data = await res.json();
    if (data.success && data.privateKeyB64) {
      els.keyReleaseCard.classList.remove("hidden");
      els.privateKeyText.value = data.privateKeyB64;
      els.copyFeedback.textContent = "";
    }
  } catch (e) {
    console.error("Key fetch error:", e);
  }
}

function copyPrivateKey() {
  const text = els.privateKeyText.value;
  if (!text) return;
  navigator.clipboard.writeText(text).then(() => {
    els.copyFeedback.textContent = "✔ Key copied to clipboard! Paste into Wana Decrypt0r.";
    els.copyFeedback.style.color = "#4ade80";
  }).catch(() => {
    els.privateKeyText.select();
    document.execCommand("copy");
    els.copyFeedback.textContent = "✔ Key copied!";
    els.copyFeedback.style.color = "#4ade80";
  });
}

function setOnline(online) {
  els.connectionStatus.classList.toggle("online", online);
  els.connectionStatus.classList.toggle("offline", !online);
  els.connectionStatus.textContent = online ? "GATEWAY ONLINE" : "GATEWAY OFFLINE";
}

// Event Listeners for Victim
els.copyKeyBtn.addEventListener("click", copyPrivateKey);

els.lookupBtn.addEventListener("click", () => {
  const rid = els.manualRidInput.value.trim();
  if (rid) fetchSessionById(rid);
});

els.manualRidInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    const rid = els.manualRidInput.value.trim();
    if (rid) fetchSessionById(rid);
  }
});

// Auto-Close Countdown Logic when Decryption completes
function triggerAutoClose() {
  state.autoCloseStarted = true;
  let remaining = 3;
  updateAutoCloseUI(remaining);

  const timer = setInterval(() => {
    remaining--;
    if (remaining > 0) {
      updateAutoCloseUI(remaining);
    } else {
      clearInterval(timer);
      executeClose();
    }
  }, 1000);
}

function updateAutoCloseUI(seconds) {
  // Update status card indicator
  els.paymentIndicator.className = "victim-indicator-box indicator-confirmed";
  els.indicatorIcon.textContent = "✔";
  els.indicatorTitle.textContent = "Decryption Complete — System Restored!";
  els.indicatorDesc.innerHTML = "All files in Lab_sandbox have been restored and verified with original SHA-256.<br>" +
    "<span style='display:inline-block; margin-top:8px; font-weight:600; color:#4ade80; font-size:14px;'>" +
    "Closing Recovery Portal window in <span style='font-size:16px; color:#fff; background:#16a34a; padding:2px 8px; border-radius:4px; font-weight:bold;'>" + seconds + "s</span>...</span>";

  // Floating notification card
  let banner = document.getElementById("autoCloseBanner");
  if (!banner) {
    banner = document.createElement("div");
    banner.id = "autoCloseBanner";
    banner.style.cssText = "position:fixed; bottom:24px; right:24px; background:#14532d; color:#ffffff; padding:14px 20px; border-radius:12px; box-shadow:0 8px 30px rgba(0,0,0,0.6); font-size:14px; font-weight:600; z-index:9999; display:flex; align-items:center; gap:14px; border:1px solid #22c55e;";
    document.body.appendChild(banner);
  }

  banner.innerHTML = "<span>✔ Decryption Complete! Auto-closing in <strong>" + seconds + "s</strong></span>" +
    "<button id='bannerCloseNowBtn' style='background:#ffffff; color:#14532d; border:none; padding:6px 14px; border-radius:6px; font-weight:bold; font-size:13px; cursor:pointer;'>Close Now</button>";

  const btn = document.getElementById("bannerCloseNowBtn");
  if (btn) {
    btn.onclick = () => executeClose();
  }
}

function executeClose() {
  try {
    window.open('', '_self', '');
    window.close();
  } catch (e) {}

  setTimeout(() => {
    if (!window.closed) {
      document.body.innerHTML = `
        <div style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100vh; background:#0f172a; color:#f8fafc; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif; text-align:center; padding:20px;">
          <div style="width:72px; height:72px; border-radius:50%; background:#16a34a; display:flex; align-items:center; justify-content:center; font-size:38px; color:#fff; margin-bottom:20px;">✔</div>
          <h1 style="color:#4ade80; font-size:28px; margin:0 0 10px 0;">Decryption Complete & Session Closed</h1>
          <p style="color:#94a3b8; font-size:16px; max-width:480px; margin:0 0 24px 0; line-height:1.5;">All files in <code>Lab_sandbox</code> have been restored. The ransomware incident has been resolved.</p>
          <button onclick="window.close()" style="background:#334155; color:#f8fafc; border:1px solid #475569; padding:8px 20px; border-radius:8px; font-size:14px; cursor:pointer;">Close Tab</button>
        </div>
      `;
      try { window.close(); } catch (e) {}
    }
  }, 200);
}

// Periodic status poll (every 800ms for fast auto-close sync)
setInterval(() => {
  if (state.autoCloseStarted) return;
  if (state.recoveryId) {
    fetchSessionById(state.recoveryId);
  } else {
    fetchLatestSession();
  }
}, 800);

initFromUrl();