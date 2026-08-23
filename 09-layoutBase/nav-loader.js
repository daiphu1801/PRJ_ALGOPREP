/* AlgoPrep — spinner overlay on page-to-page navigation. Loaded from each page's helmet. */
(function () {
  if (window.__apNavLoader) return;
  window.__apNavLoader = true;

  var DELAY = 620;

  function css(el, s) { for (var k in s) el.style[k] = s[k]; }

  function overlay(label) {
    var lang = (document.querySelector("[data-ui-lang]") || {}).getAttribute
      ? document.querySelector("[data-ui-lang]").getAttribute("data-ui-lang") : "vi";
    var theme = document.querySelector("[data-theme]");
    var wrap = document.createElement("div");
    if (theme) wrap.setAttribute("data-theme", theme.getAttribute("data-theme"));
    css(wrap, {
      position: "fixed", inset: "0", zIndex: "9999", display: "flex", flexDirection: "column",
      alignItems: "center", justifyContent: "center", gap: "16px",
      background: "var(--bg, #EEF0F2)", fontFamily: "'Be Vietnam Pro', system-ui, sans-serif",
      opacity: "0", transition: "opacity .16s ease"
    });

    var ring = document.createElement("div");
    css(ring, {
      width: "26px", height: "26px", borderRadius: "50%",
      border: "2.5px solid var(--line, #E6E8EB)", borderTopColor: "var(--accent, #30AFFF)",
      animation: "ap-nav-spin .72s linear infinite"
    });

    var text = document.createElement("div");
    text.textContent = (lang === "en" ? "Opening " : "Đang mở ") + label;
    css(text, {
      fontFamily: "'JetBrains Mono', monospace", fontSize: "12px", fontWeight: "600",
      letterSpacing: "0.1em", textTransform: "uppercase", color: "var(--ink3, #9AA0A6)"
    });

    wrap.appendChild(ring);
    wrap.appendChild(text);
    document.body.appendChild(wrap);
    requestAnimationFrame(function () { wrap.style.opacity = "1"; });
  }

  var style = document.createElement("style");
  style.textContent = "@keyframes ap-nav-spin { to { transform: rotate(360deg); } }";
  document.head.appendChild(style);

  document.addEventListener("click", function (e) {
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
    var a = e.target.closest && e.target.closest("a[href]");
    if (!a) return;
    var href = a.getAttribute("href") || "";
    if (href.indexOf(".dc.html") === -1) return;
    if (href === window.location.pathname.split("/").pop()) return;
    e.preventDefault();
    overlay((a.textContent || "").trim() || "AlgoPrep");
    setTimeout(function () { window.location.href = href; }, DELAY);
  }, true);
})();
