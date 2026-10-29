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
  let countryRequests = 0
  const benchmarkRequests = []
  let failBenchmark = false
  let failMargins = false
  const boundaryRequests = []
  const dates = ['2026-10-08', '2026-10-11', '2026-10-15', '2026-10-18', '2026-10-22']
  const commodityValues = [63.85, 67.2, 70.35, 74.7, 81.075]
  const marketValues = [68.85, 73.2, 76.35, 80.7, 87.075]
  const toPoints = (values) => dates.map((date, i) => ({ date, price: values[i] }))
  let expectedPremium = 0
  const adjusted = (values) => toPoints(values.map((value) => value + expectedPremium))
  const transformRequests = []
  const deltaRequests = []
  const removedRequests = []
  await page.route(
    /\/api\/series\/\d+\/(ajusted-prices|prices-with-freight|deltas|deltas-with-freight)\?/,
    (route) => {
      removedRequests.push(route.request().url())
      return route.fulfill({ status: 404, body: '{}' })
    },
  )
  await page.route('**/api/prices/transform', (route) => {
    assert.equal(route.request().method(), 'POST')
    const body = route.request().postDataJSON()
    transformRequests.push(body)
    assert.equal(body.operations.length, 1)
    const operation = body.operations[0]
    if (operation.type === 'ADD') {
      assert.deepEqual(body.prices, toPoints(commodityValues))
      assert.equal(operation.value, expectedPremium)
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(adjusted(commodityValues)),
      })
    }
    if (operation.type === 'ADD_FREIGHT') {
      assert.deepEqual(body.prices, adjusted(commodityValues))
      assert.equal(operation.prices[0].date, '2026-10-07')
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(adjusted(marketValues)),
      })
    }
    assert.equal(operation.type, 'MULTIPLY')
    boundaryRequests.push(body)
    const withFreight = body.prices[0].price === marketValues[0] + expectedPremium
    assert.deepEqual(body.prices, adjusted(withFreight ? marketValues : commodityValues))
    assert.ok((withFreight ? [0.8, 1.15] : [0.85, 1.1]).includes(operation.value))
    // Non-proportional responses verify that chart boundaries remain server-supplied.
    const values = operation.value < 1 ? [54, 61, 59, 75, 78] : [82, 85, 74, 90, 103]
    return route.fulfill({
      status: failMargins ? 503 : 200,
      contentType: 'application/json',
      body: JSON.stringify(failMargins ? {} : toPoints(values)),
    })
  })
  await page.route('**/api/analysis/deltas', (route) => {
    assert.equal(route.request().method(), 'POST')
    const body = route.request().postDataJSON()
    deltaRequests.push(body)
    assert.equal(body.referencePrice, 75)
    const withFreight = body.prices[0].price === marketValues[0] + expectedPremium
    assert.deepEqual(body.prices, adjusted(withFreight ? marketValues : commodityValues))
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(
        body.prices.map((point) => ({
          date: point.date,
          delta: Number((((point.price - 75) / 75) * 100).toFixed(4)),
        })),
      ),
    })
  })
  await page.route('**/api/geography/benchmark?*', (route) => {
    const params = Object.fromEntries(new URL(route.request().url()).searchParams)
    benchmarkRequests.push(params)
    return route.fulfill({
      status: failBenchmark ? 404 : 200,
      contentType: 'application/json',
      body: JSON.stringify(
        failBenchmark
          ? { message: 'No matching benchmark.' }
          : {
              id: params.isFret === 'true' ? 5 : params.country === 'Oman' ? 2 : 1,
            },
      ),
    })
  })

  await page.route('**/api/geography/countries', (route) => {
    countryRequests += 1
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(['France', 'Germany', 'Oman', 'United Kingdom', 'United States']),
    })
  })

  await page.route('**/api/series', (route) => {
    metadataRequests += 1
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 1,
          benchmarkId: 'BRENT',
          provider: 'Platts',
          unit: 'USD/MT',
          family: 'Crude Oil (Brut)',
          logic: 'POL',
          zone: 'Europe',
          margins: [-0.15, 0.1],
          marginsWithFreight: [-0.2, 0.15],
          isFret: false,
        },
        {
          id: 2,
          benchmarkId: 'DUBAI',
          provider: 'Argus',
          unit: 'USD/BBL',
          family: 'Crude Oil (Brut)',
          zone: 'Asia',
          isFret: false,
        },
        {
          id: 3,
          benchmarkId: 'WTI',
          provider: 'Argus',
          unit: 'USD/BBL',
          family: null,
          isFret: false,
        },
        {
          id: 4,
          benchmarkId: 'Unavailable',
          provider: null,
          unit: 'EUR/MWh',
          family: 'Jet Fuel',
          logic: 'POD',
          zone: 'Middle East',
          isFret: false,
        },
        {
          id: 5,
          benchmarkId: 'FREIGHT',
          provider: 'Baltic',
          unit: 'USD/MT',
          family: 'Freight',
          zone: 'Atlantic',
          isFret: true,
        },
      ]),
    })
  })
  await page.route('**/api/series/*/prices?*', (route) => {
    const url = new URL(route.request().url())
    assert.equal(url.searchParams.get('freightId'), null)
    assert.equal(url.searchParams.get('unit'), 'USD/BBL')
    const prices =
      url.pathname === '/api/series/5/prices'
        ? [
            { date: '2026-10-08', price: 5 },
            { date: '2026-10-11', price: 6 },
            { date: '2026-10-15', price: 6 },
            { date: '2026-10-18', price: 6 },
            { date: '2026-10-22', price: 6 },
          ]
        : [
            { date: '2026-10-08', price: 63.85 },
            { date: '2026-10-11', price: 67.2 },
            { date: '2026-10-15', price: 70.35 },
            { date: '2026-10-18', price: 74.7 },
            { date: '2026-10-22', price: 81.075 },
          ]
    assert.ok(
      ['/api/series/1/prices', '/api/series/3/prices', '/api/series/5/prices'].includes(
        url.pathname,
      ),
    )
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(prices),
    })
  })

  await page.route('**/api/series/5/prices?*', (route) => {
    const url = new URL(route.request().url())
    assert.equal(url.searchParams.get('unit'), 'USD/BBL')
    assert.equal(url.searchParams.get('start'), '0001-01-01')
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([
        { date: '2026-10-07', price: 5 },
        { date: '2026-10-11', price: 6 },
      ]),
    })
  })

  await page.goto(baseUrl)
  assert.equal(countryRequests, 1)
  await page.getByLabel('Country of origin').fill('Uni')
  assert.deepEqual(await page.locator('#origin-countries button').allTextContents(), [
    'United Kingdom',
    'United States',
  ])
  await page.screenshot({
    path: new URL('../artifacts/country-filter.png', import.meta.url).pathname,
    fullPage: true,
  })
  await page.getByRole('button', { name: 'United Kingdom', exact: true }).click()
  assert.equal(await page.getByLabel('Country of origin').inputValue(), 'United Kingdom')
  await page.getByLabel('Country of origin').fill('Oman')
  await page.getByLabel('Country of destination').fill('Fra')
  assert.deepEqual(await page.locator('#destination-countries button').allTextContents(), [
    'France',
  ])
  await page.getByRole('button', { name: 'France', exact: true }).click()
  assert.equal(await page.getByLabel('Country of destination').inputValue(), 'France')
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
  assert.deepEqual(await page.getByLabel('Commodity').first().locator('option').allTextContents(), [
    'Select a commodity',
    'Crude Oil (Brut)',
    'Jet Fuel',
  ])
  await page.getByLabel('Price unit', { exact: true }).selectOption('USD')
  await page.getByLabel('Quantity unit', { exact: true }).selectOption('BBL')
  assert.deepEqual(
    await page.getByLabel('Price unit', { exact: true }).locator('option').allTextContents(),
    ['EUR', 'USD'],
  )
  assert.deepEqual(
    await page.getByLabel('Quantity unit', { exact: true }).locator('option').allTextContents(),
    ['MT', 'MWh', 'BBL'],
  )
  await page.getByLabel('Price unit', { exact: true }).selectOption('EUR')
  assert.equal(await page.getByLabel('Quantity unit', { exact: true }).inputValue(), 'BBL')
  await page.getByLabel('Price unit', { exact: true }).selectOption('USD')
  await page.getByLabel('Declared price', { exact: true }).fill('750')
  await page.getByLabel('Quantity', { exact: true }).fill('10')
  assert.equal(await page.getByLabel('Incoterms').inputValue(), 'FOB')
  assert.deepEqual(await page.getByLabel('Incoterms').locator('option').allTextContents(), [
    'FAS',
    'FOB',
    'CFR',
    'CIF',
  ])
  assert.equal(await page.getByLabel('Fret series').count(), 0)
  assert.deepEqual(
    await page
      .getByLabel('Commodity series')
      .locator('option')
      .evaluateAll((options) => options.map((option) => option.value)),
    ['', '1', '2', '3', '4'],
  )
  for (const incoterm of ['CFR', 'CIF', 'FAS', 'FOB', 'CIF']) {
    await page.getByLabel('Incoterms').selectOption(incoterm)
    assert.equal(
      await page.getByLabel('Fret series').count(),
      ['CFR', 'CIF'].includes(incoterm) ? 1 : 0,
    )
  }
  assert.deepEqual(
    await page
      .getByLabel('Fret series')
      .locator('option')
      .evaluateAll((options) => options.map((option) => option.value)),
    ['', '5'],
  )
  await page.getByLabel('Commodity').first().selectOption('Crude Oil (Brut)')
  await page.getByLabel('Shipment Price Logistic').selectOption('POL')
  await page.getByLabel('Incoterms').selectOption('FOB')
  const apply = async () => {
    await page.getByRole('button', { name: 'Apply Context', exact: true }).click()
    await page.getByRole('button', { name: 'Apply Context', exact: true }).waitFor()
  }
  await apply()
  assert.equal(await page.getByLabel('Commodity series').inputValue(), '2')
  assert.deepEqual(benchmarkRequests.splice(0), [
    { country: 'Oman', family: 'Crude Oil (Brut)', isFret: 'false' },
  ])
  await page.getByLabel('Commodity series').selectOption('1')
  assert.equal(await page.getByLabel('Commodity series').inputValue(), '1')
  await page.getByLabel('Shipment Price Logistic').selectOption('POD')
  await page.getByLabel('Incoterms').selectOption('CIF')
  await apply()
  assert.equal(await page.getByLabel('Commodity series').inputValue(), '1')
  assert.equal(await page.getByLabel('Fret series').inputValue(), '5')
  assert.deepEqual(
    benchmarkRequests.splice(0).sort((a, b) => a.isFret.localeCompare(b.isFret)),
    [
      { country: 'France', family: 'Crude Oil (Brut)', isFret: 'false' },
      { country: 'France', family: 'Crude Oil (Brut)', isFret: 'true' },
    ],
  )
  failBenchmark = true
  await apply()
  await page.getByRole('alert').filter({ hasText: 'No matching benchmark.' }).waitFor()
  assert.equal(await page.getByLabel('Commodity series').inputValue(), '1')
  assert.equal(await page.getByLabel('Fret series').inputValue(), '5')
  failBenchmark = false
  await page.getByLabel('Incoterms').selectOption('FAS')
  await apply()
  await page.getByLabel('Incoterms').selectOption('CFR')
  assert.equal(await page.getByLabel('Fret series').inputValue(), '')
  await apply()
  assert.equal(await page.getByLabel('Fret series').inputValue(), '5')
  assert.equal(
    await page.getByLabel('Commodity series').locator('option:checked').textContent(),
    'Crude Oil (Brut) — Europe — Platts — USD/MT',
  )
  assert.equal(
    await page.getByLabel('Fret series').locator('option:checked').textContent(),
    'Freight — Atlantic — Baltic — USD/MT',
  )
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
  assert.equal(await page.getByLabel('Show freight price').count(), 0)
  assert.equal(await page.getByLabel('Show quantiles').isChecked(), false)
  // Hidden commodity and difference layers must still supply all tooltip values.
  await page.getByLabel('Show differences (%)').uncheck()
  const hoverMarket = async () => {
    await page.locator('.chart').scrollIntoViewIfNeeded()
    const point = await page.evaluate(async () => {
      const source = await (await fetch('/src/components/PriceChart.vue')).text()
      const echarts = await import(source.match(/from ["']([^"']*echarts[^"']*)["']/)[1])
      const chart = echarts.getInstanceByDom(document.querySelector('.chart'))
      const series = chart.getOption().series
      assertNoFreightSeries(series)
      function assertNoFreightSeries(items) {
        if (items.some((item) => item.name === 'Freight price'))
          throw new Error('Freight graph still exists')
      }
      const market = series.find((item) =>
        ['Price with freight', 'Market price'].includes(item.name),
      )
      return chart.convertToPixel({ seriesIndex: series.indexOf(market) }, market.data[2])
    })
    const box = await page.locator('.chart').boundingBox()
    await page.mouse.move(box.x + point[0], box.y + point[1])
  }
  await hoverMarket()
  for (const text of [
    'Price with freight: 76.35 USD/BBL',
    'Commodity price: 70.35 USD/BBL',
    'Freight price: 6.00 USD/BBL',
    'Declared price: 75.00 USD/BBL',
    'Difference: +1.80 %',
  ]) {
    await page.getByText(text, { exact: false }).last().waitFor({ state: 'visible' })
  }
  await page.locator('.chart-container').screenshot({
    path: new URL('../artifacts/all-values-tooltip.png', import.meta.url).pathname,
  })
  await page.getByLabel('Show differences (%)').check()
  const marginToggle = page.getByLabel('Show tolerated margins', { exact: true })
  assert.equal(await marginToggle.isChecked(), false)
  assert.deepEqual(
    boundaryRequests.map((request) => request.operations[0].value).sort(),
    [0.8, 1.15],
  )
  assert.equal(
    transformRequests.filter((request) => request.operations[0].type === 'ADD_FREIGHT').length,
    1,
  )
  assert.equal(deltaRequests.length, 1)
  const readBand = () =>
    page.evaluate(async () => {
      const source = await (await fetch('/src/components/PriceChart.vue')).text()
      const moduleUrl = source.match(/from ["']([^"']*echarts[^"']*)["']/)[1]
      const echarts = await import(moduleUrl)
      return echarts
        .getInstanceByDom(document.querySelector('.chart'))
        .getOption()
        .series.find((series) => series.name === 'Tolerated margins').data
    })
  const readDifferenceColors = () =>
    page.evaluate(async () => {
      const source = await (await fetch('/src/components/PriceChart.vue')).text()
      const echarts = await import(source.match(/from ["']([^"']*echarts[^"']*)["']/)[1])
      const chart = echarts.getInstanceByDom(document.querySelector('.chart'))
      const model = chart
        .getModel()
        .getSeries()
        .find((series) => series.name === 'Differences')
      const data = model.getData()
      return Array.from(
        { length: data.count() },
        (_, index) => data.getItemGraphicEl(index).style.stroke,
      )
    })
  // Declared price is 75: inside, inside, above, exactly on lower bound, below.
  const expectedColors = ['#22c55e', '#22c55e', '#ef4444', '#22c55e', '#ef4444']
  assert.deepEqual(await readDifferenceColors(), expectedColors)
  assert.deepEqual(await readBand(), [])
  await marginToggle.check()
  await page.waitForTimeout(400)
  assert.deepEqual(await readDifferenceColors(), expectedColors)
  const band = await readBand()
  assert.equal(band.length, 4)
  assert.deepEqual(band[0], [
    Date.parse(dates[0] + 'T12:00:00Z'),
    54,
    82,
    Date.parse(dates[1] + 'T12:00:00Z'),
    61,
    85,
  ])
  await page.mouse.move(0, 0)
  await page.locator('.chart-container').screenshot({
    path: new URL('../artifacts/margins-freight.png', import.meta.url).pathname,
  })
  await marginToggle.uncheck()
  assert.deepEqual(await readBand(), [])
  await marginToggle.check()
  await page.getByLabel('Show commodity price').check()
  await page.getByLabel('Show quantiles').check()
  await page.getByLabel('Show average price').check()
  await page.getByText('Average Price : 77.23 USD/BBL', { exact: true }).waitFor()
  assert.match(await page.locator('.additional-information').textContent(), /77.23/)
  assert.match(await page.locator('.additional-information').textContent(), /\+2.23/)
  await page.waitForTimeout(400)
  await page.getByLabel('Show differences (%)').uncheck()
  await page.getByLabel('Show differences (%)').check()

  await mkdir(new URL('../artifacts/', import.meta.url), { recursive: true })
  await page.mouse.move(0, 0)
  await page.locator(':focus').evaluateAll((elements) => elements.forEach((el) => el.blur()))
  await page.screenshot({ path: outputPath.pathname, fullPage: true })
  const labels = page.locator('.quantile-label')
  assert.equal(await labels.count(), 5)
  const boxes = await page.locator('.reference-label').evaluateAll((elements) =>
    elements.map((el) => {
      const box = el.getBoundingClientRect()
      return { top: box.top, bottom: box.bottom, left: box.left, right: box.right }
    }),
  )
  for (let i = 1; i < boxes.length; i += 1) assert.ok(boxes[i].top >= boxes[i - 1].bottom)
  const chartBox = await page.locator('.chart').boundingBox()
  assert.ok(boxes.every((box) => box.left >= chartBox.x + chartBox.width - 260))
  await page.getByLabel('Show average price').uncheck()
  assert.equal(await page.getByText('Average Price : 77.23 USD/BBL', { exact: true }).count(), 0)
  await page.getByLabel('Show average price').check()
  for (const name of ['Q05', 'Q25', 'Q50', 'Q75', 'Q95']) {
    await page.getByRole('button', { name: 'Explain ' + name, exact: true }).hover()
    assert.ok(
      await page
        .getByRole('tooltip')
        .filter({ hasText: 'percent of historical values below' })
        .filter({ visible: true })
        .count(),
    )
  }
  await page.screenshot({
    path: new URL('../artifacts/quantile-help.png', import.meta.url).pathname,
    fullPage: true,
  })
  for (const name of ['Show commodity price', 'Show differences (%)', 'Show quantiles']) {
    await page.getByLabel(name, { exact: true }).hover()
    assert.equal(
      await page.getByLabel(name, { exact: true }).locator('..').getByRole('tooltip').isVisible(),
      true,
    )
  }
  await page.screenshot({
    path: new URL('../artifacts/layer-help.png', import.meta.url).pathname,
    fullPage: true,
  })
  await page.getByLabel('Show quantiles', { exact: true }).uncheck()
  assert.equal(await labels.count(), 0)
  await page.getByLabel('Show quantiles', { exact: true }).check()
  await page.setViewportSize({ width: 768, height: 1200 })
  await page.waitForTimeout(400)
  await page.mouse.move(0, 0)
  await page.screenshot({
    path: new URL('../artifacts/quantiles-narrow.png', import.meta.url).pathname,
    fullPage: true,
  })
  await page.getByLabel('Incoterms').selectOption('FOB')
  boundaryRequests.length = 0
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await marginToggle.waitFor()
  assert.equal(await marginToggle.isChecked(), false)
  assert.deepEqual(
    boundaryRequests.map((request) => request.operations[0].value).sort(),
    [0.85, 1.1],
  )
  assert.deepEqual(deltaRequests.at(-1).prices, toPoints(commodityValues))
  await marginToggle.check()
  await page.waitForTimeout(400)
  assert.equal((await readBand()).length, 4)
  await page.mouse.move(0, 0)
  await page.locator('.chart-container').screenshot({
    path: new URL('../artifacts/margins-narrow.png', import.meta.url).pathname,
  })
  failMargins = true
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByRole('status').filter({ hasText: 'Could not load tolerated margins' }).waitFor()
  assert.equal(await marginToggle.isDisabled(), true)
  assert.deepEqual(await readDifferenceColors(), Array(5).fill('#ef4444'))
  assert.deepEqual(await readBand(), [])
  failMargins = false
  await page.getByLabel('Commodity series').selectOption('3')
  boundaryRequests.length = 0
  await page.getByRole('button', { name: 'Analyze prices' }).click()
  await page.getByRole('status').filter({ hasText: 'unavailable for this series' }).waitFor()
  assert.equal(await marginToggle.isDisabled(), true)
  assert.equal(boundaryRequests.length, 0)
  assert.equal(
    transformRequests.some((request) => request.operations[0].type === 'ADD'),
    false,
  )
  await page.getByLabel('Commodity series').selectOption('1')
  const premiumInput = page.getByLabel('Premium / discount', { exact: true })
  assert.equal(await premiumInput.inputValue(), '0')
  await page.getByLabel('Price unit', { exact: true }).selectOption('EUR')
  assert.match(await premiumInput.locator('..').textContent(), /EUR\/BBL/)
  await page.getByLabel('Price unit', { exact: true }).selectOption('USD')
  assert.match(await premiumInput.locator('..').textContent(), /USD\/BBL/)
  for (const [amount, incoterm] of [
    [2.5, 'FOB'],
    [-3, 'CFR'],
  ]) {
    expectedPremium = amount
    await premiumInput.fill(String(amount))
    await page.getByLabel('Incoterms').selectOption(incoterm)
    if (incoterm === 'CFR') await page.getByLabel('Fret series').selectOption('5')
    transformRequests.length = 0
    await page.getByRole('button', { name: 'Analyze prices' }).click()
    await marginToggle.waitFor()
    assert.deepEqual(
      transformRequests.map((request) => request.operations[0].type),
      incoterm === 'CFR'
        ? ['ADD', 'ADD_FREIGHT', 'MULTIPLY', 'MULTIPLY']
        : ['ADD', 'MULTIPLY', 'MULTIPLY'],
    )
    assert.deepEqual(
      deltaRequests.at(-1).prices,
      adjusted(incoterm === 'CFR' ? marketValues : commodityValues),
    )
  }
  await page.mouse.move(0, 0)
  await page.screenshot({
    path: new URL('../artifacts/premium-discount.png', import.meta.url).pathname,
    fullPage: true,
  })
  assert.deepEqual(removedRequests, [])
  console.log(`Screenshot saved to ${outputPath.pathname}`)
} finally {
  await browser?.close()
  server.kill('SIGTERM')
}
