// ============================================================
// Pixel Chaser — Victim Decryption Portal (Client Logic)
// Read-only passive interface: monitors payment verification & private key release
// ============================================================

const state = {
  recoveryId: null,
  session: null
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

// Periodic status poll (every 2.5s)
setInterval(() => {
  if (state.recoveryId) {
    fetchSessionById(state.recoveryId);
  } else {
    fetchLatestSession();
  }
}, 2500);

initFromUrl();