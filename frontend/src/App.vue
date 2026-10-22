<script setup>
import { ref, onMounted } from 'vue'

const series = ref([])
const loading = ref(false)
const error = ref(null)

async function loadSeries() {
  loading.value = true
  error.value = null

  try {
    const response = await fetch('/api/series')

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    series.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

onMounted(loadSeries)

const operation = ref({
  commodity: 'Crude Oil',
  origin: 'Oman',
  destination: 'France',
  shippedOnBoard: '2026-10-15',
  referencePrice: 550,
  quantity: 12000,
})

const selectedSeriesId = ref(null)
const windowDays = ref(15)
const deltas = ref([])
const analyzing = ref(false)
const analysisError = ref(null)

function shiftDate(dateString, days) {
  const date = new Date(`${dateString}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

import { computed } from 'vue'

const startDate = computed(() =>
  shiftDate(operation.value.shippedOnBoard, -windowDays.value)
)

const endDate = computed(() =>
  shiftDate(operation.value.shippedOnBoard, windowDays.value)
)

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

    const response = await fetch(
      `/api/series/${selectedSeriesId.value}/deltas?${params}`
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    deltas.value = await response.json()
  } catch (err) {
    analysisError.value = err.message
  } finally {
    analyzing.value = false
  }
}
</script>

<template>
  <main class="page">
    <header class="page-header">
      <h1>Analyse de cohérence des prix</h1>
      <p>Comparaison des prix déclarés avec les cotations du marché.</p>
    </header>

    <!-- BLOC 1 : CONTEXTE -->
    <section class="card">
      <h2>01 — Contexte de l'opération</h2>

      <div class="grid">
        <label>
          Marchandise
          <input v-model="operation.commodity" type="text" />
        </label>

        <label>
          Pays d'origine
          <input v-model="operation.origin" type="text" />
        </label>

        <label>
          Pays de destination
          <input v-model="operation.destination" type="text" />
        </label>

        <label>
          Date Shipped on Board
          <input v-model="operation.shippedOnBoard" type="date" />
        </label>

        <label>
          Prix déclaré (USD/MT)
          <input v-model.number="operation.referencePrice" type="number" min="0.01" step="any" />
        </label>

        <label>
          Quantité (MT)
          <input v-model.number="operation.quantity" type="number" min="0" />
        </label>
      </div>
    </section>

    <!-- BLOC 2 : PARAMÈTRES -->
    <section class="card">
      <h2>02 — Paramètres de l'analyse</h2>

      <div class="grid">
        <label>
          Série de cotation
          <select v-model.number="selectedSeriesId">
            <option :value="null" disabled>Choisir une série</option>

            <option
              v-for="item in series"
              :key="item.id"
              :value="item.id"
            >
              {{ item.benchmarkId }} — {{ item.provider }} — {{ item.unit }}
            </option>
          </select>
        </label>

        <label>
          Fenêtre autour de la date d'expédition
          <select v-model.number="windowDays">
            <option :value="7">±7 jours</option>
            <option :value="15">±15 jours</option>
            <option :value="30">±30 jours</option>
          </select>
        </label>
      </div>

      <p class="period">
        Période analysée : {{ startDate }} au {{ endDate }}
      </p>

      <button
        @click="analyze"
        :disabled="selectedSeriesId === null || analyzing || loading"
      >
        {{ analyzing ? 'Analyse en cours...' : 'Analyser les prix' }}
      </button>

      <p v-if="error" class="error">Erreur de chargement des séries : {{ error }}</p>
    </section>

    <!-- BLOC 3 : RÉSULTATS -->
    <section class="card">
      <h2>03 — Résultats</h2>

      <p v-if="analyzing">Calcul en cours...</p>
      <p v-else-if="analysisError" class="error">
        {{ analysisError }}
      </p>
      <p v-else-if="deltas.length === 0">
        Aucun résultat. Lance une analyse pour afficher les écarts.
      </p>

      <template v-else>
        <p>{{ deltas.length }} cotations analysées.</p>

        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Écart au prix déclaré</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="point in deltas" :key="point.date">
              <td>{{ point.date }}</td>
              <td>{{ Number(point.delta).toFixed(2) }} %</td>
            </tr>
          </tbody>
        </table>
      </template>
    </section>
  </main>
</template>

<style scoped>
.page {
  max-width: 1100px;
  margin: 40px auto;
  padding: 0 24px;
  color: #243247;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.card {
  background: white;
  border: 1px solid #e1e5ea;
  border-radius: 10px;
  padding: 28px;
  margin-bottom: 24px;
}

.card h2 {
  font-size: 18px;
  margin-bottom: 24px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

label {
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
  font: inherit;
  background: white;
  color: inherit;
}

button {
  background: #234b75;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 12px 20px;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.period {
  margin: 24px 0;
  color: #64748b;
}

.error {
  color: #b91c1c;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 10px;
  border-bottom: 1px solid #e1e5ea;
  text-align: left;
}

@media (max-width: 650px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>