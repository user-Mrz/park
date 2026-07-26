<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="24"><Van /></el-icon>
        <span>停车场管理系统</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/spots">
          <el-icon><Location /></el-icon>
          <span>车位管理</span>
        </el-menu-item>
        <el-menu-item index="/reservations">
          <el-icon><Calendar /></el-icon>
          <span>预约管理</span>
        </el-menu-item>
        <el-menu-item index="/vehicles">
          <el-icon><Van /></el-icon>
          <span>车辆进出</span>
        </el-menu-item>
        <el-menu-item index="/recharges">
          <el-icon><Wallet /></el-icon>
          <span>充值记录</span>
        </el-menu-item>
        <el-menu-item index="/payments">
          <el-icon><Money /></el-icon>
          <span>扣费记录</span>
        </el-menu-item>
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-right">
          <span class="admin-name">{{ userInfo?.nickname || userInfo?.username || '管理员' }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userInfo = ref<any>(null)

onMounted(() => {
  if (userStore.userInfo) {
    userInfo.value = userStore.userInfo
  } else {
    try {
      const stored = localStorage.getItem('admin_token')
      if (stored) {
        userInfo.value = { username: 'Admin' }
      }
    } catch (e) {}
  }
})

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #304156; overflow-y: auto; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; gap: 8px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.header { background: #fff; display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #e6e6e6; padding: 0 20px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.admin-name { color: #333; }
.main { background: #f0f2f5; padding: 20px; overflow-y: auto; }
</style>
