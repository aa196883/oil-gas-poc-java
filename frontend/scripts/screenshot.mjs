import { mkdir } from 'node:fs/promises'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'

const host = '127.0.0.1'
const port = 4173
const baseUrl = `http://${host}:${port}`
const outputPath = new URL('../artifacts/frontend.png', import.meta.url)

const server = spawn(
  process.execPath,
  ['./node_modules/vite/bin/vite.js', '--host', host, '--port', String(port)],
  { stdio: 'ignore' }
)

async function waitForServer() {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    try {
      const response = await fetch(baseUrl)
      if (response.ok) return
    } catch {
      // The development server is still starting.
    }
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  throw new Error(`Vite did not start at ${baseUrl}`)
}

let browser

try {
  await waitForServer()
  browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 } })

  await page.route('**/api/series', route =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        { id: 1, benchmarkId: 'BRENT', provider: 'Platts', unit: 'USD/MT' },
        { id: 2, benchmarkId: 'DUBAI', provider: 'Argus', unit: 'USD/MT' },
      ]),
    })
  )
  await page.route('**/api/series/*/deltas?*', route =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        { date: '2026-10-08', delta: -8.2 },
        { date: '2026-10-11', delta: -2.4 },
        { date: '2026-10-15', delta: 1.8 },
        { date: '2026-10-18', delta: 7.6 },
        { date: '2026-10-22', delta: 16.1 },
      ]),
    })
  )

  await page.goto(baseUrl)
  await page.getByLabel('Declared price (USD/MT)').fill('575')
  await page.getByLabel('Quotation series').selectOption('1')
  await page.getByLabel('Window around shipment date').selectOption('7')
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByText('5 quotations analyzed.').waitFor()
  await page.getByLabel('Show differences (%)').uncheck()
  await page.getByLabel('Show differences (%)').check()

  await mkdir(new URL('../artifacts/', import.meta.url), { recursive: true })
  await page.screenshot({ path: outputPath.pathname, fullPage: true })
  console.log(`Screenshot saved to ${outputPath.pathname}`)
} finally {
  await browser?.close()
  server.kill('SIGTERM')
}
