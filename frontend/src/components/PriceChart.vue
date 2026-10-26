<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  deltas: { type: Array, required: true },
  prices: { type: Array, required: true },
  commodityPrices: { type: Array, required: true },
  freightPrices: { type: Array, required: true },
  unit: { type: String, required: true },
  referencePrice: { type: Number, required: true },
})

const chartElement = ref(null)
const showDeltas = ref(true)
const showCommodityPrices = ref(false)
const showFreightPrices = ref(false)

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

  const commodityPoints = props.commodityPrices.map((point) => ({
    date: point.date,
    time: Date.parse(point.date + 'T12:00:00Z'),
    price: Number(point.price),
  }))
  const freightPoints = props.freightPrices.map((point) => ({
    date: point.date,
    time: Date.parse(point.date + 'T12:00:00Z'),
    price: Number(point.price),
  }))
  const hasFreightBreakdown = commodityPoints.length > 0 && freightPoints.length > 0

  const values = [
    reference,
    ...points.map((point) => point.price),
    ...(showCommodityPrices.value ? commodityPoints.map((point) => point.price) : []),
    ...(showFreightPrices.value ? freightPoints.map((point) => point.price) : []),
  ]
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

        const commodity = commodityPoints.find((p) => p.time === point.time)
        const freight = freightPoints.find((p) => p.time === point.time)
        const marketLabel = hasFreightBreakdown ? 'Price with freight' : 'Market price'
        const lines = [
          '<strong>' + point.date + '</strong><br/>',
          marketLabel + ': ' + point.price.toFixed(2) + ' ' + props.unit + '<br/>',
        ]
        if (hasFreightBreakdown && showCommodityPrices.value) {
          lines.push('Commodity price: ' + commodity.price.toFixed(2) + ' ' + props.unit + '<br/>')
        }
        if (hasFreightBreakdown && showFreightPrices.value) {
          lines.push('Freight price: ' + freight.price.toFixed(2) + ' ' + props.unit + '<br/>')
        }
        lines.push('Declared price: ' + reference.toFixed(2) + ' ' + props.unit + '<br/>')
        lines.push('Difference: ' + (point.delta > 0 ? '+' : '') + point.delta.toFixed(2) + ' %')
        return lines.join('')
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
      min: Math.floor(min - padding),
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
        name: hasFreightBreakdown ? 'Price with freight' : 'Market price',
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
      {
        name: 'Commodity price',
        type: 'line',
        z: 2,
        showSymbol: false,
        lineStyle: { color: '#7c3aed', width: 2 },
        itemStyle: { color: '#7c3aed' },
        data: showCommodityPrices.value
          ? commodityPoints.map((point) => [point.time, point.price])
          : [],
      },
      {
        name: 'Freight price',
        type: 'line',
        z: 2,
        showSymbol: false,
        lineStyle: { color: '#f59e0b', width: 2 },
        itemStyle: { color: '#f59e0b' },
        data: showFreightPrices.value
          ? freightPoints.map((point) => [point.time, point.price])
          : [],
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
  () => [
    props.deltas,
    props.prices,
    props.commodityPrices,
    props.freightPrices,
    props.referencePrice,
    props.unit,
    showDeltas.value,
    showCommodityPrices.value,
    showFreightPrices.value,
  ],
  updateChart,
)
</script>

<template>
  <div class="chart-container">
    <div class="chart-toolbar">
      <label v-if="commodityPrices.length > 0">
        <input v-model="showCommodityPrices" type="checkbox" />
        Show commodity price
      </label>
      <label v-if="freightPrices.length > 0">
        <input v-model="showFreightPrices" type="checkbox" />
        Show freight price
      </label>
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
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 16px;
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
