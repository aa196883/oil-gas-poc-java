<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import AdditionalInformation from './AdditionalInformation.vue'

const props = defineProps({
  deltas: { type: Array, required: true },
  prices: { type: Array, required: true },
  commodityPrices: { type: Array, required: true },
  freightPrices: { type: Array, required: true },
  withinMargins: { type: Array, default: () => [] },
  marginBounds: { type: Array, default: () => [] },
  marginError: { type: String, default: null },
  unit: { type: String, required: true },
  referencePrice: { type: Number, required: true },
})

const chartElement = ref(null)
const showDeltas = ref(true)
const showCommodityPrices = ref(false)
const showQuantiles = ref(false)
const showMean = ref(false)
const mean = computed(() =>
  props.prices.length
    ? props.prices.reduce((sum, point) => sum + Number(point.price), 0) / props.prices.length
    : null,
)
const showMargins = ref(false)
// Join the returned boundaries by date, independently of market prices or proportions.
const boundaryPoints = computed(() => {
  const [lower = [], upper = []] = props.marginBounds
  const upperByDate = new Map(upper.map((point) => [point.date, point.price]))
  return lower
    .map((point) => ({
      time: Date.parse(point.date + 'T12:00:00Z'),
      lower: point.price == null ? NaN : Number(point.price),
      upper: upperByDate.get(point.date) == null ? NaN : Number(upperByDate.get(point.date)),
    }))
    .sort((a, b) => a.time - b.time)
})
const boundarySegments = computed(() =>
  boundaryPoints.value.slice(1).flatMap((point, index) => {
    const previous = boundaryPoints.value[index]
    return [
      previous.time,
      previous.lower,
      previous.upper,
      point.time,
      point.lower,
      point.upper,
    ].every(Number.isFinite)
      ? [[previous, point]]
      : []
  }),
)
const hasMargins = computed(() => boundarySegments.value.length > 0)
const lineLabels = ref([])
const layerHelp = {
  commodity: 'Shows the commodity price before freight is added.',
  differences:
    'Compares the declared price with the boundary prices on each date: green within bounds (inclusive), red outside or when bounds are unavailable.',
  mean: 'Shows the average of the analyzed prices, including applied adjustments and freight.',
  quantiles: 'Shows historical price percentiles and their shaded ranges.',
  margins: 'Shows the tolerated price range for the commodity.',
}

const quantileLevels = [0.05, 0.25, 0.5, 0.75, 0.95]

let chart = null

function computeQuantiles(prices) {
  if (!prices.length) return {}
  const sorted = prices.map((point) => Number(point.price)).sort((a, b) => a - b)

  return Object.fromEntries(
    quantileLevels.map((quantile) => {
      const position = (sorted.length - 1) * quantile
      const lower = Math.floor(position)
      const upper = Math.ceil(position)
      const weight = position - lower
      const value = sorted[lower] + (sorted[upper] - sorted[lower]) * weight

      return [quantile, value]
    }),
  )
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
  const quantiles = computeQuantiles(props.prices)

  const values = [
    reference,
    ...points.map((point) => point.price),
    ...(showCommodityPrices.value ? commodityPoints.map((point) => point.price) : []),
    ...(showQuantiles.value ? Object.values(quantiles) : []),
    ...(showMargins.value
      ? boundarySegments.value.flat(1).flatMap((point) => [point.lower, point.upper])
      : []),
  ]
  const min = Math.min(...values)
  const max = Math.max(...values)
  const padding = Math.max((max - min) * 0.15, Math.abs(reference) * 0.01, 1)

  const axisMin = Math.floor(min - padding)
  const axisMax = Math.ceil(max + padding)
  const plotHeight = chart.getHeight() - 100
  // Lay out all reference labels together so coincident values remain readable.
  const labels = [
    { key: 'declared', title: 'Declared price', value: reference, color: '#475569' },
    ...(showMean.value && mean.value !== null
      ? [{ key: 'mean', title: 'Average Price', value: mean.value, color: '#b45309' }]
      : []),
    ...(showQuantiles.value && props.prices.length
      ? quantileLevels.map((q) => ({
          key: String(q),
          q,
          title: 'Q' + String(q * 100).padStart(2, '0'),
          value: quantiles[q],
          color: '#64748b',
        }))
      : []),
  ].sort((a, b) => b.value - a.value)
  let previousTop = 31
  lineLabels.value = labels.map((label) => {
    const idealTop = 45 + ((axisMax - label.value) / (axisMax - axisMin)) * plotHeight
    const top = Math.max(idealTop, previousTop + 28)
    previousTop = top
    return { ...label, value: label.value.toFixed(2), top, idealTop }
  })
  const overflow = (lineLabels.value.at(-1)?.top ?? 0) - (chart.getHeight() - 55)
  if (overflow > 0)
    lineLabels.value.forEach((label) => {
      label.top -= overflow
    })

  const option = {
    animationDuration: 300,

    grid: {
      top: 45,
      right: 260,
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
        if (commodity) {
          lines.push('Commodity price: ' + commodity.price.toFixed(2) + ' ' + props.unit + '<br/>')
        }
        if (freight) {
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
      min: axisMin,
      max: axisMax,
      axisLabel: { color: '#64748b' },
      splitLine: {
        lineStyle: { color: '#edf1f5' },
      },
    },

    series: [
      {
        name: 'Tolerated margins',
        type: 'custom',
        coordinateSystem: 'cartesian2d',
        silent: true,
        z: 1,
        renderItem(params, api) {
          const polygon = [
            api.coord([api.value(0), api.value(1)]),
            api.coord([api.value(3), api.value(4)]),
            api.coord([api.value(3), api.value(5)]),
            api.coord([api.value(0), api.value(2)]),
          ]
          const clipped = echarts.graphic.clipPointsByRect(polygon, params.coordSys)
          return {
            type: 'polygon',
            shape: { points: clipped },
            style: { fill: 'rgba(34, 197, 94, 0.24)' },
          }
        },
        data: showMargins.value
          ? boundarySegments.value.map(([a, b]) => [
              a.time,
              a.lower,
              a.upper,
              b.time,
              b.lower,
              b.upper,
            ])
          : [],
      },
      {
        name: 'Differences',
        type: 'custom',
        coordinateSystem: 'cartesian2d',
        z: 2,
        silent: true,

        renderItem(params, api) {
          const time = api.value(0)
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
              stroke: props.withinMargins[params.dataIndex] === true ? '#22c55e' : '#ef4444',
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
          label: { show: false },
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
        name: 'Average price',
        type: 'line',
        silent: true,
        data: [],
        z: 5,
        markLine:
          showMean.value && mean.value !== null
            ? {
                silent: true,
                symbol: 'none',
                animation: false,
                label: { show: false },
                lineStyle: { color: '#b45309', width: 2, type: 'dotted' },
                data: [{ yAxis: mean.value }],
              }
            : undefined,
      },
      {
        name: 'Quantiles',
        type: 'line',
        silent: true,
        symbol: 'none',
        data: [],
        markArea: showQuantiles.value
          ? {
              silent: true,
              animation: false,
              label: { show: false },
              data: [
                [
                  { yAxis: axisMin, itemStyle: { color: 'rgba(104, 143, 229, 0.2)' } },
                  { yAxis: quantiles[0.05] },
                ],
                [
                  { yAxis: quantiles[0.95], itemStyle: { color: 'rgba(104, 143, 229, 0.2)' } },
                  { yAxis: axisMax },
                ],
                [
                  { yAxis: quantiles[0.05], itemStyle: { color: 'rgba(122, 189, 229, 0.2)' } },
                  { yAxis: quantiles[0.25] },
                ],
                [
                  { yAxis: quantiles[0.25], itemStyle: { color: 'rgba(149, 228, 229, 0.2)' } },
                  { yAxis: quantiles[0.75] },
                ],
                [
                  { yAxis: quantiles[0.75], itemStyle: { color: 'rgba(122, 189, 229, 0.2)' } },
                  { yAxis: quantiles[0.95] },
                ],
              ],
            }
          : undefined,
        markLine: showQuantiles.value
          ? {
              silent: true,
              symbol: 'none',
              animation: false,
              label: { show: false },
              lineStyle: { color: '#94a3b8', width: 1, type: 'dashed' },
              data: quantileLevels.map((quantile) => ({
                name: 'Q' + String(quantile * 100).padStart(2, '0'),
                yAxis: quantiles[quantile],
              })),
            }
          : undefined,
      },
    ],
  }

  chart.setOption(option, true)
}

function resizeChart() {
  chart?.resize()
  updateChart()
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
    showQuantiles.value,
    showMean.value,
    showMargins.value,
    props.marginBounds,
    props.withinMargins,
  ],
  updateChart,
)
</script>

<template>
  <div class="chart-container">
    <div class="results-toolbar">
      <AdditionalInformation
        :mean="mean"
        :reference-price="referencePrice"
        :unit="unit"
        :within-margins="withinMargins"
      />
      <div class="chart-toolbar">
        <label>
          <input
            v-model="showMargins"
            type="checkbox"
            aria-label="Show tolerated margins"
            :disabled="!hasMargins"
          />
          Show tolerated margins
          <span class="help-popup" role="tooltip">{{ layerHelp.margins }}</span>
        </label>
        <label v-if="commodityPrices.length > 0">
          <input v-model="showCommodityPrices" type="checkbox" aria-label="Show commodity price" />
          Show commodity price
          <span class="help-popup" role="tooltip">{{ layerHelp.commodity }}</span>
        </label>
        <label>
          <input v-model="showDeltas" type="checkbox" aria-label="Show differences (%)" />
          Show differences (%)
          <span class="help-popup" role="tooltip">{{ layerHelp.differences }}</span>
        </label>
        <label>
          <input v-model="showQuantiles" type="checkbox" aria-label="Show quantiles" />
          Show quantiles
          <span class="help-popup" role="tooltip">{{ layerHelp.quantiles }}</span>
        </label>
        <label>
          <input v-model="showMean" type="checkbox" aria-label="Show average price" />
          Show average price
          <span class="help-popup" role="tooltip">{{ layerHelp.mean }}</span>
        </label>
      </div>
    </div>

    <p v-if="marginError || !hasMargins" class="margin-status" role="status">
      {{ marginError || 'No matching boundary dates are available for tolerated margins.' }}
    </p>
    <div class="chart-scroll">
      <div class="chart-frame">
        <div ref="chartElement" class="chart"></div>
        <svg class="label-connectors" width="20" height="460" aria-hidden="true">
          <path
            v-for="label in lineLabels"
            :key="label.key"
            :d="'M0 ' + label.idealTop + ' L16 ' + label.top"
            :stroke="label.color"
            fill="none"
            stroke-opacity="0.5"
          />
        </svg>
        <div
          v-for="label in lineLabels"
          :key="label.key"
          class="reference-label"
          :class="{ 'quantile-label': label.q != null }"
          :style="{ top: label.top + 'px', color: label.color }"
        >
          <span>{{ label.title }} : {{ label.value }}{{ label.q == null ? ' ' + unit : '' }}</span>
          <span v-if="label.q != null" class="quantile-help">
            <button
              type="button"
              :aria-label="'Explain Q' + String(label.q * 100).padStart(2, '0')"
            >
              ?
            </button>
            <span class="help-popup" role="tooltip">
              {{ label.q * 100 }} percent of historical values below {{ label.value }}
            </span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.margin-swatch {
  width: 14px;
  height: 10px;
  background: rgba(34, 197, 94, 0.24);
  border: 1px solid #22c55e;
  border-radius: 2px;
}
.margin-status {
  color: #64748b;
  font-size: 13px;
}
.chart-container {
  width: 100%;
}

.results-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}
.chart-scroll {
  overflow-x: auto;
}
.chart-frame {
  min-width: 620px;
}
@media (max-width: 900px) {
  .results-toolbar {
    flex-direction: column;
  }
  .chart-toolbar {
    justify-content: flex-start;
  }
}
.chart-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px 18px;
  max-width: 520px;
  margin-bottom: 12px;
}

.chart-toolbar label {
  position: relative;
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

<style scoped>
.chart-frame {
  position: relative;
}
.label-connectors {
  position: absolute;
  top: 0;
  right: 240px;
  pointer-events: none;
}
.reference-label {
  position: absolute;
  right: 8px;
  width: 235px;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.quantile-help {
  position: relative;
  display: inline-flex;
}
.quantile-help button {
  border: 1px solid #94a3b8;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  padding: 0;
  background: white;
  color: #64748b;
  cursor: help;
  font-size: 12px;
}
.help-popup {
  display: none;
  position: absolute;
  bottom: calc(100% + 8px);
  right: 0;
  width: 220px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #334155;
  box-shadow: 0 4px 16px #0f172a1a;
  font-size: 12px;
  line-height: 1.5;
  font-weight: normal;
  white-space: normal;
  pointer-events: none;
  z-index: 10;
}
.chart-toolbar label:hover .help-popup,
.chart-toolbar label:focus-within .help-popup,
.quantile-help:hover .help-popup,
.quantile-help:focus-within .help-popup {
  display: block;
}
</style>
