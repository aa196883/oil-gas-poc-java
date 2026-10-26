<script setup>
import { computed } from 'vue'
const props = defineProps({
  modelValue: { type: Object, required: true },
  units: { type: Array, required: true },
  families: { type: Array, required: true },
})
const availableUnits = computed(() => props.units.filter((unit) => unit.split('/').length === 2))
const priceUnits = computed(() => [
  ...new Set(availableUnits.value.map((unit) => unit.split('/')[0])),
])
const quantityUnits = computed(() => [
  ...new Set(['MT', ...availableUnits.value.map((unit) => unit.split('/')[1])]),
])

function updateUnit(part, value) {
  const parts = props.modelValue.unit.split('/')
  parts[part] = value
  update('unit', parts.join('/'))
}
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
        Shipment Price Logistic
        <select :value="modelValue.logic" @change="update('logic', $event.target.value)">
          <option value="POL">Port of Loading (POL)</option>
          <option value="POD">Port of Discharge (POD)</option>
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
      <div class="amounts-row">
        <div class="amount-field">
          <label for="declared-price">Declared price</label>
          <div class="amount-input">
            <input
              id="declared-price"
              :value="modelValue.referencePrice"
              type="text"
              inputmode="decimal"
              @input="update('referencePrice', $event.target.value)"
            />
            <select
              aria-label="Price unit"
              :value="modelValue.unit.split('/')[0]"
              :disabled="priceUnits.length === 0"
              @change="updateUnit(0, $event.target.value)"
            >
              <option v-for="unit in priceUnits" :key="unit" :value="unit">{{ unit }}</option>
            </select>
          </div>
        </div>
        <div class="amount-field">
          <label for="quantity">Quantity</label>
          <div class="amount-input">
            <input
              id="quantity"
              :value="modelValue.quantity"
              type="text"
              inputmode="decimal"
              @input="update('quantity', $event.target.value)"
            />
            <select
              aria-label="Quantity unit"
              :value="modelValue.unit.split('/')[1]"
              :disabled="quantityUnits.length === 0"
              @change="updateUnit(1, $event.target.value)"
            >
              <option v-for="unit in quantityUnits" :key="unit" :value="unit">{{ unit }}</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.amounts-row {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}
.amount-field {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.amount-field > label {
  font-size: 14px;
  font-weight: 600;
}
.amount-input {
  display: flex;
  gap: 8px;
}
.amount-input input {
  width: 100%;
  min-width: 0;
}
.amount-input select {
  width: auto;
  max-width: 50%;
  flex-shrink: 0;
}
</style>
