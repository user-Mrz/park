import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, Outlet } from 'react-router-dom'
import { Layout, Menu, Button, Badge, App as AntdApp } from 'antd'
import {
  CarOutlined,
  FundOutlined,
  EnvironmentOutlined,
  CalendarOutlined,
  WalletOutlined,
  MoneyCollectOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
  WarningOutlined,
  StopOutlined,
  ApartmentOutlined
} from '@ant-design/icons'
import { useUserStore } from '@/stores/user'
import { getUsers } from '@/api'

const { Sider, Header, Content } = Layout

const menuItems = [
  { key: '/dashboard', icon: <FundOutlined />, label: '首页' },
  { key: '/users', icon: <UserOutlined />, label: '用户管理' },
  { key: '/spots', icon: <EnvironmentOutlined />, label: '车位管理' },
  { key: '/reservations', icon: <CalendarOutlined />, label: '预约管理' },
  { key: '/vehicles', icon: <CarOutlined />, label: '车辆进出' },
  { key: '/visits', icon: <TeamOutlined />, label: '访客与家长报备' },
  { key: '/violations', icon: <WarningOutlined />, label: '违规记录' },
  { key: '/blacklist', icon: <StopOutlined />, label: '车辆黑名单' },
  { key: '/recharges', icon: <WalletOutlined />, label: '充值记录' },
  { key: '/payments', icon: <MoneyCollectOutlined />, label: '扣费记录' },
  { key: '/config', icon: <SettingOutlined />, label: '系统配置' }
]

export default function MainLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const userInfo = useUserStore((s) => s.userInfo)
  const logout = useUserStore((s) => s.logout)
  const [pendingIdentity, setPendingIdentity] = useState(0)

  // 待审核身份数量：用于「用户管理」菜单徽章，路由切换时刷新
  useEffect(() => {
    if (!localStorage.getItem('admin_token')) return
    getUsers({ page: 1, pageSize: 1, identityStatus: 0 })
      .then((res: any) => setPendingIdentity(res?.data?.total || 0))
      .catch(() => setPendingIdentity(0))
  }, [location.pathname])

  const menuItems = useMemo(
    () => [
      { key: '/dashboard', icon: <FundOutlined />, label: '首页' },
      {
        key: '/users',
        icon: <UserOutlined />,
        label: (
          <span>
            用户管理
            {pendingIdentity > 0 && (
              <Badge count={pendingIdentity} size="small" style={{ marginLeft: 8, backgroundColor: '#409EFF' }} />
            )}
          </span>
        )
      },
      { key: '/departments', icon: <ApartmentOutlined />, label: '学院管理' },
      { key: '/spots', icon: <EnvironmentOutlined />, label: '车位管理' },
      { key: '/reservations', icon: <CalendarOutlined />, label: '预约管理' },
      { key: '/vehicles', icon: <CarOutlined />, label: '车辆进出' },
      { key: '/visits', icon: <TeamOutlined />, label: '访客与家长报备' },
      { key: '/violations', icon: <WarningOutlined />, label: '违规记录' },
      { key: '/blacklist', icon: <StopOutlined />, label: '车辆黑名单' },
      { key: '/recharges', icon: <WalletOutlined />, label: '充值记录' },
      { key: '/payments', icon: <MoneyCollectOutlined />, label: '扣费记录' },
      { key: '/config', icon: <SettingOutlined />, label: '系统配置' }
    ],
    [pendingIdentity]
  )

  useEffect(() => {
    if (!userInfo) {
      const stored = localStorage.getItem('admin_token')
      if (stored) {
        // 仅展示管理员名，无需额外请求
        useUserStore.setState({ userInfo: { username: 'Admin' } })
      }
    }
  }, [userInfo])

  const handleLogout = () => {
    logout()
    message.success('已退出登录')
    navigate('/login')
  }

  const displayName = userInfo?.nickname || userInfo?.username || '管理员'

  return (
    <Layout className="layout-container" style={{ height: '100vh' }}>
      <Sider width={232} style={{ backgroundColor: '#304156', overflowY: 'auto' }}>
        <div
          className="logo"
          style={{
            height: 60,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 6,
            color: '#fff',
            padding: '0 8px',
            borderBottom: '1px solid rgba(255,255,255,0.1)',
            overflow: 'hidden'
          }}
        >
          <CarOutlined style={{ fontSize: 22, flexShrink: 0 }} />
          <span
            title="智能校园停车场管理系统"
            style={{
              fontSize: 15,
              fontWeight: 'bold',
              lineHeight: 1.2,
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              minWidth: 0
            }}
          >
            智能校园停车场管理系统
          </span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ backgroundColor: '#304156', borderRight: 'none' }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            borderBottom: '1px solid #e6e6e6',
            padding: '0 20px',
            height: 60,
            lineHeight: '60px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ color: '#333' }}>{displayName}</span>
            <Button type="primary" danger onClick={handleLogout}>
              退出登录
            </Button>
          </div>
        </Header>
        <Content style={{ background: '#f0f2f5', padding: 20, overflowY: 'auto' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
