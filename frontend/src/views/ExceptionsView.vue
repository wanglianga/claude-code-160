<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="status" @change="load">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button value="OPEN">待处理</el-radio-button>
          <el-radio-button value="ACKNOWLEDGED">已知晓</el-radio-button>
          <el-radio-button value="RESOLVED">已解决</el-radio-button>
        </el-radio-group>
        <el-button type="danger" :icon="Plus" @click="createVisible = true">上报异常</el-button>
      </div>
      <el-table :data="rows" border v-loading="loading">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="danger" effect="plain">{{ label('exType', row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联对象" width="160">
          <template #default="{ row }">
            <span v-if="row.patient">{{ row.patient.name }}</span>
            <span v-if="row.machine" class="muted"> {{ row.machine.code }}</span>
            <span v-if="!row.patient && !row.machine" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType('exStatus', row.status)">{{ label('exStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理" min-width="180">
          <template #default="{ row }">
            <div v-if="row.handledBy" class="muted">{{ row.handledBy.name }}：{{ row.handleNote }}</div>
            <div v-if="row.status !== 'RESOLVED'" style="margin-top:4px">
              <el-button v-if="row.status === 'OPEN'" size="small" type="warning" plain
                         @click="handle(row, 'ACKNOWLEDGED')">知晓</el-button>
              <el-button size="small" type="success" plain @click="handle(row, 'RESOLVED')">解决</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="上报异常" width="480px">
      <el-form label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option v-for="(v, k) in exTypes" :key="k" :value="k" :label="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="患者">
          <el-select v-model="form.patientId" filterable clearable style="width:100%">
            <el-option v-for="p in patients" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="机器">
          <el-select v-model="form.machineId" filterable clearable style="width:100%">
            <el-option v-for="m in machines" :key="m.id" :value="m.id" :label="m.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="doCreate">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { label, tagType, LABELS } from '../labels'

const rows = ref([])
const status = ref('')
const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const form = ref({ type: 'LATE', patientId: null, machineId: null, note: '' })
const patients = ref([])
const machines = ref([])
const exTypes = LABELS.exType

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/exceptions', { params: status.value ? { status: status.value } : {} })
  } finally { loading.value = false }
}

async function handle(row, target) {
  const { value } = await ElMessageBox.prompt('处理说明', target === 'RESOLVED' ? '解决异常' : '知晓确认',
    { inputValue: '' })
  await api.post(`/exceptions/${row.id}/handle`, { status: target, handleNote: value || '' })
  ElMessage.success('已更新')
  load()
}

async function doCreate() {
  saving.value = true
  try {
    await api.post('/exceptions', form.value)
    ElMessage.success('已上报')
    createVisible.value = false
    form.value = { type: 'LATE', patientId: null, machineId: null, note: '' }
    load()
  } catch (e) {} finally { saving.value = false }
}

onMounted(async () => {
  load()
  const [p, m] = await Promise.all([api.get('/patients'), api.get('/engineering/machines')])
  patients.value = p
  machines.value = m
})
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
