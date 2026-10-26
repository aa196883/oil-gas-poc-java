<script setup>
import { computed } from 'vue'
const props = defineProps({
  modelValue: { type: Object, required: true },
  units: { type: Array, required: true },
  families: { type: Array, required: true },
})
const availableUnits = computed(() => [...new Set([...props.units, 'USD/MT'])])
const emit = defineEmits(['update:modelValue'])

function update(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}
</script>

<template>
  <section class="card">
    <h2>01 — Operation context</h2>
    <div class="grid">
      <label class="field">
        Commodity
        <select
          :value="modelValue.commodity"
          :disabled="families.length === 0"
          @change="update('commodity', $event.target.value)"
        >
          <option value="" disabled>Select a commodity</option>
          <option v-for="family in families" :key="family" :value="family">{{ family }}</option>
        </select>
      </label>
      <label class="field">
        Incoterms
        <select :value="modelValue.incoterms" @change="update('incoterms', $event.target.value)">
          <option
            v-for="incoterm in ['FAS', 'FOB', 'CFR', 'CIF']"
            :key="incoterm"
            :value="incoterm"
          >
            {{ incoterm }}
          </option>
        </select>
      </label>
      <label class="field">
        Country of origin
        <input
          :value="modelValue.origin"
          type="text"
          @input="update('origin', $event.target.value)"
        />
      </label>
      <label class="field">
        Country of destination
        <input
          :value="modelValue.destination"
          type="text"
          @input="update('destination', $event.target.value)"
        />
      </label>
      <label class="field">
        Shipped on board date
        <input
          :value="modelValue.shippedOnBoard"
          type="date"
          @input="update('shippedOnBoard', $event.target.value)"
        />
      </label>
      <label class="field">
        Declared price
        <input
          :value="modelValue.referencePrice"
          type="number"
          min="0.01"
          step="any"
          @input="update('referencePrice', Number($event.target.value))"
        />
      </label>
      <label class="field">
        Declared price unit
        <select
          :value="modelValue.unit"
          :disabled="availableUnits.length === 0"
          @change="update('unit', $event.target.value)"
        >
          <option value="" disabled>Select a unit</option>
          <option v-for="unit in availableUnits" :key="unit" :value="unit">{{ unit }}</option>
        </select>
      </label>
    </div>
  </section>
</template>
