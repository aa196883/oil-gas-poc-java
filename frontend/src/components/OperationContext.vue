<script setup>
import { computed, ref } from 'vue'
const props = defineProps({
  modelValue: { type: Object, required: true },
  units: { type: Array, required: true },
  families: { type: Array, required: true },
  countries: { type: Array, required: true },
})
const availableUnits = computed(() => props.units.filter((unit) => unit.split('/').length === 2))
const priceUnits = computed(() => [
  ...new Set(availableUnits.value.map((unit) => unit.split('/')[0])),
])
const quantityUnits = computed(() => [
  ...new Set(['MT', ...availableUnits.value.map((unit) => unit.split('/')[1])]),
])
const activeCountryField = ref(null)

function matchingCountries(value) {
  const query = value.trim().toLocaleLowerCase()
  return props.countries.filter((country) => country.toLocaleLowerCase().includes(query))
}

function selectCountry(field, country) {
  update(field, country)
  activeCountryField.value = null
}

function closeCountryList(field) {
  setTimeout(() => {
    if (activeCountryField.value === field) activeCountryField.value = null
  })
}

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
      <div class="field country-field">
        <label for="country-origin">Country of origin</label>
        <input
          id="country-origin"
          :value="modelValue.origin"
          type="text"
          role="combobox"
          autocomplete="off"
          aria-controls="origin-countries"
          :aria-expanded="activeCountryField === 'origin'"
          @focus="activeCountryField = 'origin'"
          @blur="closeCountryList('origin')"
          @input="update('origin', $event.target.value)"
        />
        <ul v-if="activeCountryField === 'origin'" id="origin-countries" class="country-options">
          <li v-for="country in matchingCountries(modelValue.origin)" :key="country">
            <button type="button" @click="selectCountry('origin', country)">
              {{ country }}
            </button>
          </li>
        </ul>
      </div>
      <div class="field country-field">
        <label for="country-destination">Country of destination</label>
        <input
          id="country-destination"
          :value="modelValue.destination"
          type="text"
          role="combobox"
          autocomplete="off"
          aria-controls="destination-countries"
          :aria-expanded="activeCountryField === 'destination'"
          @focus="activeCountryField = 'destination'"
          @blur="closeCountryList('destination')"
          @input="update('destination', $event.target.value)"
        />
        <ul
          v-if="activeCountryField === 'destination'"
          id="destination-countries"
          class="country-options"
        >
          <li v-for="country in matchingCountries(modelValue.destination)" :key="country">
            <button type="button" @click="selectCountry('destination', country)">
              {{ country }}
            </button>
          </li>
        </ul>
      </div>
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
.country-field {
  position: relative;
}
.country-options {
  position: absolute;
  top: 100%;
  right: 0;
  left: 0;
  z-index: 20;
  max-height: 220px;
  margin: 4px 0 0;
  padding: 4px;
  overflow-y: auto;
  list-style: none;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: white;
  box-shadow: 0 6px 18px #0f172a1f;
}
.country-options button {
  width: 100%;
  padding: 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.country-options button:hover,
.country-options button:focus {
  background: #eff6ff;
}
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
