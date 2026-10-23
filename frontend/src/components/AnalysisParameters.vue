<script setup>
defineProps({
  series: { type: Array, required: true },
  fretSeries: { type: Array, required: true },
  selectedFretSeriesId: { type: Number, default: null },
  showFret: { type: Boolean, required: true },
  selectedSeriesId: { type: Number, default: null },
  windowDays: { type: Number, required: true },
  loading: { type: Boolean, required: true },
  error: { type: String, default: null },
  startDate: { type: String, required: true },
  endDate: { type: String, required: true },
  analyzing: { type: Boolean, required: true },
})
defineEmits(['update:selectedSeriesId', 'update:selectedFretSeriesId', 'update:windowDays', 'analyze'])
</script>

<template>
  <section class="card">
    <h2>02 — Analysis parameters</h2>
    <div class="grid">
      <label class="field">
        Commodity series
        <select
          :value="selectedSeriesId"
          @change="$emit('update:selectedSeriesId', Number($event.target.value))"
        >
          <option value="" disabled>Select a series</option>
          <option v-for="item in series" :key="item.id" :value="item.id">
            {{ item.benchmarkId }} — {{ item.provider }} — {{ item.unit }}
          </option>
        </select>
      </label>
      <label v-if="showFret" class="field">
        Fret series
        <select
          :value="selectedFretSeriesId"
          @change="$emit('update:selectedFretSeriesId', Number($event.target.value))"
        >
          <option value="" disabled>Select a series</option>
          <option v-for="item in fretSeries" :key="item.id" :value="item.id">
            {{ item.benchmarkId }} — {{ item.provider }} — {{ item.unit }}
          </option>
        </select>
      </label>
      <label class="field">
        Window around shipment date
        <select
          :value="windowDays"
          @change="$emit('update:windowDays', Number($event.target.value))"
        >
          <option :value="7">±7 days</option>
          <option :value="15">±15 days</option>
          <option :value="30">±30 days</option>
        </select>
      </label>
    </div>
    <p class="period">Analyzed period: {{ startDate }} to {{ endDate }}</p>
    <button
      type="button"
      :disabled="selectedSeriesId === null || analyzing || loading"
      @click="$emit('analyze')"
    >
      {{ analyzing ? 'Analyzing...' : 'Analyze prices' }}
    </button>
    <p v-if="error" class="error">Could not load quotation series: {{ error }}</p>
  </section>
</template>

<style scoped>
.period {
  margin: 24px 0;
  color: #64748b;
}
button {
  padding: 12px 20px;
  border: 0;
  border-radius: 6px;
  background: #234b75;
  color: white;
  cursor: pointer;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
