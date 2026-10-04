<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
                        :clearable="false" @change="load" />
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
      </div>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card shadow="hover" class="stat">
            <div class="num">{{ data.overallUtilization ?? '-' }}%</div>
            <div class="lbl">机位利用率</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat">
            <div class="num" style="color:#f56c6c">{{ data.exceptionRate ?? '-' }}%</div>
            <div class="lbl">异常率（{{ data.exceptionTotal || 0 }}/{{ data.sessionTotal || 0 }}）</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat">
            <div class="num" style="color:#e6a23c">{{ data.wasteRate ?? '-' }}%</div>
            <div class="lbl">耗材损耗率</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat">
            <div class="num" style="color:#67c23a">{{ data.disinfectionCompliance ?? '-' }}%</div>
            <div class="lbl">消毒合规率</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header><span>各机位利用率（%）</span></template>
          <div ref="utilChart" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>异常类型分布</span></template>
          <div ref="excChart" class="chart" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header><span>耗材损耗 / 使用量</span></template>
          <div ref="wasteChart" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>消毒记录与合规</span></template>
          <div ref="disChart" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../api'
import { label } from '../utils/labels'
import dayjs from 'dayjs'

const range = ref([dayjs().subtract(6, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const data = ref({})
const utilChart = ref(null)
const excChart = ref(null)
const wasteChart = ref(null)
const disChart = ref(null)
let charts = []

function makeChart(el, option) {
  const c = echarts.init(el)
  c.setOption(option)
  charts.push(c)
  return c
}

async function load() {
  data.value = await api.overview(range.value[0], range.value[1])
  await nextTick()
  charts.forEach(c => c.dispose())
  charts = []
  renderUtil()
  renderExceptions()
  renderWaste()
  renderDisinfection()
}

function renderUtil() {
  const rows = data.value.utilization || []
  makeChart(utilChart.value, {
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map(r => r.machineCode) },
    yAxis: { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'bar', data: rows.map(r => r.utilization), barWidth: 18,
      itemStyle: {
        color: (p) => rows[p.dataIndex].zone === 'NORMAL' ? '#67c23a'
          : rows[p.dataIndex].zone === 'HBV' ? '#e6a23c'
          : rows[p.dataIndex].zone === 'HCV' ? '#b37feb' : '#f56c6c'
      }
    }]
  })
}

function renderExceptions() {
  const byType = data.value.exceptionsByType || {}
  const items = Object.entries(byType).map(([k, v]) => ({ name: label('ExceptionType', k), value: v }))
  makeChart(excChart.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'],
      data: items.length ? items : [{ name: '无异常', value: 1, itemStyle: { color: '#e4e7ed' } }],
      label: { formatter: '{b}: {c}' }
    }]
  })
}

function renderWaste() {
  const usage = data.value.consumableUsage || {}
  const waste = data.value.consumableWaste || {}
  const names = [...new Set([...Object.keys(usage), ...Object.keys(waste)])]
  makeChart(wasteChart.value, {
    grid: { left: 110, right: 30, top: 40, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names },
    series: [
      { name: '使用/预领', type: 'bar', stack: 'a', data: names.map(n => usage[n] || 0), itemStyle: { color: '#409eff' } },
      { name: '损耗', type: 'bar', stack: 'a', data: names.map(n => waste[n] || 0), itemStyle: { color: '#f56c6c' } }
    ]
  })
}

function renderDisinfection() {
  const byType = data.value.disinfectionByType || {}
  const names = Object.keys(byType)
  makeChart(disChart.value, {
    grid: { left: 50, right: 30, top: 40, bottom: 40 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: names.map(n => label('DisinfectionType', n)), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value', name: '次数' },
    series: [{ type: 'bar', data: names.map(n => byType[n]), barWidth: 30, itemStyle: { color: '#67c23a' } }]
  })
}

onMounted(load)
window.addEventListener('resize', () => charts.forEach(c => c.resize()))
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.stat { text-align: center; }
.num { font-size: 30px; font-weight: 700; }
.lbl { color: #909399; margin-top: 4px; font-size: 13px; }
.chart { height: 300px; }
</style>
