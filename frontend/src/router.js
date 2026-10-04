import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/LoginView.vue') },
  {
    path: '/',
    component: () => import('./views/LayoutView.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('./views/DashboardView.vue'), meta: { title: '今日总览' } },
      { path: 'schedule', component: () => import('./views/ScheduleBoardView.vue'), meta: { title: '排班看板', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'DOCTOR'] } },
      { path: 'nurse', component: () => import('./views/NurseWorkbenchView.vue'), meta: { title: '透析工作台', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'DOCTOR'] } },
      { path: 'engineering', component: () => import('./views/EngineeringView.vue'), meta: { title: '机器与消毒', roles: ['ADMIN', 'HEAD_NURSE', 'ENGINEER'] } },
      { path: 'consumables', component: () => import('./views/ConsumablesView.vue'), meta: { title: '耗材库存', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'ENGINEER'] } },
      { path: 'exceptions', component: () => import('./views/ExceptionsView.vue'), meta: { title: '异常上报' } },
      { path: 'billing', component: () => import('./views/BillingView.vue'), meta: { title: '收费核对', roles: ['ADMIN', 'FINANCE'] } },
      { path: 'patients', component: () => import('./views/PatientsView.vue'), meta: { title: '患者档案', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'DOCTOR'] } },
      { path: 'reports', component: () => import('./views/ReportsView.vue'), meta: { title: '管理报表', roles: ['ADMIN', 'HEAD_NURSE'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const token = localStorage.getItem('hdms_token')
  if (to.path === '/login') return true
  if (!token) return '/login'
  if (to.meta.roles) {
    const user = JSON.parse(localStorage.getItem('hdms_user') || 'null')
    if (!user || !to.meta.roles.includes(user.role)) return '/dashboard'
  }
  return true
})

export default router
