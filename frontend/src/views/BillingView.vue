<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-radio-group v-model="status" @change="load">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button value="PENDING">待核对</el-radio-button>
          <el-radio-button value="CONFIRMED">已核对</el-radio-button>
        </el-radio-group>
        <span class="hint">治疗完成后自动生成账单：医保基准 + 自费耗材；可补收费用后核对</span>
      </div>
      <el-table :data="rows" border v-loading="loading">
        <el-table-column label="生成时间" width="150">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="患者" width="110">
          <template #default="{ row }">{{ row.treatment?.schedule?.patient?.name }}</template>
        </el-table-column>
        <el-table-column label="透析日期" width="110">
          <template #default="{ row }">{{ row.treatment?.schedule?.scheduleDate }}</template>
        </el-table-column>
        <el-table-column label="医保(¥)" width="100" align="right">
          <template #default="{ row }">{{ row.insuranceAmount }}</template>
        </el-table-column>
        <el-table-column label="自费耗材(¥)" width="110" align="right">
          <template #default="{ row }">{{ row.selfPayAmount }}</template>
        </el-table-column>
        <el-table-column label="自费明细" min-width="180">
          <template #default="{ row }"><span class="muted">{{ row.selfPayDetail || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="补收(¥)" width="90" align="right">
          <template #default="{ row }">{{ row.extraAmount }}</template>
        </el-table-column>
        <el-table-column label="合计(¥)" width="100" align="right">
          <template #default="{ row }">
            <b>{{ total(row) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="tagType('billingStatus', row.status)">{{ label('billingStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="warning" plain @click="openAdjust(row)">补收费用</el-button>
              <el-button size="small" type="success" @click="confirm(row)">核对完成</el-button>
            </template>
            <span v-else class="muted">{{ row.confirmedBy?.name }} 已核对</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="adjustVisible" title="补收费用" width="420px">
      <el-form label-width="90px">
        <el-form-item label="金额(¥)">
          <el-input-number v-model="adjustForm.extraAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="adjustForm.extraNote" placeholder="如：加做血滤 / 特殊药品" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doAdjust">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { label, tagType } from '../labels'

const rows = ref([])
const status = ref('')
const loading = ref(false)
const saving = ref(false)
const adjustVisible = ref(false)
const current = ref(null)
const adjustForm = ref({ extraAmount: 0, extraNote: '' })

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }
function total(row) {
  return (Number(row.insuranceAmount) + Number(row.selfPayAmount) + Number(row.extraAmount)).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/billing', { params: status.value ? { status: status.value } : {} })
  } finally { loading.value = false }
}

function openAdjust(row) {
  current.value = row
  adjustForm.value = { extraAmount: Number(row.extraAmount), extraNote: row.extraNote || '' }
  adjustVisible.value = true
}

async function doAdjust() {
  saving.value = true
  try {
    await api.post(`/billing/${current.value.id}/adjust`, adjustForm.value)
    ElMessage.success('已保存')
    adjustVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

async function confirm(row) {
  await ElMessageBox.confirm(`确认核对完成？合计 ¥${total(row)}`, '收费核对', { type: 'warning' })
  await api.post(`/billing/${row.id}/confirm`)
  ElMessage.success('已核对')
  load()
}

onMounted(load)
</script>

<style scoped>
.hint, .muted { color: #909399; font-size: 12px; }
</style>
