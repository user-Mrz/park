import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '管理员登录' }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '工作台' }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/user/index.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'spots',
          name: 'Spots',
          component: () => import('@/views/spot/index.vue'),
          meta: { title: '车位管理' }
        },
        {
          path: 'reservations',
          name: 'Reservations',
          component: () => import('@/views/reservation/index.vue'),
          meta: { title: '预约管理' }
        },
        {
          path: 'vehicles',
          name: 'Vehicles',
          component: () => import('@/views/vehicle/index.vue'),
          meta: { title: '车辆进出' }
        },
        {
          path: 'recharges',
          name: 'Recharges',
          component: () => import('@/views/recharge/index.vue'),
          meta: { title: '充值记录' }
        },
        {
          path: 'payments',
          name: 'Payments',
          component: () => import('@/views/payment/index.vue'),
          meta: { title: '扣费记录' }
        },
        {
          path: 'config',
          name: 'Config',
          component: () => import('@/views/config/index.vue'),
          meta: { title: '系统配置' }
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '停车场管理系统'
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
