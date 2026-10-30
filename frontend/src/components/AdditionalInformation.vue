<script setup>
import { computed } from 'vue'

const props = defineProps({
  withinMargins: { type: Array, default: () => [] },
  mean: { type: Number, default: null },
  referencePrice: { type: Number, required: true },
  unit: { type: String, required: true },
})
const difference = computed(() => (props.mean === null ? null : props.referencePrice - props.mean))
const percentage = computed(() =>
  difference.value === null || props.referencePrice === 0
    ? null
    : (difference.value / props.referencePrice) * 100,
)
const outOfMarginPercentage = computed(() =>
  props.withinMargins.length
    ? (props.withinMargins.filter((within) => within === false).length /
        props.withinMargins.length) *
      100
    : null,
)
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
        <span v-if="percentage !== null" class="percentage">
          ({{ percentage > 0 ? '+' : '' }}{{ format(percentage) }}%)
        </span>
      </dd>
    </div>
    <div>
      <dt>Out of margin proportions</dt>
      <dd>
        {{ format(outOfMarginPercentage) }}<span v-if="outOfMarginPercentage !== null">%</span>
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
  flex: 1 1 560px;
}
.additional-information > div {
  min-width: 0;
}
@media (max-width: 900px) {
  .additional-information {
    flex-basis: auto;
  }
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
.percentage {
  white-space: nowrap;
}
dd span {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}
</style>
