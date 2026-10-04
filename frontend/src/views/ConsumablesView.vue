<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <span class="page-title" style="margin:0">耗材库存</span>
        <el-button type="primary" :icon="Plus" @click="openTxn('PRE_DRAW')">预领</el-button>
        <el-button type="success" :icon="Download" @click="openTxn('RESTOCK')">入库</el-button>
        <el-button type="warning" :icon="Delete" @click="openTxn('LOSS')">损耗登记</el-button>
        <span class="hint">库存低于安全库存时自动生成“耗材不足”异常</span>
      </div>
      <el-table :data="items" border v-loading="loading">
        <el-table-column prop="code" label="编码" width="90" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <el-tag :type="row.stock < row.safetyStock ? 'danger' : 'success'" effect="dark">{{ row.stock }}</el-tag>
            <span class="muted"> / 安全 {{ row.safetyStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="自费" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.selfPaid" size="small" type="warning">自费</el-tag>
            <span v-else class="muted">医保</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
      <div class="page-title">最近出入库流水</div>
      <el-table :data="txns" border size="small">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="txnTag(row.type)">{{ label('txnType', row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗材" min-width="170">
          <template #default="{ row }">{{ row.consumable?.name }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="关联排班" width="130">
          <template #default="{ row }">
            <span v-if="row.schedule">{{ row.schedule.scheduleDate }} {{ row.schedule.patient?.name }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="90">
          <template #default="{ row }">{{ row.operator?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="140" />
      </el-table>
    </div>

    <el-dialog v-model="txnVisible" :title="label('txnType', txnForm.type)" width="480px">
      <el-form label-width="90px">
        <el-form-item label="耗材">
          <el-select v-model="txnForm.consumableId" filterable style="width:100%">
            <el-option v-for="c in items" :key="c.id" :value="c.id"
                       :label="`${c.name}（库存 ${c.stock}${c.unit}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="txnForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item v-if="txnForm.type === 'PRE_DRAW' || txnForm.type === 'USE'" label="关联排班">
          <el-select v-model="txnForm.scheduleId" filterable clearable style="width:100%"
                     placeholder="选择今日排班（预领将随治疗完成转使用）">
            <el-option v-for="s in todaySchedules" :key="s.id" :value="s.id"
                       :label="`${s.machineCode} ${label('shift', s.shift)} ${s.patientName}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="txnForm.note" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="txnVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doTxn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { label } from '../labels'

const items = ref([])
const txns = ref([])
const todaySchedules = ref([])
const loading = ref(false)
const saving = ref(false)
const txnVisible = ref(false)
const txnForm = ref({ type: 'PRE_DRAW', consumableId: null, scheduleId: null, quantity: 1, note: '' })

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '-' }
function txnTag(t) {
  return { PRE_DRAW: 'primary', USE: 'success', RETURN: 'info', LOSS: 'danger', RESTOCK: 'warning' }[t] || 'info'
}

async function load() {
  loading.value = true
  try {
    const [c, t, board] = await Promise.all([
      api.get('/consumables'),
      api.get('/consumables/txns'),
      api.get('/schedules/board', { params: { date: new Date().toISOString().slice(0, 10) } })
    ])
    items.value = c
    txns.value = t
    const list = []
    for (const m of board.machines || []) {
      for (const s of Object.keys(m.cells)) {
        for (const sc of m.cells[s]) if (sc.status === 'PLANNED') list.push(sc)
      }
    }
    todaySchedules.value = list
  } finally { loading.value = false }
}

function openTxn(type) {
  txnForm.value = { type, consumableId: null, scheduleId: null, quantity: 1, note: '' }
  txnVisible.value = true
}

async function doTxn() {
  if (!txnForm.value.consumableId) return ElMessage.warning('请选择耗材')
  saving.value = true
  try {
    await api.post('/consumables/txns', txnForm.value)
    ElMessage.success('已登记')
    txnVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; }
.muted { color: #909399; font-size: 12px; }
</style>
