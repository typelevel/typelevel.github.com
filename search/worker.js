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

const searchOptions = ["size", "skip", "highlightFields", "resultFields"]

async function searchIt(request) {
  const querier = await querierPromise
  return querier.search(
    request.query,
    ...searchOptions.map(option => request[option])
  )
}

const waitMs = 100
let timeoutId = null
let lastRequest = {query: ""}

function post(request) {
  lastRequest = request
  if (timeoutId) clearTimeout(timeoutId)
  timeoutId = setTimeout(async () => {
    timeoutId = null
    postMessage(await searchIt(lastRequest))
  }, waitMs)
}

async function flush(request) {
  if (timeoutId) {
    clearTimeout(timeoutId)
    timeoutId = null
  }
  postMessage(await searchIt(request))
}

onmessage = function(e) {
  const request = {...e.data, query: e.data.query || ''}
  if (request.flush) {
    flush(request)
  } else {
    post(request)
  }
}

if (maybeQuery == undefined) {
  searchIt({query: "warmup", size: 1})
}
// If it is defined, search.js is going to call us as soon as we return
// So we skip the warmup
