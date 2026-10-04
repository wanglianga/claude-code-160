<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <span class="page-title" style="margin:0">患者档案</span>
        <el-button v-if="canEdit" type="primary" :icon="Plus" @click="openEdit(null)">新增患者</el-button>
      </div>
      <el-table :data="rows" border v-loading="loading">
        <el-table-column prop="mrn" label="病历号" width="90" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="风险" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('risk', row.riskLevel)">{{ label('risk', row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="感染分区" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('zone', row.infectionZone)">{{ label('zone', row.infectionZone) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规律" min-width="140">
          <template #default="{ row }">
            {{ row.patternDays }} · {{ label('shift', row.preferredShift) }}
          </template>
        </el-table-column>
        <el-table-column label="接送" min-width="130">
          <template #default="{ row }">
            <el-tag v-if="row.transportNeeded" size="small" type="info">{{ row.transportWindow }}</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="干体重" width="80">
          <template #default="{ row }">{{ row.dryWeightKg }}kg</template>
        </el-table-column>
        <el-table-column label="时长" width="70">
          <template #default="{ row }">{{ row.usualDurationMin }}′</template>
        </el-table-column>
        <el-table-column prop="anticoagPlan" label="抗凝方案" min-width="130" />
        <el-table-column label="熟悉护士" width="100">
          <template #default="{ row }">{{ row.primaryNurse?.name || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="canEdit" label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑患者' : '新增患者'" width="640px">
      <el-form label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="病历号"><el-input v-model="form.mrn" :disabled="!!form.id" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel" style="width:100%">
                <el-option value="LOW" label="低危" /><el-option value="MEDIUM" label="中危" /><el-option value="HIGH" label="高危" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="感染分区">
              <el-select v-model="form.infectionZone" style="width:100%">
                <el-option value="CLEAN" label="清洁区" /><el-option value="HBV" label="乙肝区" />
                <el-option value="HCV" label="丙肝区" /><el-option value="ISOLATION" label="隔离区" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="透析日">
              <el-input v-model="form.patternDays" placeholder="如 MON,WED,FRI" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次">
              <el-select v-model="form.preferredShift" style="width:100%">
                <el-option value="MORNING" label="早班" /><el-option value="AFTERNOON" label="午班" />
                <el-option value="EVENING" label="晚班" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="需要接送">
              <el-switch v-model="form.transportNeeded" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接送窗口">
              <el-input v-model="form.transportWindow" placeholder="如 06:30-07:00" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="干体重kg"><el-input-number v-model="form.dryWeightKg" :precision="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="时长(分)"><el-input-number v-model="form.usualDurationMin" :step="30" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="抗凝"><el-input v-model="form.anticoagPlan" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { label, tagType } from '../labels'
import { useAuthStore } from '../store'

const auth = useAuthStore()
const canEdit = computed(() => ['ADMIN', 'HEAD_NURSE', 'DOCTOR'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try { rows.value = await api.get('/patients') } finally { loading.value = false }
}

function openEdit(row) {
  form.value = row ? { ...row } : {
    mrn: '', name: '', gender: '男', riskLevel: 'MEDIUM', infectionZone: 'CLEAN',
    patternDays: 'MON,WED,FRI', preferredShift: 'MORNING', transportNeeded: false,
    transportWindow: '', dryWeightKg: 60, usualDurationMin: 240, anticoagPlan: '普通肝素'
  }
  editVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await api.put(`/patients/${form.value.id}`, form.value)
    else await api.post('/patients', form.value)
    ElMessage.success('已保存')
    editVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
