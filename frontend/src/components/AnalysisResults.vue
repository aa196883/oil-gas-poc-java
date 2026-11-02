<script setup>
import AlertDisplay from './AlertDisplay.vue'
import PriceChart from './PriceChart.vue'
defineProps({
  alerts: { type: Array, default: () => [] },
  alertsError: { type: String, default: null },
  deltas: { type: Array, required: true },
  prices: { type: Array, required: true },
  commodityPrices: { type: Array, required: true },
  freightPrices: { type: Array, required: true },
  zscore: { type: Number, default: null },
  zscoreError: { type: String, default: null },
  withinMargins: { type: Array, default: () => [] },
  marginBounds: { type: Array, default: () => [] },
  marginError: { type: String, default: null },
  unit: { type: String, required: true },
  referencePrice: { type: Number, required: true },
  analyzing: { type: Boolean, required: true },
  error: { type: String, default: null },
})
</script>

<template>
  <section class="card">
    <h2>03 — Results</h2>
    <p v-if="analyzing">Calculating...</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <p v-else-if="deltas.length === 0">
      No results yet. Run an analysis to display price differences.
    </p>
    <template v-else>
      <AlertDisplay :alerts="alerts" />
      <p v-if="alertsError" class="error" role="status">{{ alertsError }}</p>
      <PriceChart
        :deltas="deltas"
        :prices="prices"
        :commodity-prices="commodityPrices"
        :freight-prices="freightPrices"
        :margin-bounds="marginBounds"
        :within-margins="withinMargins"
        :zscore="zscore"
        :zscore-error="zscoreError"
        :margin-error="marginError"
        :reference-price="referencePrice"
        :unit="unit"
      />
    </template>
  </section>
</template>
