import process from 'node:process'
import { mkdir } from 'node:fs/promises'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'
import assert from 'node:assert/strict'

const host = '127.0.0.1'
const port = 4173
const baseUrl = `http://${host}:${port}`
const outputPath = new URL('../artifacts/frontend.png', import.meta.url)

const server = spawn(
  process.execPath,
  ['./node_modules/vite/bin/vite.js', '--host', host, '--port', String(port)],
  { stdio: 'ignore' },
)

async function waitForServer() {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    try {
      const response = await fetch(baseUrl)
      if (response.ok) return
    } catch {
      // The development server is still starting.
    }
    await new Promise((resolve) => setTimeout(resolve, 200))
  }
  throw new Error(`Vite did not start at ${baseUrl}`)
}

let browser

try {
  await waitForServer()
  browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 } })
  let metadataRequests = 0

  await page.route('**/api/series', (route) => {
    metadataRequests += 1
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, benchmarkId: 'BRENT', provider: 'Platts', unit: 'USD/MT', family: 'Crude Oil (Brut)', isFret: false },
        { id: 2, benchmarkId: 'DUBAI', provider: 'Argus', unit: 'USD/BBL', family: 'Crude Oil (Brut)', isFret: false },
        { id: 3, benchmarkId: 'WTI', provider: 'Argus', unit: 'USD/BBL', family: null, isFret: false },
        { id: 4, benchmarkId: 'Unavailable', provider: null, unit: null, family: 'Jet Fuel', isFret: false },
        { id: 5, benchmarkId: 'FREIGHT', provider: 'Baltic', unit: 'USD/MT', family: 'Freight', isFret: true },
      ]),
    })
  })
  await page.route('**/api/series/*/prices-with-freight?*', (route) => {
    assert.equal(new URL(route.request().url()).pathname, '/api/series/1/prices-with-freight')
    assert.equal(new URL(route.request().url()).searchParams.get('freightId'), '5')
    assert.equal(new URL(route.request().url()).searchParams.get('unit'), 'USD/BBL')
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        { date: '2026-10-08', price: 68.85 },
        { date: '2026-10-11', price: 73.2 },
        { date: '2026-10-15', price: 76.35 },
        { date: '2026-10-18', price: 80.7 },
        { date: '2026-10-22', price: 87.075 },
      ]),
    })
  })
  await page.route("**/api/series/*/prices?*", (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.endsWith("/prices-with-freight")) return route.fallback()
    assert.equal(url.searchParams.get("freightId"), null)
    assert.equal(url.searchParams.get("unit"), "USD/BBL")
    const prices = url.pathname === "/api/series/5/prices"
      ? [
          { date: "2026-10-08", price: 5 },
          { date: "2026-10-11", price: 6 },
          { date: "2026-10-15", price: 6 },
          { date: "2026-10-18", price: 6 },
          { date: "2026-10-22", price: 6 },
        ]
      : [
          { date: "2026-10-08", price: 63.85 },
          { date: "2026-10-11", price: 67.2 },
          { date: "2026-10-15", price: 70.35 },
          { date: "2026-10-18", price: 74.7 },
          { date: "2026-10-22", price: 81.075 },
        ]
    assert.ok(["/api/series/1/prices", "/api/series/5/prices"].includes(url.pathname))
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(prices),
    })
  })

  await page.route("**/api/series/5/prices?*", (route) => {
    const url = new URL(route.request().url())
    assert.equal(url.searchParams.get("unit"), "USD/BBL")
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify([
        { date: "2026-10-08", price: 5 },
        { date: "2026-10-11", price: 6 },
        { date: "2026-10-15", price: 6 },
        { date: "2026-10-18", price: 6 },
        { date: "2026-10-22", price: 6 },
      ]),
    })
  })

  await page.route('**/api/series/*/deltas-with-freight?*', (route) => {
    assert.equal(new URL(route.request().url()).pathname, '/api/series/1/deltas-with-freight')
    const params = new URL(route.request().url()).searchParams
    assert.equal(params.get('unit'), 'USD/BBL')
    assert.equal(params.get('referencePrice'), '75')
    assert.equal(params.get('freightId'), '5')
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        { date: '2026-10-08', delta: -8.2 },
        { date: '2026-10-11', delta: -2.4 },
        { date: '2026-10-15', delta: 1.8 },
        { date: '2026-10-18', delta: 7.6 },
        { date: '2026-10-22', delta: 16.1 },
      ]),
    })
  })

  await page.goto(baseUrl)
  await page.getByLabel('Commodity').first().selectOption('Jet Fuel')
  assert.deepEqual(
    await page.getByLabel('Commodity').first().locator('option').allTextContents(),
    ['Select a commodity', 'Crude Oil (Brut)', 'Jet Fuel'],
  )
  await page.getByLabel('Declared price unit').selectOption('USD/BBL')
  assert.equal(await page.getByLabel('Declared price unit').locator('option').count(), 3)
  await page.getByLabel('Declared price', { exact: true }).fill('75')
  assert.equal(await page.getByLabel('Incoterms').inputValue(), 'FOB')
  assert.deepEqual(await page.getByLabel('Incoterms').locator('option').allTextContents(),
    ['FAS', 'FOB', 'CFR', 'CIF'])
  assert.equal(await page.getByLabel('Fret series').count(), 0)
  assert.deepEqual(await page.getByLabel('Commodity series').locator('option').evaluateAll(
    (options) => options.map((option) => option.value)), ['', '1', '2', '3', '4'])
  for (const incoterm of ['CFR', 'CIF', 'FAS', 'FOB', 'CIF']) {
    await page.getByLabel('Incoterms').selectOption(incoterm)
    assert.equal(await page.getByLabel('Fret series').count(),
      ['CFR', 'CIF'].includes(incoterm) ? 1 : 0)
  }
  assert.deepEqual(await page.getByLabel('Fret series').locator('option').evaluateAll(
    (options) => options.map((option) => option.value)), ['', '5'])
  await page.getByLabel('Fret series').selectOption('5')
  await page.getByLabel('Commodity series').selectOption('1')
  await page.getByLabel('Window around shipment date').selectOption('7')
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByText('5 quotations analyzed.').waitFor()
  assert.equal(metadataRequests, 1)
  assert.equal(await page.getByLabel('Show commodity price').isChecked(), false)
  assert.equal(await page.getByLabel('Show freight price').isChecked(), false)
  await page.getByLabel('Show commodity price').check()
  await page.getByLabel('Show freight price').check()
  await page.waitForTimeout(400)
  await page.getByLabel('Show differences (%)').uncheck()
  await page.getByLabel('Show differences (%)').check()

  await mkdir(new URL('../artifacts/', import.meta.url), { recursive: true })
  await page.screenshot({ path: outputPath.pathname, fullPage: true })
  console.log(`Screenshot saved to ${outputPath.pathname}`)
} finally {
  await browser?.close()
  server.kill('SIGTERM')
}
