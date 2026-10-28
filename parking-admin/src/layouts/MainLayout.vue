<script setup lang="ts">
import { computed, onMounted, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { prefetchRoute } from '@/router'
import HeaderClock from '@/components/HeaderClock.vue'
import { getUsers } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pendingIdentity = ref(0)
/** 待审核徽章节流：快速连续切换菜单时避免每次都打一次用户列表接口 */
let lastPendingAt = 0

/** 待审核身份数量：用于「用户管理」菜单徽章，路由切换时刷新（15 秒内最多一次） */
const loadPending = (force = false) => {
  if (!localStorage.getItem('admin_token')) return
  // 仅普通管理员有 users 菜单时才拉取徽章
  const hasUsers = userStore.role === 'super_admin' || userStore.myMenus.includes('users')
  if (!hasUsers) {
    pendingIdentity.value = 0
    return
  }
  const nowMs = Date.now()
  if (!force && nowMs - lastPendingAt < 15000) return
  lastPendingAt = nowMs
  getUsers({ page: 1, pageSize: 1, identityStatus: 0 })
    .then((res: any) => {
      pendingIdentity.value = res?.data?.total || 0
    })
    .catch(() => {
      pendingIdentity.value = 0
    })
}

loadPending(true)
watch(() => route.path, () => loadPending())

// 仅展示管理员名，无需额外请求
if (!userStore.userInfo && localStorage.getItem('admin_token')) {
  userStore.userInfo = { username: 'Admin' }
}

/**
 * 全量菜单定义（含 menuCode 字段）。
 * 超管（role === super_admin）渲染全部；普通管理员按 myMenus 过滤。
 */
type MenuItem = {
  index: string
  label: string
  icon: string | Component
  badge?: boolean
  menuCode?: string
  children?: Array<{ index: string; label: string; icon: string | Component; menuCode: string }>
}

const ALL_MENUS: MenuItem[] = [
  { index: '/dashboard', label: '首页', icon: 'Fund', menuCode: 'dashboard' },
  {
    index: '/users-group',
    label: '用户管理',
    icon: 'User',
    badge: true,
    children: [
      { index: '/users', label: '用户列表', icon: 'User', menuCode: 'users' },
      { index: '/user-roles', label: '用户角色', icon: 'Key', menuCode: 'users' }
    ]
  },
  { index: '/departments', label: '学院管理', icon: 'OfficeBuilding', menuCode: 'departments' },
  {
    index: '/spots-group',
    label: '车位管理',
    icon: 'Location',
    children: [
      { index: '/spots', label: '常驻车位管理', icon: 'Location', menuCode: 'spots' },
      { index: '/zones', label: '停车区域管理', icon: 'MapLocation', menuCode: 'spots' },
      { index: '/tidal', label: '潮汐车位管理', icon: 'Sunny', menuCode: 'tidal' }
    ]
  },
  { index: '/reservations', label: '预约管理', icon: 'Calendar', menuCode: 'reservations' },
  { index: '/vehicles', label: '车辆进出', icon: 'Van', menuCode: 'vehicles' },
  { index: '/visits', label: '访客与家长报备', icon: 'UserFilled', menuCode: 'visits' },
  { index: '/announcements', label: '公告管理', icon: 'Bell', menuCode: 'announcements' },
  { index: '/violations', label: '违规记录', icon: 'Warning', menuCode: 'violations' },
  { index: '/blacklist', label: '黑白名单', icon: 'CircleClose', menuCode: 'blacklist' },
  { index: '/monthly-passes', label: '月卡管理', icon: 'Ticket', menuCode: 'monthly_passes' },
  {
    index: '/finance-group',
    label: '用户商业记录',
    icon: 'WalletFilled',
    children: [
      { index: '/recharges', label: '充值记录', icon: 'Wallet', menuCode: 'recharges' },
      { index: '/payments', label: '扣费记录', icon: 'Money', menuCode: 'payments' }
    ]
  },
  {
    index: '/system-group',
    label: '系统管理',
    icon: 'Tools',
    children: [
      { index: '/config', label: '系统配置', icon: 'Setting', menuCode: 'config' },
      { index: '/rules', label: '停车规则', icon: 'Reading', menuCode: 'rules' },
      { index: '/server', label: '硬件监控', icon: 'Monitor', menuCode: 'server' },
      { index: '/jobs', label: '定时任务', icon: 'Timer', menuCode: 'jobs' }
    ]
  },
  // 超管专属父级菜单
  {
    index: '/admin-mgmt',
    label: '管理员管理',
    icon: 'Avatar',
    children: [
      { index: '/accounts', label: '管理员账号', icon: 'UserFilled', menuCode: 'accounts' },
      { index: '/roles', label: '角色管理', icon: 'Key', menuCode: 'accounts' },
      { index: '/login-logs', label: '登录日志', icon: 'Histogram', menuCode: 'login_logs' },
      { index: '/operation-logs', label: '操作日志', icon: 'Document', menuCode: 'operation_logs' }
    ]
  }
]

/** 预加载菜单对应的页面 chunk：悬停父级时预取其全部子页，悬停子项时预取当前页 */
const prefetchMenu = (menu: MenuItem) => {
  menu.children?.forEach((child) => prefetchRoute(child.index))
}

/**
 * 首屏空闲时逐个预热全部菜单页面：
 * 只加载组件模块、不挂载页面、不发业务请求；分批执行避免一次性占用主线程。
 */
onMounted(() => {
  const paths = ALL_MENUS.flatMap((m) => (m.children?.length ? m.children.map((c) => c.index) : [m.index]))
  const idle = (window as any).requestIdleCallback as
    | ((cb: () => void, opts?: { timeout: number }) => number)
    | undefined
  const schedule = (cb: () => void) => {
    if (typeof idle === 'function') idle(cb, { timeout: 2000 })
    else window.setTimeout(cb, 300)
  }
  const warmNext = () => {
    const path = paths.shift()
    if (!path) return
    prefetchRoute(path)
    schedule(warmNext)
  }
  schedule(warmNext)
})

const isSuperAdmin = computed(() => userStore.role === 'super_admin')

/**
 * 计算当前用户可见菜单：
 * - 超管：全部
 * - 普通管理员：保留菜单项中 menuCode 在 myMenus 内的；
 *   父级菜单仅当至少有一个子菜单可见时才渲染；
 *   「管理员管理」父级菜单仅超管可见（无论是否在 myMenus 内，因后端拒绝普通管理员访问其子菜单）。
 */
const menus = computed<MenuItem[]>(() => {
  if (isSuperAdmin.value) return ALL_MENUS
  const allowed = new Set(userStore.myMenus)
  const result: MenuItem[] = []
  for (const m of ALL_MENUS) {
    if (m.children && m.children.length > 0) {
      const visible = m.children.filter((c) => allowed.has(c.menuCode))
      if (visible.length > 0) {
        result.push({ ...m, children: visible })
      }
    } else if (m.menuCode && allowed.has(m.menuCode)) {
      result.push(m)
    }
  }
  return result
})

/** el-menu 引用：用于路由切换时自动展开对应父级 sub-menu */
const menuRef = ref<any>(null)

/** 计算当前路由对应的父级 sub-menu index（用于初始展开） */
const defaultOpeneds = computed(() => {
  for (const m of menus.value) {
    if (m.children?.some((c) => c.index === route.path)) {
      return [m.index]
    }
  }
  return []
})

/** 路由切换时自动展开目标父级 sub-menu（避免点击子菜单后父级收起） */
watch(() => route.path, (newPath) => {
  for (const m of menus.value) {
    if (m.children?.some((c) => c.index === newPath)) {
      menuRef.value?.open(m.index)
      break
    }
  }
})

const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员')

const handleSelect = (index: string) => {
  if (index !== route.path) router.push(index)
}

/** 顶栏进入个人中心（所有角色可用，仅操作自己的资料与密码） */
const goProfile = () => {
  if (route.path !== '/profile') router.push('/profile')
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')

  window.setTimeout(() => {
    window.location.replace('/login')
  }, 400)
}
</script>

<template>
  <el-container class="layout-container">
    <el-aside width="232px" class="tech-sider">
      <div class="logo">
        <el-icon class="logo-icon"><Van /></el-icon>
        <span class="logo-text" title="智能校园停车场管理系统">智能校园停车场管理系统</span>
      </div>
      <el-menu
        ref="menuRef"
        :default-active="route.path"
        :default-openeds="defaultOpeneds"
        class="tech-menu"
        background-color="transparent"
        text-color="#303133"
        active-text-color="#409eff"
        @select="handleSelect"
      >
        <template v-for="m in menus" :key="m.index">
          <el-sub-menu v-if="m.children" :index="m.index" @mouseenter="prefetchMenu(m)">
            <template #title>
              <el-icon><component :is="m.icon" /></el-icon>
              <span>{{ m.label }}</span>
              <el-badge
                v-if="m.badge && pendingIdentity > 0"
                :value="pendingIdentity"
                class="menu-badge"
                type="primary"
              />
            </template>
            <el-menu-item
              v-for="c in m.children"
              :key="c.index"
              :index="c.index"
              @mouseenter="prefetchRoute(c.index)"
            >
              <el-icon><component :is="c.icon" /></el-icon>
              <span>{{ c.label }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.index" @mouseenter="prefetchRoute(m.index)">
            <el-icon><component :is="m.icon" /></el-icon>
            <span>{{ m.label }}</span>
            <el-badge
              v-if="m.badge && pendingIdentity > 0"
              :value="pendingIdentity"
              class="menu-badge"
              type="primary"
            />
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="layout-main">
      <el-header class="tech-header" height="60px">
        <HeaderClock class="header-clock" />
        <div class="header-right">
          <span class="welcome">欢迎回来</span>
          <span class="user-name user-name-link" title="点击进入个人中心" @click="goProfile">{{ displayName }}</span>
          <el-button type="primary" plain size="small" @click="goProfile">个人中心</el-button>
          <el-button type="danger" plain size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="tech-content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-main {
  min-width: 0;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #1d2129;
  padding: 0 8px;
  border-bottom: 1px solid #ebeef5;
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08), transparent 70%);
  overflow: hidden;
}
.logo-icon {
  font-size: 22px;
  flex-shrink: 0;
  color: #409eff;
}
.logo-text {
  font-size: 14px;
  font-weight: bold;
  letter-spacing: 1px;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}
.tech-menu {
  border-right: none;
  padding-top: 6px;
}
.tech-menu :deep(.el-menu-item.is-active) {
  background: #ecf5ff !important;
  color: #409eff !important;
  font-weight: 600;
  box-shadow: inset 3px 0 0 #409eff;
}
.tech-menu :deep(.el-menu-item:hover) {
  background: #f5f7fa !important;
}
.menu-badge {
  margin-left: 8px;
}
.tech-header {
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-clock {
  color: #606266;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.welcome {
  color: #606266;
  font-size: 13px;
}
.user-name {
  color: #1d2129;
  font-weight: 600;
}
.user-name-link {
  cursor: pointer;
  transition: color 0.15s ease;
}
.user-name-link:hover {
  color: #409eff;
}
.tech-content {
  background: #ffffff;
  padding: 20px;
  overflow-y: auto;
}
</style>
