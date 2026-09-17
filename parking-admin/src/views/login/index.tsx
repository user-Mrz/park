import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, App as AntdApp } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'

interface LoginForm {
  username: string
  password: string
  captchaCode: string
}

export default function Login() {
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()
  const login = useUserStore((s) => s.login)
  const [form] = Form.useForm<LoginForm>()
  const [loading, setLoading] = useState(false)
  const [captchaImage, setCaptchaImage] = useState('')
  const [captchaKey, setCaptchaKey] = useState('')

  const refreshCaptcha = async () => {
    try {
      const res: any = await getCaptcha()
      setCaptchaKey(res.data.key)
      setCaptchaImage(res.data.image)
    } catch {
      /* ignore */
    }
  }

  useEffect(() => {
    refreshCaptcha()
  }, [])

  const handleLogin = async (values: LoginForm) => {
    setLoading(true)
    try {
      await login(values.username, values.password, captchaKey, values.captchaCode)
      message.success('登录成功')
      navigate('/dashboard')
    } catch (e: any) {
      message.error(e?.message || '登录失败')
      refreshCaptcha()
      form.setFieldValue('captchaCode', '')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="login-container"
      style={{
        height: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)'
      }}
    >
      <div
        className="login-card"
        style={{
          width: 420,
          padding: 40,
          background: '#fff',
          borderRadius: 12,
          boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
        }}
      >
        <h2 style={{ textAlign: 'center', color: '#303133', marginBottom: 4 }}>智能校园停车场管理系统</h2>
        <p style={{ textAlign: 'center', color: '#909399', marginBottom: 30, fontSize: 14 }}>管理员登录</p>
        <Form form={form} size="large" initialValues={{ username: 'admin' }} onFinish={handleLogin}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>
          <Form.Item name="captchaCode" rules={[{ required: true, message: '请输入验证码' }]}>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <Input placeholder="验证码" style={{ width: '60%' }} />
              {captchaImage && (
                <img
                  src={captchaImage}
                  onClick={refreshCaptcha}
                  alt="captcha"
                  style={{
                    height: 40,
                    cursor: 'pointer',
                    borderRadius: 4,
                    border: '1px solid #dcdfe6'
                  }}
                />
              )}
            </div>
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登 录
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}
