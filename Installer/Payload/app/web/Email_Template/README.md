# Email_Template — Pixel Chaser Beta Invite

> **DEMO ARTIFACT — University Capstone Project**
> This directory contains a static email rendering artifact only.
> **No emails are sent. No external services are contacted. No tracking is performed.**

---

## Purpose

This folder contains a fictional tester-invitation email for **Pixel Chaser**, created by
the fictional indie studio **NovaBit Studios**. It is a presentation/demo artifact for a
university security lab project.

The files demonstrate what a phishing-style delivery mechanism looks like in a controlled,
fully sandboxed simulation. No real email infrastructure is involved at any point.

---

## Files

| File           | Description                                                              |
|----------------|--------------------------------------------------------------------------|
| `invite.html`  | HTML email template — inline CSS, table layout, mobile-friendly, no JS  |
| `invite.txt`   | Plain-text fallback with identical content                               |
| `preview.html` | Standalone browser preview tool (iframe + side-by-side + 3 buttons)     |
| `README.md`    | This file                                                                |

---

## How to Preview

1. Open **`preview.html`** in any modern browser (Chrome, Firefox, Edge).
2. The left panel shows `invite.html` rendered inside an iframe.
3. Click **"Toggle Plain Text"** to switch the left panel to the `invite.txt` content.
4. Click **"Open Landing Page"** to open the configured landing URL in a new tab.
5. Click **"Copy Landing URL"** to copy the URL to the clipboard.

> **Note:** The plain-text panel uses `fetch("invite.txt")`.
> If you open `preview.html` directly via `file://` in Chrome, the fetch may be blocked
> by CORS policy. Serve the folder with a simple HTTP server instead:
> ```
> python -m http.server 8080
> ```
> Then open `http://127.0.0.1:8080/preview.html`.

---

## How to Change the Landing URL

The landing page URL is stored in **exactly one place per file**. There are two steps:

### Step 1 — Update `preview.html` (JS constant)

Open `preview.html` and find the constant near the top of the `<script>` block:

```javascript
const LANDING_URL = "http://127.0.0.1:8000/landing";
```

Replace the URL string with the actual landing page URL.
This controls all three buttons and the URL display strip in the preview tool.

### Step 2 — Replace token in `invite.html` and `invite.txt`

Both template files use the placeholder token `{{LANDING_URL}}` wherever the URL appears.
Replace every occurrence of this token with the actual URL before distributing the email.

You can do this programmatically:

**PowerShell:**
```powershell
$url = "http://127.0.0.1:8080/landing"
(Get-Content invite.html) -replace '\{\{LANDING_URL\}\}', $url | Set-Content invite.html
(Get-Content invite.txt)  -replace '\{\{LANDING_URL\}\}', $url | Set-Content invite.txt
```

**Python:**
```python
url = "http://127.0.0.1:8080/landing"
for fname in ["invite.html", "invite.txt"]:
    with open(fname, "r", encoding="utf-8") as f:
        content = f.read()
    with open(fname, "w", encoding="utf-8") as f:
        f.write(content.replace("{{LANDING_URL}}", url))
```

---

## What This File Does NOT Do

- ❌ Does **NOT** send emails (no SMTP, no SendGrid, no Gmail API)
- ❌ Does **NOT** connect to any external server
- ❌ Does **NOT** collect credentials, names, or any personal data
- ❌ Does **NOT** track email opens (no tracking pixels, no beacons)
- ❌ Does **NOT** contain hidden scripts or obfuscated code
- ❌ Does **NOT** modify any other project module

---

## Studio Identity

| Field        | Value                                      |
|--------------|--------------------------------------------|
| Studio name  | NovaBit Studios *(fictional)*              |
| Game name    | Pixel Chaser *(existing Java Swing game)*  |
| Build        | Beta 0.0.0.1                               |
| Tone         | Indie game tester invitation               |

---

## Security Notes (for the lab report)

This artifact simulates the **initial lure delivery** step of the attack chain.
In a real ransomware scenario this would be the phishing email that delivers the malware payload.
In this lab:

- The CTA link points to the **local landing page** (`Lab_sandbox` scope only).
- No external URLs, no payload downloads, no real victims.
- The email template is inspectable, static HTML with no obfuscation.
