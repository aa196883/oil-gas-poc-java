<script setup>
import { computed, onMounted, ref } from 'vue'
import AnalysisParameters from './components/AnalysisParameters.vue'
import AnalysisResults from './components/AnalysisResults.vue'
import OperationContext from './components/OperationContext.vue'

const series = ref([])
const loading = ref(false)
const error = ref(null)
const operation = ref({
  commodity: '',
  incoterms: 'FOB',
  origin: 'Oman',
  destination: 'France',
  shippedOnBoard: '2026-10-15',
  referencePrice: 550,
  unit: '',
})
const commoditySeries = computed(() => series.value.filter((item) => !item.isFret))
const fretSeries = computed(() => series.value.filter((item) => item.isFret))
const selectedFretSeriesId = ref(null)
const selectedSeriesId = ref(null)
const windowDays = ref(15)
const deltas = ref([])
const prices = ref([])
const resultReferencePrice = ref(0)
const resultUnit = ref('')
const units = computed(() =>
  [...new Set(commoditySeries.value.map((item) => item.unit).filter((unit) => unit?.trim()))].sort(),
)
const families = computed(() =>
  [...new Set(commoditySeries.value.map((item) => item.family).filter((family) => family?.trim()))].sort(),
)
const analyzing = ref(false)
const analysisError = ref(null)

async function loadSeries() {
  loading.value = true
  error.value = null
  try {
    const response = await fetch('/api/series')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    series.value = await response.json()
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

async function analyze() {
  if (selectedSeriesId.value === null || !operation.value.unit) return
  analyzing.value = true
  analysisError.value = null
  deltas.value = []
  prices.value = []
  try {
    const { referencePrice, unit } = operation.value
    const params = new URLSearchParams({
      start: startDate.value,
      end: endDate.value,
      unit,
    })
    const includeFreight =
      (operation.value.incoterms === 'CFR' || operation.value.incoterms === 'CIF') &&
      selectedFretSeriesId.value !== null
    if (includeFreight) params.set('freightId', selectedFretSeriesId.value)
    const deltaParams = new URLSearchParams(params)
    deltaParams.set('referencePrice', referencePrice)
    const priceEndpoint = includeFreight ? 'prices-with-freight' : 'prices'
    const deltaEndpoint = includeFreight ? 'deltas-with-freight' : 'deltas'
    const responses = await Promise.all([
      fetch(`/api/series/${selectedSeriesId.value}/${priceEndpoint}?${params}`),
      fetch("/api/series/" + selectedSeriesId.value + "/" + deltaEndpoint + "?" + deltaParams),
    ])
    for (const response of responses) {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
    }
    const [pricePoints, deltaPoints] = await Promise.all(
      responses.map((response) => response.json()),
    )
    prices.value = pricePoints
    deltas.value = deltaPoints
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
    <OperationContext v-model="operation" :units="units" :families="families" />
    <AnalysisParameters
      v-model:selected-series-id="selectedSeriesId"
      v-model:window-days="windowDays"
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
