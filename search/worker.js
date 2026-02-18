importScripts("./protosearch.js")

async function getQuerier(index) {
  let querier = fetch("./" + index + ".idx")
    .then(res => res.blob())
    .then(blob => QuerierBuilder.load(blob))
    .catch((error) => console.error("getQuerier error: ", error));
  return await querier
}

const urlParams = new URLSearchParams(location.search)

// Handle `index` query param
const maybeIndex = urlParams.get("index")
const index = maybeIndex ? maybeIndex : "searchIndex"

// Handle `q` query param
const maybeQuery = urlParams.get("q")

const querierPromise = getQuerier(index)

async function searchIt(query) {
  const querier = await querierPromise
  return querier.search(query)
}

onmessage = async function(e) {
  const query = e.data || '' // empty strings become undefined somehow ...
  this.postMessage(await searchIt(query))
}

if (maybeQuery == undefined) {
  searchIt("warmup")
}
// If it is defined, search.js is going to call us as soon as we return
// So we skip the warmup
