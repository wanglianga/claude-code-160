<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-radio-group v-model="status" @change="load">
          <el-radio-button value="PENDING">待核对</el-radio-button>
          <el-radio-button value="CONFIRMED">已核对</el-radio-button>
          <el-radio-button :value="undefined">全部</el-radio-button>
        </el-radio-group>
        <div class="spacer" />
        <el-statistic v-if="status === 'PENDING'" title="待核对金额合计（元）" :value="pendingTotal" :precision="2" />
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="sessionId" label="治疗单号" width="90" align="center" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column label="医保结算(元)" width="110" align="right">
          <template #default="{ row }">{{ row.insuranceAmount }}</template>
        </el-table-column>
        <el-table-column label="自费耗材(元)" width="110" align="right">
          <template #default="{ row }">{{ row.selfPayAmount }}</template>
        </el-table-column>
        <el-table-column label="补收费用(元)" width="110" align="right">
          <template #default="{ row }">{{ row.extraCharge }}</template>
        </el-table-column>
        <el-table-column label="合计(元)" width="110" align="right">
          <template #default="{ row }">
            <b>{{ total(row) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="extraReason" label="补收原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.extraReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('BillingStatus', row.status)">
              {{ label('BillingStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核对人/时间" width="180">
          <template #default="{ row }">
            <span v-if="row.confirmedBy">{{ row.confirmedBy }} {{ fmtTime(row.confirmedAt) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" text type="warning" @click="openEdit(row)">补收/调整</el-button>
              <el-button size="small" text type="primary" @click="confirm(row)">核对确认</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="调整账单" width="440px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="医保结算(元)">
          <el-input-number v-model="form.insuranceAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="自费耗材(元)">
          <el-input-number v-model="form.selfPayAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="补收费用(元)">
          <el-input-number v-model="form.extraCharge" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="补收原因">
          <el-input v-model="form.extraReason" type="textarea" :rows="2"
                    placeholder="如：透析中追加自费药品" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { label, tagType, fmtTime } from '../utils/labels'

const list = ref([])
const loading = ref(false)
const status = ref('PENDING')
const dialog = ref(false)
const form = reactive({})
const editId = ref(null)

const total = (row) =>
  (Number(row.insuranceAmount) + Number(row.selfPayAmount) + Number(row.extraCharge)).toFixed(2)

const pendingTotal = computed(() =>
  list.value.reduce((sum, r) => sum + Number(total(r)), 0))

async function load() {
  loading.value = true
  try {
    list.value = await api.billings(status.value)
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editId.value = row.id
  Object.assign(form, {
    insuranceAmount: Number(row.insuranceAmount),
    selfPayAmount: Number(row.selfPayAmount),
    extraCharge: Number(row.extraCharge),
    extraReason: row.extraReason || ''
  })
  dialog.value = true
}

async function save() {
  await api.updateBilling(editId.value, form)
  ElMessage.success('已保存')
  dialog.value = false
  await load()
}

async function confirm(row) {
  await ElMessageBox.confirm(
    `确认核对 ${row.patientName} 的账单？合计 ${total(row)} 元，确认后治疗单完成。`,
    '收费核对', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  )
  await api.confirmBilling(row.id)
  ElMessage.success('已核对确认')
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 12px; }
.spacer { flex: 1; }
</style>
