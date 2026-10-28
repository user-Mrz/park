import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'

const APP = '智能校园停车场管理系统'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '管理员登录', public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: `首页 - ${APP}`, menuCode: 'dashboard' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/index.vue'),
        meta: { title: `用户管理 - ${APP}`, menuCode: 'users' }
      },
      {
        path: 'user-roles',
        name: 'user-roles',
        component: () => import('@/views/user-role/index.vue'),
        meta: { title: `用户角色 - ${APP}`, menuCode: 'users' }
      },
      {
        path: 'departments',
        name: 'departments',
        component: () => import('@/views/department/index.vue'),
        meta: { title: `学院管理 - ${APP}`, menuCode: 'departments' }
      },
      {
        path: 'spots',
        name: 'spots',
        component: () => import('@/views/spot/index.vue'),
        meta: { title: `常驻车位管理 - ${APP}`, menuCode: 'spots' }
      },
      {
        path: 'zones',
        name: 'zones',
        component: () => import('@/views/zone/index.vue'),
        meta: { title: `停车区域管理 - ${APP}`, menuCode: 'spots' }
      },
      {
        path: 'tidal',
        name: 'tidal',
        component: () => import('@/views/tidal/index.vue'),
        meta: { title: `潮汐车位管理 - ${APP}`, menuCode: 'tidal' }
      },
      {
        path: 'reservations',
        name: 'reservations',
        component: () => import('@/views/reservation/index.vue'),
        meta: { title: `预约管理 - ${APP}`, menuCode: 'reservations' }
      },
      {
        path: 'vehicles',
        name: 'vehicles',
        component: () => import('@/views/vehicle/index.vue'),
        meta: { title: `车辆进出 - ${APP}`, menuCode: 'vehicles' }
      },
      {
        path: 'recharges',
        name: 'recharges',
        component: () => import('@/views/recharge/index.vue'),
        meta: { title: `充值记录 - ${APP}`, menuCode: 'recharges' }
      },
      {
        path: 'payments',
        name: 'payments',
        component: () => import('@/views/payment/index.vue'),
        meta: { title: `扣费记录 - ${APP}`, menuCode: 'payments' }
      },
      {
        path: 'monthly-passes',
        name: 'monthly-passes',
        component: () => import('@/views/monthly-pass/index.vue'),
        meta: { title: `月卡管理 - ${APP}`, menuCode: 'monthly_passes' }
      },
      {
        path: 'visits',
        name: 'visits',
        component: () => import('@/views/visit/index.vue'),
        meta: { title: `访客与家长报备 - ${APP}`, menuCode: 'visits' }
      },
      {
        path: 'announcements',
        name: 'announcements',
        component: () => import('@/views/announcement/index.vue'),
        meta: { title: `公告管理 - ${APP}`, menuCode: 'announcements' }
      },
      {
        path: 'violations',
        name: 'violations',
        component: () => import('@/views/violation/index.vue'),
        meta: { title: `违规记录 - ${APP}`, menuCode: 'violations' }
      },
      {
        path: 'blacklist',
        name: 'blacklist',
        component: () => import('@/views/blacklist/index.vue'),
        meta: { title: `黑白名单 - ${APP}`, menuCode: 'blacklist' }
      },
      {
        path: 'config',
        name: 'config',
        component: () => import('@/views/config/index.vue'),
        meta: { title: `系统配置 - ${APP}`, menuCode: 'config' }
      },
      {
        path: 'rules',
        name: 'rules',
        component: () => import('@/views/rules/index.vue'),
        meta: { title: `停车规则 - ${APP}`, menuCode: 'rules' }
      },
      {
        path: 'server',
        name: 'server',
        component: () => import('@/views/server/index.vue'),
        meta: { title: `硬件监控 - ${APP}`, menuCode: 'server' }
      },
      {
        path: 'jobs',
        name: 'jobs',
        component: () => import('@/views/job/index.vue'),
        meta: { title: `定时任务 - ${APP}`, menuCode: 'jobs' }
      },
      // ---- 超管专属 ----
      {
        path: 'accounts',
        name: 'accounts',
        component: () => import('@/views/account/index.vue'),
        meta: { title: `管理员账号管理 - ${APP}`, menuCode: 'accounts' }
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/role/index.vue'),
        meta: { title: `角色管理 - ${APP}`, menuCode: 'accounts' }
      },
      {
        path: 'login-logs',
        name: 'login-logs',
        component: () => import('@/views/login-log/index.vue'),
        meta: { title: `登录日志 - ${APP}`, menuCode: 'login_logs' }
      },
      {
        path: 'operation-logs',
        name: 'operation-logs',
        component: () => import('@/views/operation-log/index.vue'),
        meta: { title: `操作日志 - ${APP}`, menuCode: 'operation_logs' }
      },
      // 个人中心：不设 menuCode，所有角色的管理员都可访问（仅能操作自己的资料与密码）
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: `个人中心 - ${APP}` }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 菜单编码 → 落地路由（顺序即优先级）。
 * 用于给「没有首页权限」的角色选择第一个可访问页面，避免登录后卡在 /dashboard。
 * 一个 menuCode 可能对应多个路由（如 users 含 user-roles），这里取主路由。
 */
const MENU_LANDING: Array<[string, string]> = [
  ['dashboard', '/dashboard'],
  ['users', '/users'],
  ['departments', '/departments'],
  ['spots', '/spots'],
  ['tidal', '/tidal'],
  ['reservations', '/reservations'],
  ['vehicles', '/vehicles'],
  ['visits', '/visits'],
  ['announcements', '/announcements'],
  ['violations', '/violations'],
  ['blacklist', '/blacklist'],
  ['monthly_passes', '/monthly-passes'],
  ['recharges', '/recharges'],
  ['payments', '/payments'],
  ['config', '/config'],
  ['rules', '/rules'],
  ['server', '/server'],
  ['jobs', '/jobs'],
  ['accounts', '/accounts'],
  ['login_logs', '/login-logs'],
  ['operation_logs', '/operation-logs']
]

/** 当前账号登录后的默认落地页；没有任何菜单权限时返回 /login */
export function firstAuthorizedPath(role: string, myMenus: string[]): string {
  if (role === 'super_admin') return '/dashboard'
  const allowed = new Set(myMenus || [])
  const hit = MENU_LANDING.find(([menuCode]) => allowed.has(menuCode))
  return hit ? hit[1] : '/login'
}

const prefetchedPaths = new Set<string>()


export function prefetchRoute(path: string) {
  if (!path || prefetchedPaths.has(path)) return
  const matched = router.resolve(path).matched
  const record = matched[matched.length - 1]
  const loader = record?.components?.default
  if (typeof loader === 'function') {
    prefetchedPaths.add(path)
    ;(loader as () => Promise<unknown>)().catch(() => prefetchedPaths.delete(path))
  }
}

/**
 * 路由守卫：
 * 1. 未登录访问受保护路由 -> /login
 * 2. 已登录访问 /login -> 首个可访问页面
 * 3. 普通管理员访问不在 myMenus 内的菜单 -> 静默跳到首个可访问页面
 *    超管（role === 'super_admin'）跳过菜单校验
 */
router.beforeEach((to) => {
  const token = localStorage.getItem('admin_token')
  if (!to.meta.public && !token) {
    return { path: '/login', replace: true }
  }
  if (to.path === '/login' && token) {
    const userStore = useUserStore()
    const landing = firstAuthorizedPath(userStore.role, userStore.myMenus)
    if (landing === '/login') {
     
      userStore.logout()
      return true
    }
    return { path: landing, replace: true }
  }
  // 菜单权限校验
  if (to.meta.menuCode) {
    const userStore = useUserStore()
    // 保护：token 存在但 role 缺失或 myMenus 为空（旧缓存残留），视为未登录态
    if (token && !userStore.role && (!userStore.myMenus || userStore.myMenus.length === 0)) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role')
      localStorage.removeItem('admin_my_menus')
      return { path: '/login', replace: true }
    }
    if (userStore.role === 'super_admin') {
      return true
    }
    if (!userStore.myMenus.includes(to.meta.menuCode as string)) {
      const landing = firstAuthorizedPath(userStore.role, userStore.myMenus)
      // 未分配任何菜单时退出登录；目标即落地页仍无权限属异常数据，同样退出
      if (landing === '/login' || landing === to.path) {
        userStore.logout()
        return { path: '/login', replace: true }
      }
      // 无当前页权限时跳到该账号第一个可访问页面（如公告管理员 -> /announcements）
      return { path: landing, replace: true }
    }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title || APP
})

export default router
