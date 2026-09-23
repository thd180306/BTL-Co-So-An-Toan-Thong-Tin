const state = {
  recoveryId: null,
  session: null
};

const els = {
  connectionStatus: document.getElementById("connectionStatus"),
  noSession:        document.getElementById("noSession"),
  sessionPanel:     document.getElementById("sessionPanel"),
  recoveryId:       document.getElementById("recoveryId"),
  instanceId:       document.getElementById("instanceId"),
  sessionStatus:    document.getElementById("sessionStatus"),
  paymentStatus:    document.getElementById("paymentStatus"),
  payButton:        document.getElementById("payButton"),
  actionMessage:    document.getElementById("actionMessage"),
  stepPending:      document.getElementById("stepPending"),
  stepConfirmed:    document.getElementById("stepConfirmed"),
  stepAvailable:    document.getElementById("stepAvailable"),
  manualRidInput:   document.getElementById("manualRidInput"),
  lookupBtn:        document.getElementById("lookupBtn"),
  latestBtn:        document.getElementById("latestBtn"),
  keyReleaseCard:   document.getElementById("keyReleaseCard"),
  privateKeyText:   document.getElementById("privateKeyText"),
  copyKeyBtn:       document.getElementById("copyKeyBtn"),
  copyFeedback:     document.getElementById("copyFeedback"),
  sandboxFileList:  document.getElementById("sandboxFileList"),
  refreshFilesBtn:  document.getElementById("refreshFilesBtn"),
  refreshDbBtn:     document.getElementById("refreshDbBtn"),
  tableSessions:    document.getElementById("tableSessions").querySelector("tbody"),
  tableKeys:        document.getElementById("tableKeys").querySelector("tbody"),
  tableManifest:    document.getElementById("tableManifest").querySelector("tbody")
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
  fetchDbInspect();
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
  updateButton(session.status, session.paymentStatus);

  // Fetch sandbox files for this session
  fetchSandboxFiles(session.recoveryId);

  // If status is RECOVERY_AVAILABLE or payment is CONFIRMED, fetch & reveal Private Key!
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

function updateButton(status, payment) {
  if (status === "RECOVERY_AVAILABLE" || status === "COMPLETED") {
    els.payButton.disabled = true;
    els.payButton.textContent = "PAYMENT CONFIRMED — KEY RELEASED";
    els.payButton.style.background = "#22c55e";
    els.payButton.style.color = "#000";
    els.actionMessage.textContent = "Private Key released below. Copy it into Wana Decrypt0r on your PC.";
    return;
  }
  if (payment === "PENDING") {
    els.payButton.disabled = false;
    els.payButton.textContent = "CONFIRM DEMO PAYMENT ($300)";
    els.payButton.style.background = "#f59e0b";
    els.payButton.style.color = "#111827";
    els.actionMessage.textContent = "Demo mode — simulates victim paying ransom to attacker.";
    return;
  }
  els.payButton.disabled = true;
}

async function demoPayment() {
  if (!state.recoveryId) return;
  els.payButton.disabled = true;
  els.payButton.textContent = "PROCESSING PAYMENT...";
  els.actionMessage.textContent = "Notifying Attacker Controller :5000 to record payment in database...";

  try {
    const res = await fetch("/api/payment/demo", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ recoveryId: state.recoveryId })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Payment failed");
    renderSession(data);
    els.actionMessage.textContent = "Payment confirmed by attacker! Retrieving Private Key...";
    fetchPrivateKey(state.recoveryId);
    fetchDbInspect();
  } catch (e) {
    els.payButton.disabled = false;
    els.payButton.textContent = "CONFIRM DEMO PAYMENT ($300)";
    els.actionMessage.textContent = "Error: " + e.message;
  }
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

// ── Live File Sandbox Monitor ──────────────────────────────────────────

async function fetchSandboxFiles(rid) {
  if (!rid) return;
  try {
    const res = await fetch("/api/sandbox/files/" + encodeURIComponent(rid));
    if (!res.ok) return;
    const data = await res.json();
    renderSandboxFiles(data.files || []);
  } catch (e) {
    console.error("Sandbox fetch error:", e);
  }
}

function renderSandboxFiles(files) {
  if (!files || files.length === 0) {
    els.sandboxFileList.innerHTML = '<div class="file-item-empty">No files found in workspace directory.</div>';
    return;
  }

  els.sandboxFileList.innerHTML = files.map(f => {
    const isLocked = f.isLocked;
    const badgeClass = isLocked ? "badge-locked" : "badge-restored";
    const badgeText  = isLocked ? "ENCRYPTED (.locked)" : "PLAINTEXT (NORMAL)";
    const icon = isLocked ? "🔒" : "📄";

    return `
      <div class="sandbox-file-card ${isLocked ? 'file-locked' : 'file-normal'}">
        <div class="file-card-top">
          <span class="file-icon">${icon}</span>
          <span class="file-name" title="${f.name}">${f.name}</span>
        </div>
        <div class="file-card-details">
          <span>Size: <b>${f.size.toLocaleString()} bytes</b></span>
          <span class="file-badge ${badgeClass}">${badgeText}</span>
        </div>
      </div>
    `;
  }).join("");
}

// ── Live Database Inspector ────────────────────────────────────────────

async function fetchDbInspect() {
  try {
    const res = await fetch("/api/db/inspect");
    if (!res.ok) return;
    const data = await res.json();
    renderDbInspect(data);
  } catch (e) {
    console.error("DB inspect error:", e);
  }
}

function renderDbInspect(data) {
  // 1. Sessions table
  if (data.sessions && data.sessions.length > 0) {
    els.tableSessions.innerHTML = data.sessions.map(s => `
      <tr>
        <td><code>${s.sessionId}</code></td>
        <td><strong style="color:#fbbf24;">${s.recoveryId}</strong></td>
        <td><span class="db-status-tag tag-${s.status.toLowerCase()}">${s.status}</span></td>
        <td><span class="db-status-tag tag-${s.paymentStatus.toLowerCase()}">${s.paymentStatus}</span></td>
        <td>${new Date(s.createdAt * 1000).toLocaleTimeString()}</td>
      </tr>
    `).join("");
  } else {
    els.tableSessions.innerHTML = '<tr><td colspan="5" class="table-empty">No sessions recorded yet in sessions.db.</td></tr>';
  }

  // 2. Recovery keys table
  if (data.recoveryKeys && data.recoveryKeys.length > 0) {
    els.tableKeys.innerHTML = data.recoveryKeys.map(k => `
      <tr>
        <td><strong>${k.recoveryId}</strong></td>
        <td><code>${k.keyId}</code></td>
        <td><span class="db-status-tag tag-${k.status.toLowerCase()}">${k.status}</span></td>
        <td>${k.keyLen} chars (RSA-4096)</td>
        <td>${k.releasedAt ? new Date(k.releasedAt * 1000).toLocaleTimeString() : '<em>Not yet released</em>'}</td>
      </tr>
    `).join("");
  } else {
    els.tableKeys.innerHTML = '<tr><td colspan="5" class="table-empty">No keys generated yet.</td></tr>';
  }

  // 3. File manifest table
  if (data.fileManifest && data.fileManifest.length > 0) {
    els.tableManifest.innerHTML = data.fileManifest.map(m => `
      <tr>
        <td><strong>${m.recoveryId}</strong></td>
        <td>📄 ${m.originalName}</td>
        <td><code class="hash-code" title="${m.origSha256}">${m.origSha256 ? m.origSha256.substring(0, 16) + '...' : '-'}</code></td>
        <td><code>${m.currentName}</code></td>
        <td><span class="db-status-tag tag-${m.restoreStatus.toLowerCase()}">${m.restoreStatus}</span></td>
      </tr>
    `).join("");
  } else {
    els.tableManifest.innerHTML = '<tr><td colspan="5" class="table-empty">No files tracked in manifest yet.</td></tr>';
  }
}

function setOnline(online) {
  els.connectionStatus.classList.toggle("online", online);
  els.connectionStatus.classList.toggle("offline", !online);
  els.connectionStatus.textContent = online ? "CONTROLLER ONLINE" : "CONTROLLER OFFLINE";
}

// ── Tab Switching for Database Inspector ───────────────────────────────

document.querySelectorAll(".db-tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".db-tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".db-tab-content").forEach(c => c.classList.add("hidden"));

    btn.classList.add("active");
    const targetId = btn.getAttribute("data-tab");
    const targetContent = document.getElementById(targetId);
    if (targetContent) targetContent.classList.remove("hidden");
  });
});

// Event Listeners
els.payButton.addEventListener("click", demoPayment);
els.copyKeyBtn.addEventListener("click", copyPrivateKey);
els.refreshFilesBtn.addEventListener("click", () => {
  if (state.recoveryId) fetchSandboxFiles(state.recoveryId);
});
els.refreshDbBtn.addEventListener("click", fetchDbInspect);

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

els.latestBtn.addEventListener("click", () => {
  fetchLatestSession();
  fetchDbInspect();
});

// Periodic status poll (every 2.5s)
setInterval(() => {
  if (state.recoveryId) {
    fetchSessionById(state.recoveryId);
    fetchSandboxFiles(state.recoveryId);
  } else {
    fetchLatestSession();
  }
  fetchDbInspect();
}, 2500);

initFromUrl();