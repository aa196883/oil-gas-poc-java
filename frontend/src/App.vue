<script setup>
import { computed, onMounted, ref } from 'vue'
import AnalysisParameters from './components/AnalysisParameters.vue'
import AnalysisResults from './components/AnalysisResults.vue'
import OperationContext from './components/OperationContext.vue'

const series = ref([])
const loading = ref(false)
const error = ref(null)
const operation = ref({
  commodity: 'Crude Oil',
  origin: 'Oman',
  destination: 'France',
  shippedOnBoard: '2026-10-15',
  referencePrice: 550,
})
const selectedSeriesId = ref(null)
const windowDays = ref(15)
const deltas = ref([])
const analyzing = ref(false)
const analysisError = ref(null)

async function loadSeries() {
  loading.value = true
  error.value = null
  try {
    const response = await fetch('/api/series')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    series.value = await response.json()
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
  if (selectedSeriesId.value === null) return
  analyzing.value = true
  analysisError.value = null
  deltas.value = []
  try {
    const params = new URLSearchParams({
      start: startDate.value,
      end: endDate.value,
      referencePrice: operation.value.referencePrice,
    })
    const response = await fetch(`/api/series/${selectedSeriesId.value}/deltas?${params}`)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    deltas.value = await response.json()
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
    <OperationContext v-model="operation" />
    <AnalysisParameters
      v-model:selected-series-id="selectedSeriesId"
      v-model:window-days="windowDays"
      :series="series"
      :loading="loading"
      :error="error"
      :start-date="startDate"
      :end-date="endDate"
      :analyzing="analyzing"
      @analyze="analyze"
    />
    <AnalysisResults
      :deltas="deltas"
      :reference-price="operation.referencePrice"
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
