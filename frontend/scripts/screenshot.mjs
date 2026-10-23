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
        { id: 1, benchmarkId: 'BRENT', provider: 'Platts', unit: 'USD/MT' },
        { id: 2, benchmarkId: 'DUBAI', provider: 'Argus', unit: 'USD/BBL' },
        { id: 3, benchmarkId: 'WTI', provider: 'Argus', unit: 'USD/BBL' },
        { id: 4, benchmarkId: 'Unavailable', provider: null, unit: null },
      ]),
    })
  })
  await page.route('**/api/series/*/prices?*', (route) => {
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
  await page.route('**/api/series/*/deltas?*', (route) => {
    const params = new URL(route.request().url()).searchParams
    assert.equal(params.get('unit'), 'USD/BBL')
    assert.equal(params.get('referencePrice'), '75')
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
  await page.getByLabel('Declared price unit').selectOption('USD/BBL')
  assert.equal(await page.getByLabel('Declared price unit').locator('option').count(), 3)
  await page.getByLabel('Declared price', { exact: true }).fill('75')
  await page.getByLabel('Quotation series').selectOption('1')
  await page.getByLabel('Window around shipment date').selectOption('7')
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByText('5 quotations analyzed.').waitFor()
  assert.equal(metadataRequests, 1)
  await page.getByLabel('Show differences (%)').uncheck()
  await page.getByLabel('Show differences (%)').check()

  await mkdir(new URL('../artifacts/', import.meta.url), { recursive: true })
  await page.screenshot({ path: outputPath.pathname, fullPage: true })
  console.log(`Screenshot saved to ${outputPath.pathname}`)
} finally {
  await browser?.close()
  server.kill('SIGTERM')
}
