function handleBurgerClick(el) {
  const target = el.dataset.target;
  const $target = document.getElementById(target);
  el.classList.toggle('bulma-is-active');
  $target.classList.toggle('bulma-is-active');
}

// search

function showSearchModal() {
  const modal = document.getElementById("search-modal");
  if (!modal.classList.contains("bulma-is-active")) {
    modal.classList.add("bulma-is-active");
    const input = document.getElementById("search-input");
    input.value = "";
    input.focus();
    const results = document.getElementById("search-results");
    results.innerHTML = `<p class="bulma-has-text-grey bulma-has-text-centered">Type to search...</p>`;
  }
}

function hideSearchModal() {
  const modal = document.getElementById("search-modal");
  if (modal.classList.contains("bulma-is-active"))
    modal.classList.remove("bulma-is-active");
}

function renderHit(hit) {
  const link = `${hit.fields.path}.html`
  const title = hit.highlights["title"] || hit.fields["title"]
  const preview = hit.highlights["body"]
  const tags = []
  if (link.startsWith("/blog")) tags.push("blog")
  const renderedTags = tags.map(tag => `<span class="bulma-tag bulma-is-link bulma-is-light">${tag}</span>`).join("")

  return `
  <div class="bulma-card">
    <div class="bulma-card-content">
      <div class="bulma-content">
        <a class="bulma-subtitle bulma-has-text-link" href="${link}">${title}</a>
        &nbsp;${renderedTags}
        <p>${preview}</p>
      </div>
    </div>
  </div>
  `
}

const searchWorker = new Worker("/search/worker.js");

searchWorker.onmessage = function (e) {
  const fallback = `<p class="bulma-has-text-grey bulma-has-text-centered">No results found</p>`
  const markup = e.data.map(renderHit).join("") || fallback
  const results = document.getElementById("search-results");
  results.innerHTML = markup
}

function onSearchInput(event) {
  searchWorker.postMessage(event.target.value || "");
}

// Keyboard shortcuts: `/` to open, `Escape` to close
window.addEventListener("keydown", (event) => {
  if (event.defaultPrevented) return;
  if (event.code == "Slash") {
    event.preventDefault();
    showSearchModal();
  }
  if (event.code == "Escape") {
    event.preventDefault();
    hideSearchModal();
  }
});
