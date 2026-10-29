<script setup>
import { computed } from 'vue'

const props = defineProps({
  mean: { type: Number, default: null },
  referencePrice: { type: Number, required: true },
  unit: { type: String, required: true },
})
const difference = computed(() => (props.mean === null ? null : props.mean - props.referencePrice))
const format = (value) => (value === null ? '—' : value.toFixed(2))
</script>

<template>
  <dl class="additional-information">
    <div>
      <dt>Average price</dt>
      <dd>
        {{ format(mean) }} <span>{{ unit }}</span>
      </dd>
    </div>
    <div>
      <dt>Difference from average price</dt>
      <dd>
        {{ difference > 0 ? '+' : '' }}{{ format(difference) }} <span>{{ unit }}</span>
      </dd>
    </div>
  </dl>
</template>

<style scoped>
.additional-information {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 28px;
  margin: 0;
  font-variant-numeric: tabular-nums;
}
dt {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}
dd {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 600;
}
dd span {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}
</style>
