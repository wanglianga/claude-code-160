<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="13">
        <el-card>
          <template #header>
            <div class="head">
              <span>耗材库存</span>
              <el-button size="small" :icon="Plus" @click="openEdit(null)">新增耗材</el-button>
            </div>
          </template>
          <el-table :data="list" v-loading="loading" border stripe size="small"
                    :row-class-name="rowClass">
            <el-table-column prop="code" label="编码" width="110" />
            <el-table-column prop="name" label="名称" min-width="130" />
            <el-table-column prop="spec" label="规格" width="110" />
            <el-table-column label="库存" width="110" align="center">
              <template #default="{ row }">
                <b :style="row.stock <= row.safeStock ? 'color:#f56c6c' : ''">{{ row.stock }}</b>
                <span class="sub"> / {{ row.safeStock }} {{ row.unit }}</span>
              </template>
            </el-table-column>
            <el-table-column label="自费" width="60" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.selfPay" size="small" type="warning">自费</el-tag>
                <span v-else class="sub">医保</span>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="80">
              <template #default="{ row }">{{ row.selfPay ? '¥' + row.price : '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openTxn(row)">出入库</el-button>
                <el-button size="small" text type="warning" @click="openEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="11">
        <el-card>
          <template #header><span>最近出入库流水</span></template>
          <el-table :data="txns" size="small" max-height="560">
            <el-table-column prop="consumableName" label="耗材" width="120" show-overflow-tooltip />
            <el-table-column label="类型" width="70">
              <template #default="{ row }">
                <el-tag size="small" :type="row.qty < 0 ? 'warning' : 'success'">
                  {{ label('TxnType', row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" width="60" align="right" />
            <el-table-column prop="patientName" label="患者" width="80">
              <template #default="{ row }">{{ row.patientName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="80" />
            <el-table-column label="时间" width="140">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="note" label="备注" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 出入库 -->
    <el-dialog v-model="txnDialog" :title="`出入库 - ${txnTarget?.name || ''}`" width="440px">
      <el-form :model="txnForm" label-width="90px">
        <el-form-item label="类型">
          <el-radio-group v-model="txnForm.type">
            <el-radio-button value="USE">使用</el-radio-button>
            <el-radio-button value="WASTE">损耗</el-radio-button>
            <el-radio-button value="RETURN">归还</el-radio-button>
            <el-radio-button value="RESTOCK">入库</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="txnForm.qty" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联患者" v-if="txnForm.type === 'USE'">
          <el-input v-model="txnForm.patientName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="txnForm.note" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="txnDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTxn">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑耗材 -->
    <el-dialog v-model="editDialog" :title="editForm.id ? '编辑耗材' : '新增耗材'" width="440px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="编码"><el-input v-model="editForm.code" :disabled="!!editForm.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="editForm.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="editForm.unit" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="editForm.safeStock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="自费耗材"><el-switch v-model="editForm.selfPay" /></el-form-item>
        <el-form-item label="单价(元)" v-if="editForm.selfPay">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="saveConsumable">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'
import { label, fmtTime } from '../utils/labels'

const list = ref([])
const txns = ref([])
const loading = ref(false)
const txnDialog = ref(false)
const editDialog = ref(false)
const txnTarget = ref(null)
const txnForm = reactive({ type: 'USE', qty: 1, patientName: '', note: '' })
const editForm = reactive({})

const rowClass = ({ row }) => (row.stock <= row.safeStock ? 'warn-row' : '')

async function load() {
  loading.value = true
  try {
    const [cs, ts] = await Promise.all([api.consumables(), api.consumableTxns()])
    list.value = cs
    txns.value = ts
  } finally {
    loading.value = false
  }
}

function openTxn(row) {
  txnTarget.value = row
  Object.assign(txnForm, { type: 'USE', qty: 1, patientName: '', note: '' })
  txnDialog.value = true
}

async function saveTxn() {
  await api.consumableTxn(txnTarget.value.id, { ...txnForm })
  ElMessage.success('已记录')
  txnDialog.value = false
  await load()
}

function openEdit(row) {
  if (row) {
    Object.assign(editForm, JSON.parse(JSON.stringify(row)))
  } else {
    Object.keys(editForm).forEach(k => delete editForm[k])
    Object.assign(editForm, { code: '', name: '', spec: '', unit: '支', safeStock: 10, selfPay: false, price: 0 })
  }
  editDialog.value = true
}

async function saveConsumable() {
  if (editForm.id) {
    await api.updateConsumable(editForm.id, editForm)
  } else {
    await api.createConsumable(editForm)
  }
  ElMessage.success('已保存')
  editDialog.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.sub { font-size: 12px; color: #909399; }
:deep(.warn-row) { background: #fef0f0; }
</style>
