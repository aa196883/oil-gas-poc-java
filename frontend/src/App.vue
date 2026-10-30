<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AnalysisParameters from './components/AnalysisParameters.vue'
import AnalysisResults from './components/AnalysisResults.vue'
import OperationContext from './components/OperationContext.vue'

const series = ref([])
const countries = ref([])
const loading = ref(false)
const error = ref(null)
const operation = ref({
  commodity: '',
  logic: 'POL',
  incoterms: 'FOB',
  origin: 'Oman',
  destination: 'France',
  shippedOnBoard: '2026-07-15',
  referencePrice: 550,
  quantity: 1,
  unit: '',
})
const commoditySeries = computed(() => series.value.filter((item) => !item.isFret))
const fretSeries = computed(() => series.value.filter((item) => item.isFret))
const selectedFretSeriesId = ref(null)
const selectedSeriesId = ref(null)
const windowDays = ref(15)
const premium = ref(0)
const deltas = ref([])
const prices = ref([])
const commodityPrices = ref([])
const freightPrices = ref([])
const marginBounds = ref([])
const withinMargins = ref([])
const marginError = ref(null)
const resultReferencePrice = ref(0)
const resultUnit = ref('')
const units = computed(() =>
  [
    ...new Set(commoditySeries.value.map((item) => item.unit).filter((unit) => unit?.trim())),
  ].sort(),
)
const families = computed(() =>
  [
    ...new Set(commoditySeries.value.map((item) => item.family).filter((family) => family?.trim())),
  ].sort(),
)
watch(
  () => operation.value.commodity,
  (family) => {
    operation.value.logic =
      commoditySeries.value.find(
        (item) => item.family === family && ['POL', 'POD'].includes(item.logic),
      )?.logic ?? 'POL'
  },
)
const analyzing = ref(false)
const analysisError = ref(null)
const applyingContext = ref(false)
const contextError = ref(null)

async function applyContext() {
  applyingContext.value = true
  contextError.value = null
  const { commodity, logic, origin, destination, incoterms } = operation.value
  const country = logic === 'POL' ? origin : destination
  const includeFreight = ['CFR', 'CIF'].includes(incoterms)
  try {
    const findBenchmark = async (isFret) => {
      const params = new URLSearchParams({ country, family: commodity, isFret })
      const response = await fetch('/api/geography/benchmark?' + params)
      const result = await response.json()
      if (!response.ok) throw new Error(result.message || 'Could not find a benchmark.')
      return result.id
    }
    const [commodityId, freightId] = await Promise.all([
      findBenchmark(false),
      includeFreight ? findBenchmark(true) : Promise.resolve(null),
    ])
    selectedSeriesId.value = commodityId
    selectedFretSeriesId.value = freightId
  } catch (err) {
    contextError.value = err.message
  } finally {
    applyingContext.value = false
  }
}

async function loadSeries() {
  loading.value = true
  error.value = null
  try {
    const [seriesResponse, countriesResponse] = await Promise.all([
      fetch('/api/series'),
      fetch('/api/geography/countries'),
    ])
    if (!seriesResponse.ok) throw new Error(`HTTP ${seriesResponse.status}`)
    if (!countriesResponse.ok) throw new Error(`HTTP ${countriesResponse.status}`)
    series.value = await seriesResponse.json()
    countries.value = await countriesResponse.json()
    operation.value.unit = units.value.includes('USD/MT') ? 'USD/MT' : (units.value[0] ?? '')
    operation.value.commodity = families.value[0] ?? ''
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function shiftDate(dateString, days) {
  const date = new Date(`${dateString}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

const startDate = computed(() => shiftDate(operation.value.shippedOnBoard, -windowDays.value))
const endDate = computed(() => shiftDate(operation.value.shippedOnBoard, windowDays.value))

async function analyze({ start, end }) {
  if (selectedSeriesId.value === null || !operation.value.unit) return
  analyzing.value = true
  analysisError.value = null
  deltas.value = []
  prices.value = []
  commodityPrices.value = []
  freightPrices.value = []
  marginBounds.value = []
  withinMargins.value = []
  marginError.value = null
  try {
    const { unit } = operation.value
    const adjustment = Number(premium.value)
    if (!Number.isFinite(adjustment)) throw new Error('Enter a valid premium or discount.')
    const price = Number(operation.value.referencePrice)
    const quantity = Number(operation.value.quantity)
    const referencePrice = price / quantity
    if (
      !Number.isFinite(price) ||
      price <= 0 ||
      !Number.isFinite(quantity) ||
      quantity <= 0 ||
      !Number.isFinite(referencePrice) ||
      referencePrice <= 0
    ) {
      throw new Error('Enter a positive price and quantity.')
    }
    const params = new URLSearchParams({
      start,
      end,
      unit,
    })
    const includeFreight =
      (operation.value.incoterms === 'CFR' || operation.value.incoterms === 'CIF') &&
      selectedFretSeriesId.value !== null
    const readJson = async (response) => {
      const result = await response.json()
      if (!response.ok) throw new Error(result.message || `HTTP ${response.status}`)
      return result
    }
    const post = async (url, body) =>
      readJson(
        await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        }),
      )
    const transform = (prices, operations) => post('/api/prices/transform', { prices, operations })
    // Include earlier quotations so ADD_FREIGHT can carry the last known value forward.
    const freightParams = new URLSearchParams(params)
    freightParams.set('start', '0001-01-01')
    const [rawCommodityPoints, freightPoints] = await Promise.all([
      fetch(`/api/series/${selectedSeriesId.value}/prices?${params}`).then(readJson),
      includeFreight
        ? fetch(`/api/series/${selectedFretSeriesId.value}/prices?${freightParams}`).then(readJson)
        : Promise.resolve([]),
    ])
    const commodityPoints =
      adjustment === 0
        ? rawCommodityPoints
        : await transform(rawCommodityPoints, [{ type: 'ADD', value: adjustment }])
    const pricePoints = includeFreight
      ? await transform(commodityPoints, [{ type: 'ADD_FREIGHT', prices: freightPoints }])
      : commodityPoints

    const selectedSeries = series.value.find((item) => item.id === selectedSeriesId.value)
    const margins = includeFreight ? selectedSeries?.marginsWithFreight : selectedSeries?.margins
    const boundsRequest =
      Array.isArray(margins) &&
      margins.length === 2 &&
      margins.every((value) => typeof value === 'number' && Number.isFinite(value))
        ? Promise.all(
            margins.map((proportion) =>
              transform(pricePoints, [{ type: 'MULTIPLY', value: 1 + proportion }]),
            ),
          ).then(
            (bounds) => ({ bounds }),
            () => ({ error: 'Could not load tolerated margins. Run the analysis again to retry.' }),
          )
        : Promise.resolve({ error: 'Tolerated margins are unavailable for this series.' })
    const [deltaPoints, boundaryResult] = await Promise.all([
      post('/api/analysis/deltas', { prices: pricePoints, referencePrice }),
      boundsRequest,
    ])
    const [lowerBounds = [], upperBounds = []] = boundaryResult.bounds ?? []
    withinMargins.value = await post('/api/analysis/within-margins', {
      prices: pricePoints.map((point) => ({ date: point.date, price: referencePrice })),
      lowerBounds,
      upperBounds,
    })
    prices.value = pricePoints
    commodityPrices.value = includeFreight ? commodityPoints : []
    // Use the freight amounts actually applied to the market dates in the tooltip.
    freightPrices.value = includeFreight
      ? pricePoints.map((point, index) => ({
          date: point.date,
          price: Number(point.price) - Number(commodityPoints[index].price),
        }))
      : []
    deltas.value = deltaPoints
    marginBounds.value = boundaryResult.bounds ?? []
    marginError.value = boundaryResult.error ?? null
    resultReferencePrice.value = referencePrice
    resultUnit.value = unit
  } catch (err) {
    analysisError.value = err.message
  } finally {
    analyzing.value = false
  }
}

onMounted(loadSeries)
</script>

<template>
  <main class="page">
    <header class="page-header">
      <h1>Price consistency analysis</h1>
      <p>Compare declared prices with market quotations.</p>
    </header>
    <OperationContext
      v-model="operation"
      :units="units"
      :families="families"
      :countries="countries"
      :applying="applyingContext"
      :context-error="contextError"
      @apply="applyContext"
    />
    <AnalysisParameters
      v-model:selected-series-id="selectedSeriesId"
      v-model:window-days="windowDays"
      v-model:premium="premium"
      :unit="operation.unit"
      v-model:selected-fret-series-id="selectedFretSeriesId"
      :series="commoditySeries"
      :fret-series="fretSeries"
      :show-fret="operation.incoterms === 'CFR' || operation.incoterms === 'CIF'"
      :loading="loading || !operation.unit"
      :error="error"
      :start-date="startDate"
      :end-date="endDate"
      :analyzing="analyzing"
      @analyze="analyze"
    />
    <AnalysisResults
      :deltas="deltas"
      :prices="prices"
      :commodity-prices="commodityPrices"
      :freight-prices="freightPrices"
      :margin-bounds="marginBounds"
      :within-margins="withinMargins"
      :margin-error="marginError"
      :reference-price="resultReferencePrice"
      :unit="resultUnit"
      :analyzing="analyzing"
      :error="analysisError"
    />
  </main>
</template>

<style>
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  background: #f8fafc;
  color: #243247;
  font-family:
    Inter,
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
}
.page {
  max-width: 1100px;
  margin: 40px auto;
  padding: 0 24px;
}
.page-header {
  margin-bottom: 32px;
}
.page-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
}
.page-header p {
  margin: 0;
}
.card {
  margin-bottom: 24px;
  padding: 28px;
  border: 1px solid #e1e5ea;
  border-radius: 10px;
  background: white;
}
.card h2 {
  margin: 0 0 24px;
  font-size: 18px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}
input,
select {
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: white;
  color: inherit;
  font: inherit;
}
.error {
  color: #b91c1c;
}
@media (max-width: 650px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
