<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-radio-group v-model="status" @change="load">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button value="OPEN">待处理</el-radio-button>
          <el-radio-button value="PROCESSING">处理中</el-radio-button>
          <el-radio-button value="RESOLVED">已解决</el-radio-button>
        </el-radio-group>
        <div class="spacer" />
        <el-button type="danger" :icon="Plus" @click="openCreate">上报异常</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'OPEN' ? 'danger' : 'info'">
              {{ label('ExceptionType', row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者" width="90">
          <template #default="{ row }">{{ row.patientName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="machineCode" label="机位" width="70">
          <template #default="{ row }">{{ row.machineCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('ExceptionStatus', row.status)">
              {{ label('ExceptionStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="上报人" width="110" />
        <el-table-column label="上报时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="处理" width="180">
          <template #default="{ row }">
            <div v-if="row.handlerName" class="sub">
              {{ row.handlerName }}：{{ row.handleNote || '已处理' }}
            </div>
            <el-button v-if="row.status !== 'RESOLVED'" size="small" text type="primary"
                       @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上报异常 -->
    <el-dialog v-model="createDialog" title="上报异常" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="异常类型">
          <el-select v-model="createForm.type" style="width:100%">
            <el-option v-for="o in options('ExceptionType')" :key="o.value" :value="o.value" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联患者">
          <el-select v-model="createForm.patientId" filterable clearable style="width:100%"
                     @change="onPatientChange">
            <el-option v-for="p in patients" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3"
                    placeholder="请描述异常情况、影响范围与已采取措施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="danger" @click="saveCreate">上报</el-button>
      </template>
    </el-dialog>

    <!-- 处理异常 -->
    <el-dialog v-model="handleDialog" title="处理异常" width="480px">
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="处理状态">
          <el-radio-group v-model="handleForm.status">
            <el-radio value="PROCESSING">处理中</el-radio>
            <el-radio value="RESOLVED">已解决</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="handleForm.handleNote" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialog = false">取消</el-button>
        <el-button type="primary" @click="saveHandle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'
import { label, tagType, options, fmtTime } from '../utils/labels'

const list = ref([])
const patients = ref([])
const loading = ref(false)
const status = ref(undefined)
const createDialog = ref(false)
const handleDialog = ref(false)
const handleTarget = ref(null)
const createForm = reactive({ type: 'OTHER', patientId: null, patientName: '', description: '' })
const handleForm = reactive({ status: 'RESOLVED', handleNote: '' })

async function load() {
  loading.value = true
  try {
    list.value = await api.exceptions(status.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { type: 'OTHER', patientId: null, patientName: '', description: '' })
  createDialog.value = true
}

function onPatientChange(id) {
  const p = patients.value.find(x => x.id === id)
  createForm.patientName = p?.name || ''
}

async function saveCreate() {
  if (!createForm.description) {
    ElMessage.warning('请填写异常描述')
    return
  }
  await api.createException(createForm)
  ElMessage.success('已上报')
  createDialog.value = false
  await load()
}

function openHandle(row) {
  handleTarget.value = row
  Object.assign(handleForm, { status: 'RESOLVED', handleNote: '' })
  handleDialog.value = true
}

async function saveHandle() {
  await api.handleException(handleTarget.value.id, handleForm.status, handleForm.handleNote)
  ElMessage.success('已保存处理结果')
  handleDialog.value = false
  await load()
}

onMounted(async () => {
  await load()
  patients.value = (await api.patients()).filter(p => p.active)
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
.spacer { flex: 1; }
.sub { font-size: 12px; color: #909399; }
</style>
