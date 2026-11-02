<script setup>
defineProps({
  alerts: { type: Array, default: () => [] },
})
</script>

<template>
  <div v-if="alerts.length" class="alerts" role="status" aria-label="Price alerts">
    <p
      v-for="(alert, index) in alerts"
      :key="`${alert.type}-${index}`"
      class="alert-message"
      :class="`alert-message--${alert.severity}`"
    >
      {{ alert.message }}
    </p>
  </div>
</template>

<style scoped>
.alerts {
  display: grid;
  gap: 10px;
  margin-bottom: 24px;
}
.alert-message {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.alert-message--high {
  color: #991b1b;
  background: #fef2f2;
  border-color: #fca5a5;
}
.alert-message--medium {
  color: #9a3412;
  background: #fff7ed;
  border-color: #fdba74;
}
</style>
