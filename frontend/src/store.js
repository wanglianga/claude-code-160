import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('hdms_token') || '',
    user: JSON.parse(localStorage.getItem('hdms_user') || 'null')
  }),
  getters: {
    isLogin: s => !!s.token,
    role: s => s.user?.role || '',
    name: s => s.user?.name || ''
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('hdms_token', token)
      localStorage.setItem('hdms_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('hdms_token')
      localStorage.removeItem('hdms_user')
    }
  }
})
