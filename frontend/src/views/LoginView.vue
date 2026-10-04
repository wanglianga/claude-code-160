<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">HD</div>
        <h2>血透中心智能调度平台</h2>
        <p>排班 · 透析记录 · 机器消毒 · 耗材 · 异常上报</p>
      </div>
      <el-form @submit.prevent="doLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large"
                    :prefix-icon="Lock" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider content-position="left">演示账号</el-divider>
      <div class="demo-accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc" @click="fill(a.u, a.p)">
          {{ a.label }} {{ a.u }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store'

const router = useRouter()
const auth = useAuthStore()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

const accounts = [
  { u: 'admin', p: 'admin123', label: '管理员' },
  { u: 'headnurse', p: 'head123', label: '护士长' },
  { u: 'nurse1', p: 'nurse123', label: '护士' },
  { u: 'doctor1', p: 'doctor123', label: '医生' },
  { u: 'eng1', p: 'eng123', label: '工程' },
  { u: 'fin1', p: 'fin123', label: '财务' }
]

function fill(u, p) { username.value = u; password.value = p }

async function doLogin() {
  if (!username.value || !password.value) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const data = await api.post('/auth/login', { username: username.value, password: password.value })
    auth.setLogin(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.name}`)
    router.push('/dashboard')
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f4e79 0%, #2e6da4 50%, #4a90c2 100%); }
.login-box { width: 400px; background: #fff; border-radius: 12px; padding: 36px; box-shadow: 0 12px 40px rgba(0,0,0,.25); }
.login-header { text-align: center; margin-bottom: 24px; }
.logo { width: 56px; height: 56px; margin: 0 auto 12px; border-radius: 14px; background: #1f4e79;
  color: #fff; font-size: 24px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.login-header h2 { margin: 0 0 6px; font-size: 20px; color: #1f4e79; }
.login-header p { margin: 0; color: #909399; font-size: 13px; }
.demo-accounts { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; }
.acc { cursor: pointer; }
</style>
