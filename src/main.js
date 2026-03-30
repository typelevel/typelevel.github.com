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
//

let postMetadata = {};
let isFilter = false;

fetch("/search/metadata.json")
  .then(r => r.json())
  .then(data => { postMetadata = data; renderResults(); })
  .catch(() => { postMetadata = {}; });


let filters = {
  tags: [],
  authors: []
};

function switchSearchMode(mode) {
  const textTab = document.getElementById("tab-text");
  const metaTab = document.getElementById("tab-meta");

  const textBox = document.getElementById("text-search-container");
  const metaBox = document.getElementById("meta-search-container");

  if (mode === "text") {
    textTab.classList.add("bulma-is-active");
    metaTab.classList.remove("bulma-is-active");

    textBox.style.display = "block";
    metaBox.style.display = "none";

    document.getElementById("search-input").focus();
  } else {
    metaTab.classList.add("bulma-is-active");
    textTab.classList.remove("bulma-is-active");

    textBox.style.display = "none";
    metaBox.style.display = "block";

    document.getElementById("meta-input").focus();
  }
}
function handleKey(e) {
  if (e.key === "Enter") {
    const value = e.target.value.trim();
    if (!value) return;

    if (value.startsWith("tag:")) {
      addFilter("tags", value.replace("tag:", ""));
    } else if (value.startsWith("author:")) {
      addFilter("authors", value.replace("author:", ""));
    }
    e.target.value = "";
  }
}


function addFilter(type, value) {
  value = value.toLowerCase();
  
  if (!filters[type].includes(value)) {
    filters[type].push(value);
  }
  
  if(filters[type].length > 0) isFilter = true;
  renderChips();
  renderResults();
}

function removeFilter(type, value) {
  filters[type] = filters[type].filter(v => v !== value);
  if(filters["tags"].length == 0 && filters["authors"].length == 0) isFilter = false;
  renderChips();
  renderResults();
}

function renderChips() {
  const container = document.getElementById("filter-chips");
  
  const chips = [
    ...filters.tags.map(t => ({ type: "tags", value: t })),
    ...filters.authors.map(a => ({ type: "authors", value: a }))
  ];
  
  container.innerHTML = chips.map(c => `
    <span style="
    display:inline-flex;
    align-items:center;
    background:#eef;
    padding:0.3rem 0.6rem;
    border-radius:999px;
    font-size:0.8rem;
    ">
    ${c.type.slice(0,-1)}: ${c.value}
    <span 
    onclick="removeFilter('${c.type}','${c.value}')"
    style="margin-left:0.4rem; cursor:pointer; font-weight:bold;"
    >×</span>
    </span>
    `).join("");
  }
  
  function renderResults() {
    
    const results = Object.entries(postMetadata).filter(([path, meta]) => {
      
      const tagMatch =
      filters.tags.length === 0 ||
      filters.tags.every(f =>
        (meta.tags || []).some(t => t.toLowerCase().includes(f))
      );
      
      const authorMatch =
      filters.authors.length === 0 ||
      filters.authors.every(f =>
        (meta.authors || []).some(a => a.toLowerCase().includes(f))
      );
      
      return tagMatch && authorMatch;
    });
    
    // console.log(results)
    const html = results.map(([path, meta]) => `
  <div class="bulma-card" style="margin-bottom: 0.6rem; max-height: 12rem; overflow-y: auto;">
    <div class="bulma-card-content">
    
      <a href="${path}" style="font-weight:600;">
        ${path.split("/").pop().replace(".html","")}
      </a>
    
      <div style="margin-top:0.3rem;">
        ${(meta.tags || []).map(t => `
          <span class="bulma-tag bulma-is-link bulma-is-light">
            ${t}
          </span>
        `).join("")}

        ${(meta.authors || []).map(a => `
          <span class="bulma-tag bulma-is-info bulma-is-light">
            ${a}
          </span>
        `).join("")}

        <span class="bulma-tag bulma-is-light">
          ${meta.date || ""}
        </span>
      </div>

    </div>
  </div>
`).join("");


  if(isFilter) document.getElementById("metadata-results").innerHTML =
    html || `<p class="bulma-has-text-grey">No results found</p>`;
  else document.getElementById("metadata-results").innerHTML = "";
}
//
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
  searchWorker.postMessage({"query": event.target.value || ""});
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
