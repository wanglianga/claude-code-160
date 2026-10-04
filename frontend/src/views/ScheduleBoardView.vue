<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false"
                        @change="load" />
        <el-button :icon="Refresh" circle @click="load" />
        <el-button v-if="canPlan" type="primary" :icon="Plus" @click="openCreate">新增排班</el-button>
        <el-button v-if="canPlan" type="success" :icon="MagicStick" @click="autoVisible = true">自动排班</el-button>
        <span class="hint">同一患者：空机 · 同组 · 感染分区 · 熟悉护士 · 接送窗口 一并考虑；班次间需留 {{ board.gapMinutes || 30 }} 分钟消毒间隔</span>
      </div>

      <el-table :data="board.machines || []" border v-loading="loading" class="board-table">
        <el-table-column label="机位" width="150" fixed>
          <template #default="{ row }">
            <div class="machine-cell">
              <b>{{ row.machine.code }}</b>
              <el-tag size="small" :type="tagType('zone', row.machine.zone)">{{ label('zone', row.machine.zone) }}</el-tag>
              <el-tag size="small" :type="tagType('machineStatus', row.machine.status)">
                {{ label('machineStatus', row.machine.status) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-for="s in shifts" :key="s" :label="label('shift', s)" min-width="240">
          <template #default="{ row }">
            <div class="shift-cell">
              <div v-for="sc in row.cells[s]" :key="sc.id" class="sched-chip" :class="chipClass(sc)"
                   @click="openActions(sc, $event)">
                <div class="chip-head">
                  <span class="seq">#{{ sc.seqOrder }}</span>
                  <b>{{ sc.patientName }}</b>
                  <el-tag size="small" :type="tagType('risk', sc.riskLevel)">{{ label('risk', sc.riskLevel) }}</el-tag>
                  <el-tag v-if="sc.transportNeeded" size="small" type="info" effect="plain">接送</el-tag>
                </div>
                <div class="chip-sub">
                  <el-tag size="small" :type="tagType('scheduleStatus', sc.status)" effect="dark">
                    {{ label('scheduleStatus', sc.status) }}
                  </el-tag>
                  <span>{{ sc.plannedDurationMin }}min</span>
                  <span v-if="sc.treatmentStatus">· {{ label('treatmentStatus', sc.treatmentStatus) }}</span>
                </div>
              </div>
              <div v-if="!row.cells[s]?.length" class="empty-cell">空机</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card" v-if="board.unscheduled?.length">
      <div class="page-title">当日规律透析但未排班患者（{{ board.unscheduled.length }}）</div>
      <el-tag v-for="p in board.unscheduled" :key="p.id" class="unsched" :type="tagType('zone', p.infectionZone)">
        {{ p.name }} · {{ label('zone', p.infectionZone) }} · {{ label('shift', p.preferredShift) }}
        <template v-if="p.transportNeeded"> · 接送{{ p.transportWindow }}</template>
      </el-tag>
    </div>

    <!-- 新增排班 -->
    <el-dialog v-model="createVisible" title="新增排班" width="480px">
      <el-form label-width="90px">
        <el-form-item label="患者">
          <el-select v-model="createForm.patientId" filterable style="width:100%" @change="onPatientPick">
            <el-option v-for="p in patients" :key="p.id" :value="p.id"
                       :label="`${p.name}（${label('zone', p.infectionZone)} / ${label('risk', p.riskLevel)}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="机器">
          <el-select v-model="createForm.machineId" filterable style="width:100%">
            <el-option v-for="m in compatibleMachines" :key="m.id" :value="m.id"
                       :label="`${m.code}（${label('zone', m.zone)} / ${label('machineStatus', m.status)}）`"
                       :disabled="m.status === 'FAULT' || m.status === 'MAINTENANCE'" />
          </el-select>
        </el-form-item>
        <el-form-item label="班次">
          <el-radio-group v-model="createForm.shift">
            <el-radio-button v-for="s in shifts" :key="s" :value="s">{{ label('shift', s) }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时长(分)">
          <el-input-number v-model="createForm.durationMin" :min="60" :max="480" :step="30" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.note" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 自动排班 -->
    <el-dialog v-model="autoVisible" title="自动排班" width="420px">
      <p class="hint">按患者风险（高危优先）、接送窗口、感染分区与机器状态自动分配空机。</p>
      <el-form label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="autoDate" type="date" value-format="YYYY-MM-DD" :clearable="false" />
        </el-form-item>
        <el-form-item label="班次">
          <el-radio-group v-model="autoShift">
            <el-radio-button v-for="s in shifts" :key="s" :value="s">{{ label('shift', s) }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoVisible = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="doAuto">执行</el-button>
      </template>
    </el-dialog>

    <!-- 排班操作 -->
    <el-popover v-model:visible="actionVisible" :virtual-ref="actionRef" trigger="click" virtual-triggering width="260">
      <div v-if="current" class="action-pop">
        <div class="action-title">
          <b>{{ current.patientName }}</b>
          <el-tag size="small" :type="tagType('scheduleStatus', current.status)">
            {{ label('scheduleStatus', current.status) }}
          </el-tag>
        </div>
        <div class="action-meta">{{ current.machineCode }} · {{ label('shift', current.shift) }} · 第{{ current.seqOrder }}台</div>
        <el-button v-if="canPlan && current.status === 'PLANNED'" size="small" type="primary"
                   @click="openMove">调班 / 换机</el-button>
        <el-button v-if="current.status === 'PLANNED'" size="small" type="warning" @click="doLate">迟到上报</el-button>
        <el-button v-if="current.status === 'PLANNED'" size="small" type="danger" plain
                   @click="doAbsent">未到/请假</el-button>
        <el-button v-if="canPlan && current.status === 'PLANNED'" size="small" type="danger"
                   @click="doCancel">取消排班</el-button>
        <div v-if="current.status !== 'PLANNED'" class="hint">仅“已排班”状态可调整</div>
      </div>
    </el-popover>

    <!-- 调班 -->
    <el-dialog v-model="moveVisible" title="调班 / 换机（含影响预览）" width="640px">
      <div v-if="current" class="move-src">
        将 <b>{{ current.patientName }}</b> 从 {{ current.machineCode }} · {{ label('shift', current.shift) }}
        调整到：
      </div>
      <el-form inline>
        <el-form-item label="目标机器">
          <el-select v-model="moveForm.machineId" style="width:220px" @change="preview = null">
            <el-option v-for="m in machines" :key="m.id" :value="m.id"
                       :label="`${m.code}（${label('zone', m.zone)}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标班次">
          <el-select v-model="moveForm.shift" style="width:140px" @change="preview = null">
            <el-option v-for="s in shifts" :key="s" :value="s" :label="label('shift', s)" />
          </el-select>
        </el-form-item>
        <el-button type="primary" plain :loading="previewing" @click="doPreview">影响预览</el-button>
      </el-form>

      <template v-if="preview">
        <el-alert v-for="(e, i) in preview.errors" :key="'e' + i" :title="e" type="error" :closable="false" class="mb8" />
        <el-descriptions :column="1" border size="small" class="mb8">
          <el-descriptions-item label="目标机位现有队列">
            <span v-if="!preview.targetQueue.length">空机</span>
            <el-tag v-for="(q, i) in preview.targetQueue" :key="i" size="small" class="mr6">
              #{{ q.seqOrder }} {{ q.patientName }}（{{ q.durationMin }}min）
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="受影响患者">
            <el-tag v-for="p in preview.affectedPatients" :key="p" size="small" type="warning" class="mr6">{{ p }}</el-tag>
            <span v-if="!preview.affectedPatients.length">无</span>
          </el-descriptions-item>
          <el-descriptions-item label="消毒时间线">
            <div v-if="preview.timelineIssues.length">
              <div v-for="(t, i) in preview.timelineIssues" :key="i" class="timeline-issue">
                ⚠ {{ t.patientName }}（{{ label('shift', t.shift) }}）：{{ t.message }}
              </div>
            </div>
            <span v-else>消毒间隔充足，无压缩</span>
          </el-descriptions-item>
          <el-descriptions-item label="已预领耗材">
            <el-tag v-for="(c, i) in preview.preDrawnConsumables" :key="i" size="small" type="info" class="mr6">
              {{ c.consumable }}×{{ c.quantity }}
            </el-tag>
            <span v-if="!preview.preDrawnConsumables.length">无</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!preview || preview.errors.length > 0" :loading="saving"
                   @click="doMove">确认调班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Plus, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { label, tagType } from '../labels'
import { useAuthStore } from '../store'

const auth = useAuthStore()
const canPlan = computed(() => ['ADMIN', 'HEAD_NURSE'].includes(auth.role))
const shifts = ['MORNING', 'AFTERNOON', 'EVENING']

const date = ref(new Date().toISOString().slice(0, 10))
const board = ref({})
const loading = ref(false)
const saving = ref(false)

const createVisible = ref(false)
const createForm = ref({ patientId: null, machineId: null, shift: 'MORNING', durationMin: 240, note: '' })
const patients = ref([])
const machines = ref([])

const autoVisible = ref(false)
const autoDate = ref(new Date().toISOString().slice(0, 10))
const autoShift = ref('MORNING')

const actionVisible = ref(false)
const actionRef = ref(null)
const current = ref(null)

const moveVisible = ref(false)
const moveForm = ref({ machineId: null, shift: 'MORNING' })
const preview = ref(null)
const previewing = ref(false)

const compatibleMachines = computed(() => {
  const p = patients.value.find(x => x.id === createForm.value.patientId)
  if (!p) return machines.value
  return machines.value.filter(m => m.zone === p.infectionZone)
})

function chipClass(sc) {
  return { 'chip-high': sc.riskLevel === 'HIGH', 'chip-done': sc.status === 'COMPLETED' }
}

async function load() {
  loading.value = true
  try {
    board.value = await api.get('/schedules/board', { params: { date: date.value } })
  } finally { loading.value = false }
}

async function loadRefs() {
  const [p, m] = await Promise.all([api.get('/patients'), api.get('/engineering/machines')])
  patients.value = p
  machines.value = m
}

function onPatientPick(id) {
  const p = patients.value.find(x => x.id === id)
  if (p) {
    createForm.value.durationMin = p.usualDurationMin || 240
    if (p.preferredShift) createForm.value.shift = p.preferredShift
    createForm.value.machineId = null
  }
}

function openCreate() { createVisible.value = true }

async function doCreate() {
  if (!createForm.value.patientId || !createForm.value.machineId) return ElMessage.warning('请选择患者和机器')
  saving.value = true
  try {
    await api.post('/schedules', { ...createForm.value, date: date.value })
    ElMessage.success('排班成功')
    createVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

async function doAuto() {
  saving.value = true
  try {
    const r = await api.post('/schedules/auto', { date: autoDate.value, shift: autoShift.value })
    ElMessage.success(`已排 ${r.assigned.length} 人${r.skipped.length ? '，跳过：' + r.skipped.join('、') : ''}`)
    autoVisible.value = false
    date.value = autoDate.value
    load()
  } catch (e) {} finally { saving.value = false }
}

function openActions(sc, evt) {
  current.value = sc
  actionRef.value = evt.currentTarget
  actionVisible.value = true
}

function openMove() {
  actionVisible.value = false
  moveForm.value = { machineId: current.value.machineId, shift: current.value.shift }
  preview.value = null
  moveVisible.value = true
}

async function doPreview() {
  previewing.value = true
  try {
    preview.value = await api.post(`/schedules/${current.value.id}/move/preview`, moveForm.value)
  } catch (e) {} finally { previewing.value = false }
}

async function doMove() {
  saving.value = true
  try {
    await api.post(`/schedules/${current.value.id}/move`, moveForm.value)
    ElMessage.success('调班完成')
    moveVisible.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

async function doLate() {
  actionVisible.value = false
  const { value } = await ElMessageBox.prompt('迟到说明（如交通车晚点）', '迟到上报', { inputValue: '' })
  await api.post(`/schedules/${current.value.id}/late`, { reason: value || '患者迟到' })
  ElMessage.success('已上报迟到异常')
}

async function doAbsent() {
  actionVisible.value = false
  await ElMessageBox.confirm('确认患者未到/请假？已预领耗材将自动退回库存。', '提示', { type: 'warning' })
  await api.post(`/schedules/${current.value.id}/absent`, { reason: '临时请假' })
  ElMessage.success('已标记未到并上报异常')
  load()
}

async function doCancel() {
  actionVisible.value = false
  await ElMessageBox.confirm('确认取消该排班？已预领耗材将自动退回库存。', '提示', { type: 'warning' })
  await api.post(`/schedules/${current.value.id}/cancel`, { reason: '护士长取消' })
  ElMessage.success('已取消')
  load()
}

onMounted(() => { load(); loadRefs() })
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; }
.board-table { width: 100%; }
.machine-cell { display: flex; flex-direction: column; gap: 4px; align-items: flex-start; }
.shift-cell { display: flex; flex-direction: column; gap: 6px; min-height: 40px; }
.empty-cell { color: #c0c4cc; font-size: 12px; }
.sched-chip { border: 1px solid #dcdfe6; border-left: 4px solid #409eff; border-radius: 6px;
  padding: 6px 8px; cursor: pointer; background: #fff; transition: box-shadow .15s; }
.sched-chip:hover { box-shadow: 0 2px 8px rgba(0,0,0,.15); }
.chip-high { border-left-color: #f56c6c; }
.chip-done { opacity: .75; background: #f8f9fa; }
.chip-head { display: flex; align-items: center; gap: 6px; }
.seq { color: #909399; font-size: 12px; }
.chip-sub { display: flex; align-items: center; gap: 6px; margin-top: 4px; font-size: 12px; color: #909399; }
.unsched { margin: 0 8px 8px 0; }
.action-pop { display: flex; flex-direction: column; gap: 8px; }
.action-title { display: flex; align-items: center; gap: 8px; font-size: 15px; }
.action-meta { color: #909399; font-size: 12px; }
.move-src { margin-bottom: 12px; }
.mb8 { margin-bottom: 8px; }
.mr6 { margin-right: 6px; }
.timeline-issue { color: #e6a23c; font-size: 13px; padding: 2px 0; }
</style>
