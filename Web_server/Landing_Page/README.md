# Landing_Page — Pixel Chaser Static Site

> **DEMO ARTIFACT — University Capstone Project**
> Static website. No credential collection. No external network calls. No tracking.

---

## Purpose

This is the fake indie-studio landing page that the tester-invitation email
([`Email_Template/invite.html`](../Email_Template/invite.html)) links to.

It simulates the "lure delivery" step of the attack chain in a controlled, fully
sandboxed environment. The site is entirely static (HTML + CSS + JS) and runs
on `localhost` only.

**Studio:** NovaBit Studios *(fictional)*
**Game:** Pixel Chaser *(existing Java Swing game in `Pixel Chaser\`)*

---

## Files

| File        | Purpose                                                               |
|-------------|-----------------------------------------------------------------------|
| `index.html`| Single-page site (hero, project, screenshots, tester, requirements, download) |
| `style.css` | Dark indie-dev theme — system fonts, no external resources           |
| `app.js`    | Smooth scroll, nav highlight, mobile toggle, download click logging  |
| `README.md` | This file                                                             |

---

## How to Preview Locally

Serve the folder with a simple HTTP server — required so that `app.js` loads correctly.

**Python (recommended):**
```sh
cd "D:\Bài tập lớn Cơ sở an toàn thông tin\Web_Server\Landing_Page"
python -m http.server 8080
```
Then open: **http://127.0.0.1:8080/**

**Node.js alternative:**
```sh
npx serve . -p 8080
```

> Opening `index.html` directly via `file://` also works for visual inspection,
> but smooth-scroll and nav-highlight rely on `app.js` which loads correctly in both modes.

---

## Configuring the Download URL — ONE place only

The installer download path is defined in **exactly one location**:

**File:** `app.js`
**Line:** 10 (top of file, immediately visible)

```javascript
const DOWNLOAD_URL = "../Installer/Output/PixelChaser_Setup.exe";
```

Change this string to update the download link across the entire page.
`app.js` reads this constant and injects it into:

- `#hero-download-btn` (hero section CTA)
- `#main-download-btn` (download section button)
- `#download-path-display` (visible path label in download section)

The nav Download button (`#nav-download-btn`) scrolls to `#download` section
rather than triggering a direct download — this is intentional.

> The `href` attributes in `index.html` already contain the same relative path
> as a **fallback** (in case JavaScript is disabled). If you change `DOWNLOAD_URL`
> in `app.js`, also update the `href` on `#hero-download-btn` and `#main-download-btn`
> in `index.html` for no-JS parity.

---

## Placeholder Assets — Replace Before Demo

The following elements use CSS/inline substitutes and can be upgraded later:

| Element | Location in `index.html` | Replacement |
|---------|--------------------------|-------------|
| Screenshot 1 — Main Menu | `#screenshots .screenshot-frame:nth-child(1)` | Replace `.screenshot-img` div with `<img src="img/ss-menu.png">` |
| Screenshot 2 — Gameplay  | `#screenshots .screenshot-frame:nth-child(2)` | Replace with `<img src="img/ss-gameplay.png">` |
| Screenshot 3 — Pause     | `#screenshots .screenshot-frame:nth-child(3)` | Replace with `<img src="img/ss-pause.png">` |
| Screenshot 4 — Game Over | `#screenshots .screenshot-frame:nth-child(4)` | Replace with `<img src="img/ss-gameover.png">` |
| Studio logo (nav + footer)| SVG `<rect>` pixel grid | Replace with `<img src="img/logo.svg">` |

Create `Landing_Page/img/` directory for actual image assets.

---

## Navigation Sections

| Nav Link | Scrolls To |
|----------|-----------|
| HOME | `#hero` |
| GAME PROJECT | `#project` |
| SCREENSHOTS | `#screenshots` |
| TESTER PROGRAM | `#tester` |
| SYSTEM REQUIREMENTS | `#requirements` |
| DOWNLOAD (CTA) | `#download` |

---

## What This Page Does NOT Do

- ❌ Does **NOT** collect credentials, names, or any personal data
- ❌ Does **NOT** contain any form with `<input type="password">`
- ❌ Does **NOT** add tracking pixels, analytics beacons, or fingerprinting
- ❌ Does **NOT** load external JavaScript, CSS, or fonts
- ❌ Does **NOT** trigger automatic downloads
- ❌ Does **NOT** redirect to any external URL
- ❌ Does **NOT** connect to any remote server (all JS is local and offline-capable)
- ❌ Does **NOT** modify Pixel Chaser, Ransomware, Lab_sandbox, or Installer directories

---

## Browser Compatibility

Tested layout targets: Chrome 120+, Firefox 120+, Edge 120+.
Uses CSS Grid, CSS custom properties (`var()`), and `scroll-behavior: smooth` —
all supported in modern browsers without polyfills.

---

## Security Note (for lab report)

In a real ransomware scenario this landing page would host the malware download.
In this lab:
- The "Download" button links to `../Installer/Output/PixelChaser_Setup.exe`
  which is a legitimate installer artifact inside the project directory.
- No payload is fetched from the network.
- All operations remain on `localhost`.
