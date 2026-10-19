/* Interactive tutorials (prototype).
 *
 * Loads the lesson's sample code from the .sc file named by the
 * `data-code-src` attribute, hands it to an embedded Scastie editor, and wires
 * up the "Open in Scastie" button.
 */
(function () {
  "use strict";

  var SCASTIE_URL = "https://scastie.scala-lang.org/";

  function ready(fn) {
    if (document.readyState !== "loading") fn();
    else document.addEventListener("DOMContentLoaded", fn);
  }

  function showToast(message) {
    var toast = document.getElementById("tutorial-toast");
    if (!toast) return;
    toast.textContent = message;
    toast.hidden = false;
    window.setTimeout(function () {
      toast.hidden = true;
    }, 4000);
  }

  // Options for scastie.Embedded, taken from the page's Laika config (see
  // /tutorials/directory.conf) with sane fallbacks.
  function embedOptions(pre) {
    var options = {
      theme: "light", // the site is light-only (see main.template.html)
      isWorksheetMode: pre.dataset.worksheet !== "false",
      targetType: pre.dataset.targetType || "scala-cli"
    };
    if (pre.dataset.scalaVersion) options.scalaVersion = pre.dataset.scalaVersion;
    return options;
  }

  function embed(pre) {
    if (!window.scastie) {
      // embedded.js failed to load; the plain <pre> stays, which at least
      // leaves the sample code readable.
      console.warn("Scastie embedded.js is unavailable; showing static code.");
      return;
    }
    var options = embedOptions(pre);
    try {
      scastie.Embedded("#" + pre.id, options);
    } catch (e) {
      console.warn("Scastie embed failed for target " + options.targetType + ":", e);
      if (options.targetType !== "scala3") {
        // Last resort: a plain Scala 3 editor. Any `//> using` directives in
        // the lesson won't take effect, but the editor still works.
        options.targetType = "scala3";
        try {
          scastie.Embedded("#" + pre.id, options);
        } catch (e2) {
          console.warn("Scastie embed failed:", e2);
        }
      }
    }
  }

  // The code as it currently stands in the editor, falling back to the code we
  // loaded if Scastie hasn't mounted (or has changed its DOM under us).
  function currentCode(originalCode) {
    var content = document.querySelector(".scastie .cm-content");
    var edited = content ? content.innerText : "";
    return edited.trim() ? edited : originalCode;
  }

  function openInScastie(originalCode) {
    // Scastie's embed comes with its own "open in Scastie" control (the small
    // external-link icon it overlays on the editor), which saves the current
    // editor contents as a snippet and opens it. Delegate to it, so we get the
    // user's edits and don't have to reimplement the save API.
    var overlayButton = document.querySelector(".scastie .embedded-overlay li.logo");
    if (overlayButton) {
      overlayButton.click();
      return;
    }

    // Fallback for when the embed didn't mount (or Scastie changed its DOM):
    // hand the code over in the URL, and put it on the clipboard too in case
    // Scastie ignores the parameter.
    var code = currentCode(originalCode);
    var opened = window.open(SCASTIE_URL + "?code=" + encodeURIComponent(code), "_blank", "noopener");
    if (!opened) {
      showToast("Scastie was blocked from opening in a new tab.");
      return;
    }
    if (navigator.clipboard) {
      navigator.clipboard.writeText(code).then(
        function () {
          showToast("Opened Scastie. Your code is also on the clipboard.");
        },
        function () {
          /* clipboard unavailable; the query parameter is all we have */
        }
      );
    }
  }

  // The panes fill the viewport below the navbar, whose height depends on the
  // font metrics, so measure it rather than guessing.
  function trackNavbarHeight() {
    var navbar = document.querySelector("nav.bulma-navbar");
    if (!navbar) return;
    var apply = function () {
      document.documentElement.style.setProperty("--tutorial-navbar-height", navbar.offsetHeight + "px");
    };
    apply();
    if (window.ResizeObserver) new ResizeObserver(apply).observe(navbar);
    else window.addEventListener("resize", apply);
  }

  ready(function () {
    var pre = document.getElementById("tutorial-code");
    if (!pre) return;

    trackNavbarHeight();

    var button = document.getElementById("open-in-scastie");
    var source = pre.dataset.codeSrc;
    if (!source) {
      pre.textContent = "This lesson does not name a code file (set `scastie.code` in its header).";
      if (button) button.disabled = true;
      return;
    }

    fetch(source)
      .then(function (response) {
        if (!response.ok) throw new Error(response.status + " " + response.statusText);
        return response.text();
      })
      .then(function (code) {
        var trimmed = code.replace(/\s+$/, "");
        pre.textContent = trimmed;
        embed(pre);
        if (button) {
          button.addEventListener("click", function () {
            openInScastie(trimmed);
          });
        }
      })
      .catch(function (error) {
        pre.textContent = "Could not load " + source + " (" + error.message + ").";
        if (button) button.disabled = true;
      });
  });
})();
