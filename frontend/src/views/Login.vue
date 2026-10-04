<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">
        <el-icon size="30" color="#409eff"><FirstAidKit /></el-icon>
        <h2>血透中心管理平台</h2>
        <p>排班 · 消毒 · 耗材 · 异常上报一体化</p>
      </div>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                    :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider content-position="left">测试账号</el-divider>
      <div class="accounts">
        <div v-for="a in accounts" :key="a.u" class="acc" @click="fill(a)">
          <el-tag size="small">{{ a.role }}</el-tag> {{ a.u }} / {{ a.p }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import api from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

const accounts = [
  { u: 'admin', p: 'admin123', role: '护士长' },
  { u: 'nurse1', p: '123456', role: '护士' },
  { u: 'doctor1', p: '123456', role: '医生' },
  { u: 'eng1', p: '123456', role: '工程' },
  { u: 'fin1', p: '123456', role: '收费' }
]

function fill(a) {
  form.username = a.u
  form.password = a.p
}

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await api.login(form.username, form.password)
    auth.setLogin(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.name}`)
    router.push('/dashboard')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b4c6f 100%);
}
.login-card { width: 420px; border-radius: 10px; }
.title { text-align: center; margin-bottom: 18px; }
.title h2 { margin: 8px 0 4px; color: #303133; }
.title p { margin: 0; color: #909399; font-size: 13px; }
.btn { width: 100%; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acc { cursor: pointer; font-size: 12px; color: #606266; padding: 2px 6px; border-radius: 4px; }
.acc:hover { background: #ecf5ff; }
</style>
