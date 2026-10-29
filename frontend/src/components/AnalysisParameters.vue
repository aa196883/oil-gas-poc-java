<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  series: { type: Array, required: true },
  fretSeries: { type: Array, required: true },
  selectedFretSeriesId: { type: Number, default: null },
  showFret: { type: Boolean, required: true },
  selectedSeriesId: { type: Number, default: null },
  windowDays: { type: Number, required: true },
  premium: { type: Number, default: 0 },
  unit: { type: String, required: true },
  loading: { type: Boolean, required: true },
  error: { type: String, default: null },
  startDate: { type: String, required: true },
  endDate: { type: String, required: true },
  analyzing: { type: Boolean, required: true },
})
const emit = defineEmits([
  'update:selectedSeriesId',
  'update:selectedFretSeriesId',
  'update:windowDays',
  'update:premium',
  'analyze',
])
const custom = ref(false)
const customStart = ref('')
const customEnd = ref('')
const range = computed(() => ({
  start: custom.value ? customStart.value : props.startDate,
  end: custom.value ? customEnd.value : props.endDate,
}))
const dateError = computed(() => {
  if (!custom.value) return ''
  if (!customStart.value || !customEnd.value) return 'Choose both a start and an end date.'
  if (customStart.value > customEnd.value) return 'End date must be on or after start date.'
  return ''
})

function selectWindow(value) {
  custom.value = value === 'custom'
  if (custom.value) {
    customStart.value ||= props.startDate
    customEnd.value ||= props.endDate
  } else {
    emit('update:windowDays', Number(value))
  }
}

function analyze() {
  if (!dateError.value) emit('analyze', range.value)
}
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
            {{ item.family }} — {{ item.zone }} — {{ item.provider }} — {{ item.unit }}
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
            {{ item.family }} — {{ item.zone }} — {{ item.provider }} — {{ item.unit }}
          </option>
        </select>
      </label>
      <label class="field">
        Window around shipment date
        <select :value="custom ? 'custom' : windowDays" @change="selectWindow($event.target.value)">
          <option :value="7">±7 days</option>
          <option :value="15">±15 days</option>
          <option :value="30">±30 days</option>
          <option value="custom">Custom</option>
        </select>
      </label>
      <template v-if="custom">
        <label class="field">
          Start date
          <input
            v-model="customStart"
            type="date"
            required
            :max="customEnd || undefined"
            :aria-invalid="!!dateError"
            aria-describedby="custom-date-error"
          />
        </label>
        <label class="field">
          End date
          <input
            v-model="customEnd"
            type="date"
            required
            :min="customStart || undefined"
            :aria-invalid="!!dateError"
            aria-describedby="custom-date-error"
          />
        </label>
      </template>
    </div>
    <fieldset class="advanced-group">
      <legend>Advanced</legend>
      <div class="grid">
        <label class="field">
          Premium / discount ({{ unit }})
          <input
            type="number"
            step="any"
            aria-label="Premium / discount"
            :value="premium"
            @input="
              $emit('update:premium', $event.target.value === '' ? 0 : Number($event.target.value))
            "
          />
        </label>
      </div>
    </fieldset>
    <p v-if="dateError" id="custom-date-error" class="error" role="alert">{{ dateError }}</p>
    <p class="period">Analyzed period: {{ range.start || '—' }} to {{ range.end || '—' }}</p>
    <button
      type="button"
      :disabled="
        selectedSeriesId === null ||
        (showFret && selectedFretSeriesId === null) ||
        analyzing ||
        loading ||
        !!dateError
      "
      @click="analyze"
    >
      {{ analyzing ? 'Analyzing...' : 'Analyze prices' }}
    </button>
    <p v-if="error" class="error">Could not load quotation series: {{ error }}</p>
  </section>
</template>

<style scoped>
.advanced-group {
  min-width: 0;
  margin: 20px 0 0;
  padding: 16px;
  border: 1px solid #e1e5ea;
  border-radius: 6px;
}
.advanced-group legend {
  padding: 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

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
