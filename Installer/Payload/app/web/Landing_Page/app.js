/* ============================================================
   app.js — Pixel Chaser Landing Page
   DEMO ARTIFACT — University Capstone Project
   No credential collection. No external calls. Console.log only.
   ============================================================
   CONFIGURABLE DOWNLOAD URL — change this ONE constant.
   This is the only place the download path is defined.
   Update to match the actual Installer output path before demo.
   ============================================================ */
const DOWNLOAD_URL = "../Installer/Output/PixelChaser_Setup.exe";

(function () {
  "use strict";

  /* ── Apply download URL to all download buttons ── */
  function applyDownloadURL() {
    ["hero-download-btn", "main-download-btn", "nav-download-btn"].forEach(function (id) {
      var el = document.getElementById(id);
      if (el && id !== "nav-download-btn") el.href = DOWNLOAD_URL;
    });
    var display = document.getElementById("download-path-display");
    if (display) display.textContent = DOWNLOAD_URL;
  }

  /* ── Nav: scroll-spy highlight ── */
  var sections = ["hero","project","screenshots","tester","requirements","download"];
  var navLinks  = document.querySelectorAll(".nav-link[data-section]");
  var header    = document.getElementById("site-header");

  function onScroll() {
    var scrollY = window.scrollY;

    /* Scrolled shadow on nav */
    if (header) header.classList.toggle("scrolled", scrollY > 40);

    /* Active section highlight */
    var active = sections[0];
    sections.forEach(function (id) {
      var sec = document.getElementById(id);
      if (sec && sec.getBoundingClientRect().top <= 100) active = id;
    });
    navLinks.forEach(function (link) {
      link.classList.toggle("active", link.dataset.section === active);
    });
  }

  /* ── Mobile nav toggle ── */
  var toggleBtn = document.getElementById("nav-toggle");
  var navList   = document.getElementById("nav-links");
  if (toggleBtn && navList) {
    toggleBtn.addEventListener("click", function () {
      var open = navList.classList.toggle("open");
      toggleBtn.setAttribute("aria-expanded", open);
    });
    navList.addEventListener("click", function () {
      navList.classList.remove("open");
      toggleBtn.setAttribute("aria-expanded", "false");
    });
  }

  /* ── Download click tracking (console only) ── */
  function trackDownload(e) {
    console.log("[PixelChaser] Download initiated:", DOWNLOAD_URL, new Date().toISOString());
  }
  ["hero-download-btn","main-download-btn"].forEach(function (id) {
    var el = document.getElementById(id);
    if (el) el.addEventListener("click", trackDownload);
  });

  /* ── Init ── */
  document.addEventListener("DOMContentLoaded", function () {
    applyDownloadURL();
    onScroll();
  });
  window.addEventListener("scroll", onScroll, { passive: true });

}());
