<template>
  <div>
    <div class="page-card">
      <div class="page-title">透析机状态</div>
      <el-table :data="machines" border v-loading="loading">
        <el-table-column prop="code" label="机位" width="90" />
        <el-table-column label="分区" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('zone', row.zone)">{{ label('zone', row.zone) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="型号" min-width="140" />
        <el-table-column prop="location" label="位置" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagType('machineStatus', row.status)">{{ label('machineStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button v-if="canEdit" size="small" type="warning" plain
                       :disabled="row.status === 'FAULT'" @click="setStatus(row, 'FAULT')">报故障</el-button>
            <el-button v-if="canEdit" size="small" type="success" plain
                       :disabled="row.status === 'IDLE'" @click="setStatus(row, 'IDLE')">恢复空闲</el-button>
            <el-button v-if="canEdit" size="small" type="info" plain
                       :disabled="row.status === 'MAINTENANCE'" @click="setStatus(row, 'MAINTENANCE')">维护</el-button>
            <el-button v-if="canEdit" size="small" type="primary" @click="openRecord(row)">登记消毒/滤芯/水处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
      <div class="page-title">最近工程记录（消毒 / 滤芯 / 水处理）</div>
      <el-table :data="records" border size="small">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmt(row.performedAt) }}</template>
        </el-table-column>
        <el-table-column label="机位" width="80">
          <template #default="{ row }">{{ row.machine?.code }}</template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'DISINFECTION' ? 'primary' : row.type === 'FILTER_CHANGE' ? 'warning' : 'success'" effect="plain">
              {{ label('disType', row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'PASS' ? 'success' : 'danger'">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="说明" min-width="220" />
        <el-table-column label="操作人" width="100">
          <template #default="{ row }">{{ row.performedBy?.name }}</template>
        </el-table-column>
        <el-table-column label="下次到期" width="150">
          <template #default="{ row }">{{ row.nextDueAt ? fmt(row.nextDueAt) : '-' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="recordVisible" :title="`登记工程记录 — ${current?.code || ''}`" width="480px">
      <el-form label-width="90px">
        <el-form-item label="类型">
          <el-radio-group v-model="recordForm.type">
            <el-radio-button value="DISINFECTION">机器消毒</el-radio-button>
            <el-radio-button value="FILTER_CHANGE">滤芯更换</el-radio-button>
            <el-radio-button value="WATER_TREATMENT">水处理</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结果">
          <el-radio-group v-model="recordForm.result">
            <el-radio-button value="PASS">合格</el-radio-button>
            <el-radio-button value="FAIL">不合格</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="recordForm.note" placeholder="如：热消毒+柠檬酸 / 反渗水电导率" />
        </el-form-item>
        <el-form-item v-if="recordForm.type !== 'DISINFECTION'" label="下次到期">
          <el-date-picker v-model="recordForm.nextDueAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { label, tagType } from '../labels'
import { useAuthStore } from '../store'

const auth = useAuthStore()
const canEdit = computed(() => ['ENGINEER', 'ADMIN', 'HEAD_NURSE'].includes(auth.role))
const machines = ref([])
const records = ref([])
const loading = ref(false)
const saving = ref(false)
const recordVisible = ref(false)
const current = ref(null)
const recordForm = ref({ type: 'DISINFECTION', result: 'PASS', note: '', nextDueAt: null })

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    const [m, r] = await Promise.all([api.get('/engineering/machines'), api.get('/engineering/records')])
    machines.value = m
    records.value = r
  } finally { loading.value = false }
}

async function setStatus(row, status) {
  let note = ''
  if (status === 'FAULT') {
    const { value } = await ElMessageBox.prompt('故障现象（将自动生成机器故障异常）', '报故障', { inputValue: '' })
    note = value || ''
  }
  await api.put(`/engineering/machines/${row.id}/status`, { status, note })
  ElMessage.success('状态已更新')
  load()
}

function openRecord(row) {
  current.value = row
  recordForm.value = { type: 'DISINFECTION', result: 'PASS', note: '', nextDueAt: null }
  recordVisible.value = true
}

async function doRecord() {
  saving.value = true
  try {
    await api.post('/engineering/records', { machineId: current.value.id, ...recordForm.value })
    ElMessage.success('已登记；消毒合格后机器自动恢复空闲')
    recordVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

onMounted(load)
</script>
