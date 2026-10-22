<script setup>
const props = defineProps({ modelValue: { type: Object, required: true } })
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
        <input
          :value="modelValue.commodity"
          type="text"
          @input="update('commodity', $event.target.value)"
        />
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
        Declared price (USD/MT)
        <input
          :value="modelValue.referencePrice"
          type="number"
          min="0.01"
          step="any"
          @input="update('referencePrice', Number($event.target.value))"
        />
      </label>
    </div>
  </section>
</template>
