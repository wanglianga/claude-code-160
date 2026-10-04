// 枚举中文映射与标签颜色
export const LABELS = {
  role: { ADMIN: '管理员', HEAD_NURSE: '护士长', NURSE: '护士', DOCTOR: '医生', ENGINEER: '工程', FINANCE: '财务' },
  risk: { LOW: '低危', MEDIUM: '中危', HIGH: '高危' },
  zone: { CLEAN: '清洁区', HBV: '乙肝区', HCV: '丙肝区', ISOLATION: '隔离区' },
  machineStatus: { IDLE: '空闲', IN_USE: '透析中', DISINFECTING: '待消毒', FAULT: '故障', MAINTENANCE: '维护中' },
  shift: { MORNING: '早班', AFTERNOON: '午班', EVENING: '晚班' },
  scheduleStatus: { PLANNED: '已排班', ON_DIALYSIS: '透析中', COMPLETED: '已完成', CANCELLED: '已取消', ABSENT: '未到' },
  treatmentStatus: { PRE_RECORDED: '已评估', ON_DIALYSIS: '透析中', COMPLETED: '已完成' },
  entryType: { VITALS: '生命体征', ALARM: '机器报警', FLUID: '补液', BLOOD_DRAW: '抽血', DISCOMFORT: '患者不适', ORDER_CHANGE: '医嘱变更' },
  disType: { DISINFECTION: '机器消毒', FILTER_CHANGE: '滤芯更换', WATER_TREATMENT: '水处理' },
  txnType: { PRE_DRAW: '预领', USE: '使用', RETURN: '退回', LOSS: '损耗', RESTOCK: '入库' },
  exType: { LATE: '患者迟到', LEAVE: '临时请假', INFECTION_CHANGE: '感染标记变化', MACHINE_FAULT: '机器故障', CONSUMABLE_SHORTAGE: '耗材不足', HYPOTENSION: '透析中低血压', ORDER_CHANGE: '医嘱调整' },
  exStatus: { OPEN: '待处理', ACKNOWLEDGED: '已知晓', RESOLVED: '已解决' },
  billingStatus: { PENDING: '待核对', CONFIRMED: '已核对' },
  departure: { SELF: '自行离开', FAMILY: '家属接送', TRANSPORT: '交通车接送' }
}

export const TAG_TYPES = {
  risk: { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' },
  machineStatus: { IDLE: 'success', IN_USE: 'primary', DISINFECTING: 'warning', FAULT: 'danger', MAINTENANCE: 'info' },
  scheduleStatus: { PLANNED: 'info', ON_DIALYSIS: 'primary', COMPLETED: 'success', CANCELLED: 'info', ABSENT: 'danger' },
  exStatus: { OPEN: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success' },
  billingStatus: { PENDING: 'warning', CONFIRMED: 'success' },
  zone: { CLEAN: 'success', HBV: 'warning', HCV: 'danger', ISOLATION: 'info' }
}

export function label(group, key) {
  return LABELS[group]?.[key] ?? key ?? '-'
}

export function tagType(group, key) {
  return TAG_TYPES[group]?.[key] ?? 'info'
}
