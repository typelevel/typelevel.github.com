// Protosearch - Docs Renderer
// Renders search results for Laika documentation pages
// Fields: path, title, body

function renderDocs(hit, config) {
  const path = hit.fields.path.startsWith("/") ? hit.fields.path.slice(1) : hit.fields.path
  const htmlPath = `${path}.html`
  const link = new URL(htmlPath, config.baseUrl)
  const title = hit.highlights["title"] || hit.fields["title"]
  const preview = hit.highlights["body"]
  const score = hit.score.toFixed(4)

  const previewHtml = config.showPreview && preview
    ? `<p class="ps-preview">${preview}</p>`
    : ""

  const scoreHtml = config.showScore
    ? `<span class="ps-score">score: ${score}</span>`
    : ""

  const pathHtml = config.showPath
    ? `<span class="ps-path">${path}</span>`
    : ""

  const metaHtml = (config.showScore || config.showPath)
    ? `<footer class="ps-meta">${scoreHtml}${pathHtml}</footer>`
    : ""

  return `
<article class="ps-result">
  <header>
    <a href="${link}">${title}</a>
  </header>
  ${previewHtml}
  ${metaHtml}
</article>`
}

window.Protosearch.registerRenderer("docs", renderDocs)

