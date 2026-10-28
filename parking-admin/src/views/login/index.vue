<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Van } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { firstAuthorizedPath } from '@/router'
import { getCaptcha } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({
  username: 'admin',
  password: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const refreshCaptcha = async () => {
  try {
    const res: any = await getCaptcha()
    captchaKey.value = res.data.key
    captchaImage.value = res.data.image
  } catch {
    /* ignore */
  }
}

onMounted(() => {
  refreshCaptcha()
})

const handleLogin = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password, captchaKey.value, form.captchaCode)
    const landing = firstAuthorizedPath(userStore.role, userStore.myMenus)
    if (landing === '/login') {
      userStore.logout()
      ElMessage.error('该账号未分配任何菜单权限，请联系超级管理员')
      return
    }
    ElMessage.success('登录成功')
    router.push(landing)
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
    refreshCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div
    class="login-container"
    :style="{
      height: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background:
        'radial-gradient(1000px 500px at 50% 0%, rgba(64,158,255,0.10), transparent 60%), radial-gradient(800px 400px at 100% 100%, rgba(64,158,255,0.07), transparent 60%), #ffffff'
    }"
  >
    <div class="login-card tech-panel" :style="{ width: '420px', padding: '40px', position: 'relative' }">
      <span class="corner" />
      <div :style="{ textAlign: 'center', marginBottom: '30px' }">
        <el-icon :size="40" color="#409eff">
          <Van />
        </el-icon>
        <h2
          :style="{
            textAlign: 'center',
            color: '#1d2129',
            margin: '12px 0 4px',
            letterSpacing: '3px'
          }"
        >
          智能校园停车场管理系统
        </h2>
        <p
          :style="{
            textAlign: 'center',
            color: '#606266',
            marginBottom: 0,
            fontSize: '14px',
            letterSpacing: '6px'
          }"
        >
          管理员登录
        </p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="off">
            <template #prefix>
              <el-icon color="#606266"><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="new-password"
            >
            <template #prefix>
              <el-icon color="#606266"><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div :style="{ display: 'flex', gap: '12px', alignItems: 'center', width: '100%' }">
            <el-input v-model="form.captchaCode" placeholder="验证码" :style="{ width: '60%' }" />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              alt="captcha"
              :style="{
                height: '40px',
                cursor: 'pointer',
                borderRadius: '4px',
                border: '1px solid #dcdfe6'
              }"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">登 录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>

</style>
