<template>
  <div>
    <div class="stat-row">
      <div v-for="c in cards" :key="c.label" class="page-card stat-card">
        <div class="stat-label">{{ c.label }}</div>
        <div class="stat-num" :style="{ color: c.color }">{{ c.value }}</div>
        <div class="stat-sub">{{ c.sub }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="10">
        <div class="page-card">
          <div class="page-title">机器状态分布</div>
          <div ref="machineChart" style class="chart" />
        </div>
      </el-col>
      <el-col :span="7">
        <div class="page-card">
          <div class="page-title">低库存耗材预警</div>
          <el-table v-if="dash.lowStock?.length" :data="dash.lowStock" size="small">
            <el-table-column prop="name" label="耗材" />
            <el-table-column label="库存" width="120">
              <template #default="{ row }">
                <el-tag type="danger">{{ row.stock }}</el-tag> / 安全 {{ row.safetyStock }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="库存充足" :image-size="60" />
        </div>
      </el-col>
      <el-col :span="7">
        <div class="page-card">
          <div class="page-title">待处理异常</div>
          <div v-if="openExceptions.length" class="ex-list">
            <div v-for="e in openExceptions" :key="e.id" class="ex-item">
              <el-tag :type="tagType('exStatus', e.status)" size="small">{{ label('exStatus', e.status) }}</el-tag>
              <el-tag size="small" effect="plain">{{ label('exType', e.type) }}</el-tag>
              <span class="ex-note">{{ e.note }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无待处理异常" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../api'
import { label, tagType } from '../labels'

const dash = ref({})
const openExceptions = ref([])
const machineChart = ref(null)
let chart = null

const cards = computed(() => [
  { label: '今日排班', value: dash.value.scheduled ?? '-', sub: '全部班次', color: '#1f4e79' },
  { label: '透析中', value: dash.value.onDialysis ?? '-', sub: '正在上机', color: '#409eff' },
  { label: '已完成', value: dash.value.completed ?? '-', sub: '今日下机', color: '#67c23a' },
  { label: '待处理异常', value: dash.value.openExceptions ?? '-', sub: '需跟进', color: '#f56c6c' },
  { label: '待核对账单', value: dash.value.pendingBilling ?? '-', sub: '收费窗口', color: '#e6a23c' }
])

async function load() {
  dash.value = await api.get('/dashboard/today')
  openExceptions.value = await api.get('/exceptions', { params: { status: 'OPEN' } })
  await nextTick()
  renderChart()
}

function renderChart() {
  const data = dash.value.machineStatus || {}
  const items = ['IDLE', 'IN_USE', 'DISINFECTING', 'FAULT', 'MAINTENANCE']
    .map(k => ({ name: label('machineStatus', k), value: data[k] || 0 }))
  if (!chart) chart = echarts.init(machineChart.value)
  chart.setOption({
    tooltip: {},
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      label: { formatter: '{b}: {c}' },
      data: items,
      color: ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#909399']
    }]
  })
}

onMounted(load)
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 13px; }
.stat-sub { color: #c0c4cc; font-size: 12px; margin-top: 4px; }
.chart { height: 260px; }
.ex-list { max-height: 260px; overflow-y: auto; }
.ex-item { display: flex; gap: 6px; align-items: center; padding: 6px 0; border-bottom: 1px dashed #ebeef5; font-size: 13px; }
.ex-note { color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
