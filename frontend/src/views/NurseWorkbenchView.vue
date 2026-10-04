<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <el-button :icon="Refresh" circle @click="load" />
        <span class="hint">透析流程：透析前评估 → 上机 → 定时监测 → 下机记录；低血压（收缩压&lt;90）自动上报异常</span>
      </div>
      <el-table :data="rows" border v-loading="loading">
        <el-table-column label="患者" width="150">
          <template #default="{ row }">
            <b>{{ row.patientName }}</b>
            <el-tag size="small" :type="tagType('risk', row.riskLevel)" style="margin-left:4px">
              {{ label('risk', row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="机位/班次" width="130">
          <template #default="{ row }">{{ row.machineCode }} · {{ label('shift', row.shift) }}</template>
        </el-table-column>
        <el-table-column label="排班状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType('scheduleStatus', row.status)">{{ label('scheduleStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="治疗进度" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.treatmentStatus" type="primary" effect="plain">
              {{ label('treatmentStatus', row.treatmentStatus) }}
            </el-tag>
            <span v-else class="muted">未开始</span>
          </template>
        </el-table-column>
        <el-table-column label="耗材预领" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="(c, i) in row.consumables" :key="i" size="small" type="info" effect="plain" class="mr4">
              {{ c.name }}×{{ c.quantity }}
            </el-tag>
            <span v-if="!row.consumables?.length" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PLANNED' && !row.treatmentId" size="small" type="primary"
                       @click="openPre(row)">透析前评估</el-button>
            <el-button v-if="row.treatmentStatus === 'PRE_RECORDED'" size="small" type="success"
                       @click="doStart(row)">上机</el-button>
            <el-button v-if="row.treatmentStatus === 'ON_DIALYSIS'" size="small" type="primary"
                       @click="openMonitor(row)">监测记录</el-button>
            <el-button v-if="row.treatmentStatus === 'ON_DIALYSIS'" size="small" type="warning"
                       @click="openComplete(row)">下机</el-button>
            <el-button v-if="row.treatmentId" size="small" @click="openTimeline(row)">治疗记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 透析前评估 -->
    <el-dialog v-model="preVisible" title="透析前评估" width="560px">
      <el-form label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="透前体重(kg)"><el-input-number v-model="preForm.preWeightKg" :precision="1" :step="0.1" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划脱水(ml)"><el-input-number v-model="preForm.plannedUfMl" :step="100" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="收缩压"><el-input-number v-model="preForm.preBpSys" :min="40" :max="260" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="舒张压"><el-input-number v-model="preForm.preBpDia" :min="20" :max="160" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="通路情况">
          <el-select v-model="preForm.accessCondition" style="width:100%" allow-create filterable>
            <el-option v-for="a in accessOptions" :key="a" :value="a" :label="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="上次不适">
          <el-input v-model="preForm.lastDiscomfort" placeholder="如：上次透析中低血压 / 无" />
        </el-form-item>
        <el-form-item label="抗凝方案">
          <el-input v-model="preForm.anticoagPlan" />
        </el-form-item>
        <el-form-item label="调整脱水量">
          <el-switch v-model="preForm.adjustDehydration" active-text="需要调整" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="preForm.preNotes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="preVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doPre">保存评估</el-button>
      </template>
    </el-dialog>

    <!-- 监测抽屉 -->
    <el-drawer v-model="monitorVisible" :title="`透析中监测 — ${current?.patientName || ''}`" size="520px">
      <el-form inline class="entry-form">
        <el-form-item label="类型">
          <el-select v-model="entryForm.type" style="width:130px">
            <el-option v-for="(v, k) in entryTypes" :key="k" :value="k" :label="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="血压">
          <el-input-number v-model="entryForm.bpSys" :min="0" :max="300" placeholder="收缩" style="width:90px" />
          <span>/</span>
          <el-input-number v-model="entryForm.bpDia" :min="0" :max="200" placeholder="舒张" style="width:90px" />
        </el-form-item>
        <el-form-item label="心率">
          <el-input-number v-model="entryForm.heartRate" :min="0" :max="220" style="width:90px" />
        </el-form-item>
        <el-form-item label="说明" style="width:100%">
          <el-input v-model="entryForm.note" placeholder="报警内容 / 补液量 / 不适表现 / 医嘱变更" style="width:100%" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="doEntry">录入</el-button>
      </el-form>
      <el-timeline class="mt16">
        <el-timeline-item v-for="e in entries" :key="e.id" :timestamp="fmtTime(e.recordedAt)"
                          :type="e.type === 'ALARM' ? 'danger' : e.type === 'VITALS' ? 'primary' : 'warning'">
          <div>
            <el-tag size="small" :type="e.type === 'ALARM' ? 'danger' : 'info'" effect="plain">
              {{ label('entryType', e.type) }}
            </el-tag>
            <span v-if="e.bpSys" class="ml8">BP {{ e.bpSys }}/{{ e.bpDia }}</span>
            <span v-if="e.heartRate" class="ml8">HR {{ e.heartRate }}</span>
            <el-tag v-if="e.bpSys && e.bpSys < 90" size="small" type="danger" class="ml8">低血压</el-tag>
          </div>
          <div v-if="e.note" class="muted">{{ e.note }}</div>
          <div class="muted small">记录人：{{ e.recordedBy?.name || '-' }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <!-- 下机 -->
    <el-dialog v-model="completeVisible" title="下机记录" width="520px">
      <el-form label-width="110px">
        <el-form-item label="透后体重(kg)">
          <el-input-number v-model="completeForm.postWeightKg" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="穿刺点止血">
          <el-switch v-model="completeForm.hemostasisOk" active-text="已止血" inactive-text="未止血" />
        </el-form-item>
        <el-form-item label="离开方式">
          <el-radio-group v-model="completeForm.departureMethod">
            <el-radio-button value="SELF">自行离开</el-radio-button>
            <el-radio-button value="FAMILY">家属接送</el-radio-button>
            <el-radio-button value="TRANSPORT">交通车</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="下次排班">
          <el-input v-model="completeForm.nextScheduleNote" placeholder="如：按规律周三早班 / 医生要求加做一次" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.postNotes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="warning" :loading="saving" @click="doComplete">确认下机</el-button>
      </template>
    </el-dialog>

    <!-- 治疗记录时间线 -->
    <el-drawer v-model="timelineVisible" title="完整治疗记录" size="560px">
      <template v-if="timeline.treatment">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="患者">{{ timeline.treatment.schedule?.patient?.name }}</el-descriptions-item>
          <el-descriptions-item label="护士">{{ timeline.treatment.nurse?.name }}</el-descriptions-item>
          <el-descriptions-item label="透前体重">{{ timeline.treatment.preWeightKg }} kg</el-descriptions-item>
          <el-descriptions-item label="透前血压">{{ timeline.treatment.preBpSys }}/{{ timeline.treatment.preBpDia }}</el-descriptions-item>
          <el-descriptions-item label="通路">{{ timeline.treatment.accessCondition }}</el-descriptions-item>
          <el-descriptions-item label="抗凝">{{ timeline.treatment.anticoagPlan }}</el-descriptions-item>
          <el-descriptions-item label="计划脱水">{{ timeline.treatment.plannedUfMl }} ml</el-descriptions-item>
          <el-descriptions-item label="调整脱水">{{ timeline.treatment.adjustDehydration ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="上机时间">{{ fmtTime(timeline.treatment.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="下机时间">{{ fmtTime(timeline.treatment.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="透后体重">{{ timeline.treatment.postWeightKg || '-' }} kg</el-descriptions-item>
          <el-descriptions-item label="止血">{{ timeline.treatment.hemostasisOk == null ? '-' : (timeline.treatment.hemostasisOk ? '已止血' : '未止血') }}</el-descriptions-item>
          <el-descriptions-item label="离开方式">{{ label('departure', timeline.treatment.departureMethod) }}</el-descriptions-item>
          <el-descriptions-item label="下次排班">{{ timeline.treatment.nextScheduleNote || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="page-title mt16">耗材使用</div>
        <el-tag v-for="t in timeline.consumables" :key="t.id" size="small" class="mr4"
                :type="t.type === 'USE' ? 'primary' : 'info'" effect="plain">
          {{ label('txnType', t.type) }} {{ t.consumable?.name }}×{{ t.quantity }}
        </el-tag>
        <span v-if="!timeline.consumables?.length" class="muted">无</span>
        <div class="page-title mt16">监测记录</div>
        <el-timeline>
          <el-timeline-item v-for="e in timeline.entries" :key="e.id" :timestamp="fmtTime(e.recordedAt)">
            <el-tag size="small" effect="plain">{{ label('entryType', e.type) }}</el-tag>
            <span v-if="e.bpSys" class="ml8">BP {{ e.bpSys }}/{{ e.bpDia }}</span>
            <span v-if="e.heartRate" class="ml8">HR {{ e.heartRate }}</span>
            <div v-if="e.note" class="muted">{{ e.note }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { label, tagType, LABELS } from '../labels'

const date = ref(new Date().toISOString().slice(0, 10))
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const current = ref(null)

const preVisible = ref(false)
const preForm = ref({})
const accessOptions = ['内瘘震颤正常', '内瘘震颤减弱', '穿刺点红肿', '穿刺点渗血', '移植物内瘘正常', '导管固定良好']

const monitorVisible = ref(false)
const entries = ref([])
const entryForm = ref({ type: 'VITALS', bpSys: null, bpDia: null, heartRate: null, note: '' })
const entryTypes = LABELS.entryType

const completeVisible = ref(false)
const completeForm = ref({ postWeightKg: null, hemostasisOk: true, departureMethod: 'SELF', nextScheduleNote: '', postNotes: '' })

const timelineVisible = ref(false)
const timeline = ref({})

function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(5, 16) : '-' }

async function load() {
  loading.value = true
  try {
    const board = await api.get('/schedules/board', { params: { date: date.value } })
    const list = []
    for (const m of board.machines || []) {
      for (const s of Object.keys(m.cells)) {
        for (const sc of m.cells[s]) {
          if (sc.status === 'CANCELLED') continue
          list.push({ ...sc, consumables: [] })
        }
      }
    }
    // 拉取每条排班的预领耗材
    await Promise.all(list.map(async r => {
      if (r.status === 'PLANNED' || r.status === 'ON_DIALYSIS') {
        const txns = await api.get(`/consumables/txns/schedule/${r.id}`)
        r.consumables = txns.filter(t => t.type === 'PRE_DRAW')
          .map(t => ({ name: t.consumable.name, quantity: t.quantity }))
      }
    }))
    rows.value = list
  } finally { loading.value = false }
}

function openPre(row) {
  current.value = row
  preForm.value = {
    scheduleId: row.id, preWeightKg: null, preBpSys: 130, preBpDia: 80,
    accessCondition: '内瘘震颤正常', lastDiscomfort: '', anticoagPlan: '',
    adjustDehydration: false, plannedUfMl: 2000, preNotes: ''
  }
  preVisible.value = true
}

async function doPre() {
  saving.value = true
  try {
    await api.post('/treatments/pre', preForm.value)
    ElMessage.success('评估已保存')
    preVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

async function doStart(row) {
  await api.post(`/treatments/${row.treatmentId}/start`)
  ElMessage.success(`${row.patientName} 已上机`)
  load()
}

async function openMonitor(row) {
  current.value = row
  entryForm.value = { type: 'VITALS', bpSys: null, bpDia: null, heartRate: null, note: '' }
  monitorVisible.value = true
  await loadEntries()
}

async function loadEntries() {
  const t = await api.get(`/treatments/${current.value.treatmentId}`)
  entries.value = t.entries
}

async function doEntry() {
  saving.value = true
  try {
    await api.post(`/treatments/${current.value.treatmentId}/entries`, entryForm.value)
    ElMessage.success('已录入')
    if (entryForm.value.bpSys && entryForm.value.bpSys < 90) {
      ElMessage.warning('检测到低血压，已自动上报异常')
    }
    entryForm.value = { type: 'VITALS', bpSys: null, bpDia: null, heartRate: null, note: '' }
    loadEntries()
  } catch (e) {} finally { saving.value = false }
}

function openComplete(row) {
  current.value = row
  completeForm.value = { postWeightKg: null, hemostasisOk: true, departureMethod: 'SELF', nextScheduleNote: '', postNotes: '' }
  completeVisible.value = true
}

async function doComplete() {
  saving.value = true
  try {
    await api.post(`/treatments/${current.value.treatmentId}/complete`, completeForm.value)
    ElMessage.success('已下机：机器转入待消毒，账单已生成')
    completeVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

async function openTimeline(row) {
  timeline.value = await api.get(`/treatments/${row.treatmentId}`)
  timelineVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; }
.muted { color: #909399; font-size: 12px; }
.small { font-size: 11px; }
.mr4 { margin-right: 4px; }
.ml8 { margin-left: 8px; }
.mt16 { margin-top: 16px; }
.entry-form { border-bottom: 1px dashed #ebeef5; padding-bottom: 8px; }
</style>
