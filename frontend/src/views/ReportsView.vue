<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        start-placeholder="开始" end-placeholder="结束" @change="load" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </div>
      <div class="stat-row">
        <div class="kpi">
          <div class="kpi-label">机位利用率</div>
          <div class="kpi-num" style="color:#1f4e79">{{ data.utilization?.rate ?? '-' }}%</div>
          <div class="kpi-sub">{{ data.utilization?.sessionCount ?? 0 }} 台次 / {{ fmtMin(data.utilization?.usedMin) }} 机时</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">异常率</div>
          <div class="kpi-num" style="color:#f56c6c">{{ data.exceptions?.rate ?? '-' }}%</div>
          <div class="kpi-sub">{{ data.exceptions?.total ?? 0 }} 起 / {{ data.exceptions?.completedTreatments ?? 0 }} 次治疗</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">耗材损耗</div>
          <div class="kpi-num" style="color:#e6a23c">¥{{ data.consumableLoss?.amount ?? '-' }}</div>
          <div class="kpi-sub">{{ data.consumableLoss?.count ?? 0 }} 件损耗</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">消毒合规率</div>
          <div class="kpi-num" style="color:#67c23a">{{ data.disinfectionCompliance?.rate ?? '-' }}%</div>
          <div class="kpi-sub">{{ data.disinfectionCompliance?.compliant ?? 0 }}/{{ data.disinfectionCompliance?.machineDays ?? 0 }} 机日合格</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">每日机位利用率</div>
          <div ref="utilChart" class="chart" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="page-title">异常类型分布</div>
          <div ref="exChart" class="chart" />
        </div>
      </el-col>
    </el-row>

    <div class="page-card">
      <div class="page-title">耗材损耗明细</div>
      <el-table :data="lossRows" border size="small">
        <el-table-column prop="name" label="耗材" />
        <el-table-column prop="qty" label="损耗数量" width="120" />
      </el-table>
      <el-empty v-if="!lossRows.length" description="无损耗记录" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../api'
import { label } from '../labels'

const today = new Date().toISOString().slice(0, 10)
const weekAgo = new Date(Date.now() - 6 * 86400000).toISOString().slice(0, 10)
const range = ref([weekAgo, today])
const data = ref({})
const utilChart = ref(null)
const exChart = ref(null)
let utilC = null, exC = null

const lossRows = computed(() => Object.entries(data.value.consumableLoss?.byItem || {})
  .map(([name, qty]) => ({ name, qty })))

function fmtMin(min) { return min ? (min / 60).toFixed(1) + 'h' : '0h' }

async function load() {
  if (!range.value?.length) return
  data.value = await api.get('/reports/overview', { params: { from: range.value[0], to: range.value[1] } })
  await nextTick()
  render()
}

function render() {
  const daily = data.value.utilization?.daily || []
  if (!utilC) utilC = echarts.init(utilChart.value)
  utilC.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: daily.map(d => d.date.slice(5)) },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{ type: 'bar', data: daily.map(d => d.rate), itemStyle: { color: '#2e6da4' }, barMaxWidth: 40 }]
  })
  const byType = data.value.exceptions?.byType || {}
  if (!exC) exC = echarts.init(exChart.value)
  exC.setOption({
    tooltip: {},
    series: [{
      type: 'pie', radius: ['35%', '65%'],
      label: { formatter: '{b}: {c}' },
      data: Object.entries(byType).map(([k, v]) => ({ name: label('exType', k), value: v })),
      color: ['#f56c6c', '#e6a23c', '#b88230', '#909399', '#409eff', '#d4506b', '#7b5ea7']
    }]
  })
}

onMounted(load)
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.kpi { text-align: center; padding: 12px; background: #f8f9fb; border-radius: 8px; }
.kpi-label { color: #909399; font-size: 13px; }
.kpi-num { font-size: 30px; font-weight: 700; margin: 6px 0; }
.kpi-sub { color: #c0c4cc; font-size: 12px; }
.chart { height: 280px; }
</style>
