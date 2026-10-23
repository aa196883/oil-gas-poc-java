<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  deltas: { type: Array, required: true },
  prices: { type: Array, required: true },
  unit: { type: String, required: true },
  referencePrice: { type: Number, required: true },
})

const chartElement = ref(null)
const showDeltas = ref(true)

let chart = null

function deltaColor(delta) {
  const value = Math.abs(delta)

  if (value <= 5) return '#22c55e'
  if (value >= 15) return '#ef4444'

  const t = (value - 5) / 10

  return echarts.color.lerp(t, ['#22c55e', '#f59e0b', '#ef4444'])
}

function updateChart() {
  if (!chart) return

  const reference = props.referencePrice

  const deltasByDate = new Map(props.deltas.map((point) => [point.date, Number(point.delta)]))
  const points = props.prices.map((point) => {
    const delta = deltasByDate.get(point.date)

    return {
      date: point.date,
      time: Date.parse(`${point.date}T12:00:00Z`),
      delta,
      price: Number(point.price),
    }
  })

  const values = [reference, ...points.map((p) => p.price)]
  const min = Math.min(...values)
  const max = Math.max(...values)
  const padding = Math.max((max - min) * 0.15, Math.abs(reference) * 0.01, 1)

  const option = {
    animationDuration: 300,

    grid: {
      top: 45,
      right: 35,
      bottom: 55,
      left: 75,
    },

    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const point = points.find((p) => p.time === params[0]?.value?.[0])
        if (!point) return ''

        return `
          <strong>${point.date}</strong><br/>
          Market price: ${point.price.toFixed(2)} ${props.unit}<br/>
          Declared price: ${reference.toFixed(2)} ${props.unit}<br/>
          Difference: ${point.delta > 0 ? '+' : ''}${point.delta.toFixed(2)} %
        `
      },
    },

    xAxis: {
      type: 'time',
      axisLine: { lineStyle: { color: '#94a3b8' } },
      axisLabel: { color: '#64748b' },
      splitLine: { show: false },
    },

    yAxis: {
      type: 'value',
      name: props.unit,
      nameTextStyle: { color: '#64748b' },
      min: min - padding,
      max: Math.ceil(max + padding),
      axisLabel: { color: '#64748b' },
      splitLine: {
        lineStyle: { color: '#edf1f5' },
      },
    },

    series: [
      {
        name: 'Differences',
        type: 'custom',
        coordinateSystem: 'cartesian2d',
        z: 2,
        silent: true,

        renderItem(params, api) {
          const time = api.value(0)
          const delta = api.value(1)
          const price = api.value(2)

          const start = api.coord([time, reference])
          const end = api.coord([time, price])

          return {
            type: 'line',
            shape: {
              x1: start[0],
              y1: start[1],
              x2: end[0],
              y2: end[1],
            },
            style: {
              stroke: deltaColor(delta),
              lineWidth: 4,
              opacity: 0.8,
            },
          }
        },

        data: showDeltas.value ? points.map((p) => [p.time, p.delta, p.price]) : [],
      },

      {
        name: 'Market price',
        type: 'line',
        z: 3,
        smooth: false,
        showSymbol: false,
        symbolSize: 6,
        lineStyle: {
          color: '#2563eb',
          width: 2.5,
        },
        itemStyle: {
          color: '#2563eb',
        },

        data: points.map((p) => [p.time, p.price]),

        markLine: {
          silent: true,
          symbol: 'none',
          animation: false,
          label: {
            show: true,
            position: 'insideEndTop',
            formatter: `Declared price: ${reference.toFixed(2)} ${props.unit}`,
            color: '#475569',
          },
          lineStyle: {
            color: '#475569',
            width: 2,
            type: 'dashed',
          },
          data: [{ yAxis: reference }],
        },
      },
    ],
  }

  chart.setOption(option, true)
}

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  chart = echarts.init(chartElement.value)
  updateChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

watch(
  () => [props.deltas, props.prices, props.referencePrice, props.unit, showDeltas.value],
  updateChart,
)
</script>

<template>
  <div class="chart-container">
    <div class="chart-toolbar">
      <label>
        <input v-model="showDeltas" type="checkbox" />
        Show differences (%)
      </label>
    </div>

    <div ref="chartElement" class="chart"></div>
  </div>
</template>

<style scoped>
.chart-container {
  width: 100%;
}

.chart-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.chart-toolbar label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
}

.chart {
  width: 100%;
  height: 460px;
}
</style>
