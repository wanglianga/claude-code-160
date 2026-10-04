<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="brand-logo">HD</div>
        <div>
          <div class="brand-name">血透调度平台</div>
          <div class="brand-sub">区域血透中心</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="menu" background-color="#14324f"
               text-color="#b8c7d9" active-text-color="#ffffff">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="crumb">{{ $route.meta.title || '' }}</div>
        <div class="user-box">
          <el-tag size="small" effect="plain">{{ label('role', auth.role) }}</el-tag>
          <span class="uname">{{ auth.name }}</span>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store'
import { label } from '../labels'

const router = useRouter()
const auth = useAuthStore()

const ALL = [
  { path: '/dashboard', title: '今日总览', icon: 'Odometer' },
  { path: '/schedule', title: '排班看板', icon: 'Calendar', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'DOCTOR'] },
  { path: '/nurse', title: '透析工作台', icon: 'FirstAidKit', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'DOCTOR'] },
  { path: '/engineering', title: '机器与消毒', icon: 'SetUp', roles: ['ADMIN', 'HEAD_NURSE', 'ENGINEER'] },
  { path: '/consumables', title: '耗材库存', icon: 'Box', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'ENGINEER'] },
  { path: '/exceptions', title: '异常上报', icon: 'Warning' },
  { path: '/billing', title: '收费核对', icon: 'Money', roles: ['ADMIN', 'FINANCE'] },
  { path: '/patients', title: '患者档案', icon: 'User', roles: ['ADMIN', 'HEAD_NURSE', 'NURSE', 'DOCTOR'] },
  { path: '/reports', title: '管理报表', icon: 'DataAnalysis', roles: ['ADMIN', 'HEAD_NURSE'] }
]

const menus = computed(() => ALL.filter(m => !m.roles || m.roles.includes(auth.role)))

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #14324f; }
.brand { display: flex; align-items: center; gap: 10px; padding: 18px 16px; }
.brand-logo { width: 40px; height: 40px; border-radius: 10px; background: #4a90c2; color: #fff;
  display: flex; align-items: center; justify-content: center; font-weight: 700; }
.brand-name { color: #fff; font-weight: 600; font-size: 15px; }
.brand-sub { color: #7f97ad; font-size: 12px; }
.menu { border-right: none; }
:deep(.el-menu-item.is-active) { background: #1f4e79; }
.header { background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed; }
.crumb { font-weight: 600; color: #303133; }
.user-box { display: flex; align-items: center; gap: 10px; }
.uname { font-weight: 500; }
.main { background: #f0f2f5; overflow-y: auto; }
</style>
