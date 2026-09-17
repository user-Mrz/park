import { lazy, Suspense, useEffect } from 'react'
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom'
import { Spin } from 'antd'
import MainLayout from '@/layouts/MainLayout'

const Login = lazy(() => import('@/views/login'))
const Dashboard = lazy(() => import('@/views/dashboard'))
const Users = lazy(() => import('@/views/user'))
const Spots = lazy(() => import('@/views/spot'))
const Reservations = lazy(() => import('@/views/reservation'))
const Vehicles = lazy(() => import('@/views/vehicle'))
const Recharges = lazy(() => import('@/views/recharge'))
const Payments = lazy(() => import('@/views/payment'))
const Visits = lazy(() => import('@/views/visit'))
const Violations = lazy(() => import('@/views/violation'))
const Blacklist = lazy(() => import('@/views/blacklist'))
const Departments = lazy(() => import('@/views/department'))
const Config = lazy(() => import('@/views/config'))

/** 路由守卫：未登录跳转登录页；已登录访问 /login 跳转首页 */
function AuthGuard() {
  const token = localStorage.getItem('admin_token')
  if (!token) return <Navigate to="/login" replace />
  return <Outlet />
}

function LoginRedirect() {
  const token = localStorage.getItem('admin_token')
  if (token) return <Navigate to="/dashboard" replace />
  return <Login />
}

/** 路由懒加载容器 */
function PageLoading({ children }: { children: React.ReactNode }) {
  return (
    <Suspense
      fallback={
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
          <Spin size="large" />
        </div>
      }
    >
      {children}
    </Suspense>
  )
}

function withTitle(title: string, element: React.ReactNode) {
  return (
    <PageTitle title={title}>{element}</PageTitle>
  )
}

function PageTitle({ title, children }: { title: string; children: React.ReactNode }) {
  useEffect(() => {
    document.title = title
  }, [title])
  return <>{children}</>
}

const router = createBrowserRouter([
  {
    path: '/login',
    element: withTitle('管理员登录', <LoginRedirect />)
  },
  {
    path: '/',
    element: <AuthGuard />,
    children: [
      {
        path: '/',
        element: <MainLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard', element: withTitle('首页 - 智能校园停车场管理系统', <PageLoading><Dashboard /></PageLoading>) },
          { path: 'users', element: withTitle('用户管理 - 智能校园停车场管理系统', <PageLoading><Users /></PageLoading>) },
          { path: 'departments', element: withTitle('学院管理 - 智能校园停车场管理系统', <PageLoading><Departments /></PageLoading>) },
          { path: 'spots', element: withTitle('车位管理 - 智能校园停车场管理系统', <PageLoading><Spots /></PageLoading>) },
          { path: 'reservations', element: withTitle('预约管理 - 智能校园停车场管理系统', <PageLoading><Reservations /></PageLoading>) },
          { path: 'vehicles', element: withTitle('车辆进出 - 智能校园停车场管理系统', <PageLoading><Vehicles /></PageLoading>) },
          { path: 'recharges', element: withTitle('充值记录 - 智能校园停车场管理系统', <PageLoading><Recharges /></PageLoading>) },
          { path: 'payments', element: withTitle('扣费记录 - 智能校园停车场管理系统', <PageLoading><Payments /></PageLoading>) },
          { path: 'visits', element: withTitle('访客与家长报备 - 智能校园停车场管理系统', <PageLoading><Visits /></PageLoading>) },
          { path: 'violations', element: withTitle('违规记录 - 智能校园停车场管理系统', <PageLoading><Violations /></PageLoading>) },
          { path: 'blacklist', element: withTitle('车辆黑名单 - 智能校园停车场管理系统', <PageLoading><Blacklist /></PageLoading>) },
          { path: 'config', element: withTitle('系统配置 - 智能校园停车场管理系统', <PageLoading><Config /></PageLoading>) }
        ]
      }
    ]
  },
  { path: '*', element: <Navigate to="/" replace /> }
])

export default router
