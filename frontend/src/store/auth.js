import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('hd_token') || '',
    user: JSON.parse(localStorage.getItem('hd_user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.user?.role || '',
    roleLabel: (state) => {
      const map = { ADMIN: '护士长/管理', NURSE: '护士', DOCTOR: '医生', ENGINEER: '工程', FINANCE: '收费' }
      return map[state.user?.role] || ''
    }
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('hd_token', token)
      localStorage.setItem('hd_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('hd_token')
      localStorage.removeItem('hd_user')
    }
  }
})
