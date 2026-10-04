<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="姓名 / 病历号" clearable style="width:220px"
                  :prefix-icon="Search" @input="load" />
        <div class="spacer" />
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增患者</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="mrn" label="病历号" width="80" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column label="感染标记" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('InfectionMarker', row.infectionMarker)">
              {{ label('InfectionMarker', row.infectionMarker) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType('RiskLevel', row.riskLevel)">
              {{ label('RiskLevel', row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通路" width="120">
          <template #default="{ row }">{{ label('AccessType', row.accessType) }}</template>
        </el-table-column>
        <el-table-column prop="dryWeight" label="干体重" width="70" />
        <el-table-column label="固定透析日" width="110">
          <template #default="{ row }">{{ patternText(row.usualPattern) }} {{ label('Shift', row.usualShift) }}</template>
        </el-table-column>
        <el-table-column label="接送" width="130">
          <template #default="{ row }">
            {{ label('TransportMode', row.transportMode) }}
            <span v-if="row.transportWindow" class="sub">{{ row.transportWindow }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="familiarNurse" label="熟悉护士" width="90" />
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.active ? 'success' : 'info'">{{ row.active ? '在透' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑患者' : '新增患者'" width="720px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="病历号"><el-input v-model="form.mrn" :disabled="!!form.id" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="4"><el-form-item label="性别"><el-select v-model="form.gender"><el-option value="男" label="男" /><el-option value="女" label="女" /></el-select></el-form-item></el-col>
          <el-col :span="4"><el-form-item label="年龄"><el-input-number v-model="form.age" :min="1" :max="120" controls-position="right" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="感染标记">
              <el-select v-model="form.infectionMarker">
                <el-option v-for="o in options('InfectionMarker')" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel">
                <el-option v-for="o in options('RiskLevel')" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="通路类型">
              <el-select v-model="form.accessType">
                <el-option v-for="o in options('AccessType')" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="干体重(kg)"><el-input-number v-model="form.dryWeight" :step="0.1" :precision="1" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="透析时长(分)"><el-input-number v-model="form.durationMin" :min="120" :max="360" :step="30" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="熟悉护士"><el-input v-model="form.familiarNurse" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="接送方式">
              <el-select v-model="form.transportMode">
                <el-option v-for="o in options('TransportMode')" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="接送时间窗"><el-input v-model="form.transportWindow" placeholder="如 06:50-07:10" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="常用班次">
              <el-select v-model="form.usualShift">
                <el-option v-for="o in options('Shift')" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="固定透析日">
          <el-checkbox-group v-model="patternArr">
            <el-checkbox v-for="(d, i) in ['周一','周二','周三','周四','周五','周六','周日']" :key="i" :value="String(i + 1)">{{ d }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="抗凝方案"><el-input v-model="form.anticoagPlan" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="在透状态"><el-switch v-model="form.active" active-text="在透" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import api from '../api'
import { label, tagType, options } from '../utils/labels'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const dialog = ref(false)
const form = reactive({})
const patternArr = ref([])

const WEEK = ['一', '二', '三', '四', '五', '六', '日']
function patternText(p) {
  if (!p) return '-'
  return '周' + p.split(',').map(d => WEEK[Number(d) - 1]).join('/')
}

async function load() {
  loading.value = true
  try {
    list.value = await api.patients(keyword.value)
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(form, JSON.parse(JSON.stringify(row)))
    patternArr.value = (row.usualPattern || '').split(',').filter(Boolean)
  } else {
    Object.keys(form).forEach(k => delete form[k])
    Object.assign(form, {
      mrn: '', name: '', gender: '男', age: 60, infectionMarker: 'NONE', riskLevel: 'LOW',
      accessType: 'AVF', dryWeight: 60, durationMin: 240, familiarNurse: '',
      transportMode: 'SELF', transportWindow: '', usualShift: 'AM', anticoagPlan: '',
      notes: '', active: true
    })
    patternArr.value = ['1', '3', '5']
  }
  dialog.value = true
}

async function save() {
  if (!form.mrn || !form.name) {
    ElMessage.warning('请填写病历号和姓名')
    return
  }
  form.usualPattern = patternArr.value.join(',')
  if (form.id) {
    const res = await api.updatePatient(form.id, form)
    if (res.warning) {
      ElMessageBox.alert(res.warning, '感染分区提醒', { type: 'warning' })
    } else {
      ElMessage.success('已保存')
    }
  } else {
    await api.createPatient(form)
    ElMessage.success('已创建')
  }
  dialog.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
.spacer { flex: 1; }
.sub { font-size: 12px; color: #909399; margin-left: 4px; }
</style>
