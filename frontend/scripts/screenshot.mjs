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
        { id: 1, benchmarkId: 'BRENT', provider: 'Platts', unit: 'USD/MT', family: 'Crude Oil (Brut)', logic: 'POL', zone: 'Europe', isFret: false },
        { id: 2, benchmarkId: 'DUBAI', provider: 'Argus', unit: 'USD/BBL', family: 'Crude Oil (Brut)', zone: 'Asia', isFret: false },
        { id: 3, benchmarkId: 'WTI', provider: 'Argus', unit: 'USD/BBL', family: null, isFret: false },
        { id: 4, benchmarkId: 'Unavailable', provider: null, unit: 'EUR/MWh', family: 'Jet Fuel', logic: 'POD', zone: 'Middle East', isFret: false },
        { id: 5, benchmarkId: 'FREIGHT', provider: 'Baltic', unit: 'USD/MT', family: 'Freight', zone: 'Atlantic', isFret: true },
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
  await page.getByLabel('Commodity').first().selectOption('Crude Oil (Brut)')
  assert.equal(await page.getByLabel('Shipment Price Logistic').inputValue(), 'POL')
  await page.getByLabel('Commodity').first().selectOption('Jet Fuel')
  assert.equal(await page.getByLabel('Shipment Price Logistic').inputValue(), 'POD')
  await page.getByLabel('Shipment Price Logistic').selectOption('POL')
  await page.getByLabel('Country of origin').fill('Oman')
  assert.equal(await page.getByLabel('Shipment Price Logistic').inputValue(), 'POL')
  await page.getByLabel('Commodity').first().selectOption('Crude Oil (Brut)')
  await page.getByLabel('Commodity').first().selectOption('Jet Fuel')
  assert.equal(await page.getByLabel('Shipment Price Logistic').inputValue(), 'POD')
  assert.deepEqual(
    await page.getByLabel('Commodity').first().locator('option').allTextContents(),
    ['Select a commodity', 'Crude Oil (Brut)', 'Jet Fuel'],
  )
  await page.getByLabel('Price unit', { exact: true }).selectOption('USD')
  await page.getByLabel('Quantity unit', { exact: true }).selectOption('BBL')
  assert.deepEqual(await page.getByLabel('Price unit', { exact: true }).locator('option').allTextContents(), ['EUR', 'USD'])
  assert.deepEqual(await page.getByLabel('Quantity unit', { exact: true }).locator('option').allTextContents(), ['MT', 'MWh', 'BBL'])
  await page.getByLabel('Price unit', { exact: true }).selectOption('EUR')
  assert.equal(await page.getByLabel('Quantity unit', { exact: true }).inputValue(), 'BBL')
  await page.getByLabel('Price unit', { exact: true }).selectOption('USD')
  await page.getByLabel('Declared price', { exact: true }).fill('750')
  await page.getByLabel('Quantity', { exact: true }).fill('10')
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
  assert.equal(await page.getByLabel('Commodity series').locator('option:checked').textContent(), 'Crude Oil (Brut) — Europe — Platts — USD/MT')
  assert.equal(await page.getByLabel('Fret series').locator('option:checked').textContent(), 'Freight — Atlantic — Baltic — USD/MT')
  await page.getByLabel('Window around shipment date').selectOption('7')
  await page.getByLabel('Quantity', { exact: true }).fill('0')
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByText('Enter a positive price and quantity.', { exact: true }).waitFor()
  await page.getByLabel('Quantity', { exact: true }).fill('10')
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByLabel('Show quantiles', { exact: true }).waitFor()
  assert.equal(await page.getByText('5 quotations analyzed.').count(), 0)
  assert.equal(metadataRequests, 1)
  assert.equal(await page.getByLabel('Show commodity price').isChecked(), false)
  assert.equal(await page.getByLabel('Show freight price').isChecked(), false)
  assert.equal(await page.getByLabel('Show quantiles').isChecked(), false)
  await page.getByLabel('Show commodity price').check()
  await page.getByLabel('Show freight price').check()
  await page.getByLabel('Show quantiles').check()
  await page.waitForTimeout(400)
  await page.getByLabel('Show differences (%)').uncheck()
  await page.getByLabel('Show differences (%)').check()

  await mkdir(new URL('../artifacts/', import.meta.url), { recursive: true })
  await page.screenshot({ path: outputPath.pathname, fullPage: true })
  const labels = page.locator('.quantile-label')
  assert.equal(await labels.count(), 5)
  const boxes = await labels.evaluateAll((elements) => elements.map((el) => {
    const box = el.getBoundingClientRect()
    return { top: box.top, bottom: box.bottom, left: box.left, right: box.right }
  }))
  for (let i = 1; i < boxes.length; i += 1) assert.ok(boxes[i].top >= boxes[i - 1].bottom)
  const chartBox = await page.locator('.chart').boundingBox()
  assert.ok(boxes.every((box) => box.left >= chartBox.x + chartBox.width - 170))
  for (const name of ['Q05', 'Q25', 'Q50', 'Q75', 'Q95']) {
    await page.getByRole('button', { name: 'Explain ' + name, exact: true }).hover()
    assert.ok(await page.getByRole('tooltip').filter({ hasText: 'percent of historical values below' }).filter({ visible: true }).count())
  }
  await page.screenshot({ path: new URL('../artifacts/quantile-help.png', import.meta.url).pathname, fullPage: true })
  for (const name of ['Show commodity price', 'Show freight price', 'Show differences (%)', 'Show quantiles']) {
    await page.getByLabel(name, { exact: true }).hover()
    assert.equal(await page.getByLabel(name, { exact: true }).locator('..').getByRole('tooltip').isVisible(), true)
  }
  await page.screenshot({ path: new URL('../artifacts/layer-help.png', import.meta.url).pathname, fullPage: true })
  await page.getByLabel('Show quantiles', { exact: true }).uncheck()
  assert.equal(await labels.count(), 0)
  await page.getByLabel('Show quantiles', { exact: true }).check()
  await page.setViewportSize({ width: 768, height: 1200 })
  await page.waitForTimeout(400)
  await page.mouse.move(0, 0)
  await page.screenshot({ path: new URL('../artifacts/quantiles-narrow.png', import.meta.url).pathname, fullPage: true })
  console.log(`Screenshot saved to ${outputPath.pathname}`)
} finally {
  await browser?.close()
  server.kill('SIGTERM')
}
