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

const waitMs = 100
let timeoutId = null
let lastValue = ""

function post(value) {
  lastValue = value
  if (timeoutId) clearTimeout(timeoutId)
  timeoutId = setTimeout(async () => {
    timeoutId = null
    postMessage(await searchIt(lastValue))
  }, waitMs)
}

async function flush(value) {
  if (timeoutId) {
    clearTimeout(timeoutId)
    timeoutId = null
  }
  postMessage(await searchIt(value))
}

onmessage = function(e) {
  const msg = e.data
  const query = msg.query || ''
  if (msg.flush) {
    flush(query)
  } else {
    post(query)
  }
}

if (maybeQuery == undefined) {
  searchIt("warmup")
}
// If it is defined, search.js is going to call us as soon as we return
// So we skip the warmup
