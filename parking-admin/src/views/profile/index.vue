<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateMyNickname, changeMyPassword } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Profile {
  id: string
  username: string
  nickname: string
  roleCode: string
  roleName: string
  status: number
  createdAt: string
}

const loading = ref(false)
const profile = ref<Profile | null>(null)

// 昵称表单
const nickname = ref('')
const savingNickname = ref(false)

// 密码表单
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingPassword = ref(false)

const isSuperAdmin = computed(() => profile.value?.roleCode === 'super_admin')

const loadProfile = async () => {
  loading.value = true
  try {
    const res: any = await getMyProfile()
    profile.value = res.data || null
    nickname.value = profile.value?.nickname || ''
    // 同步顶栏展示名，避免修改昵称后仍显示旧名称
    if (profile.value) {
      userStore.userInfo = { ...(userStore.userInfo || {}), ...profile.value }
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)

const saveNickname = async () => {
  const value = nickname.value.trim()
  if (!value) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (value.length > 32) {
    ElMessage.warning('昵称不能超过 32 个字符')
    return
  }
  if (profile.value && value === profile.value.nickname) {
    ElMessage.info('昵称未变化')
    return
  }
  savingNickname.value = true
  try {
    await updateMyNickname(value)
    if (profile.value) profile.value.nickname = value
    userStore.userInfo = { ...(userStore.userInfo || {}), nickname: value }
    ElMessage.success('昵称已更新')
  } finally {
    savingNickname.value = false
  }
}

const savePassword = async () => {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (newPassword.length < 6 || newPassword.length > 50) {
    ElMessage.warning('新密码长度需为 6-50 位')
    return
  }
  if (newPassword === oldPassword) {
    ElMessage.warning('新密码不能与原密码相同')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await changeMyPassword(oldPassword, newPassword)
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    ElMessage.success('密码已修改，请牢记新密码用于下次登录')
  } finally {
    savingPassword.value = false
  }
}

const fmtTime = (t: any) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <div class="card-header">
          <span>我的账号信息</span>
          <el-tag v-if="isSuperAdmin" type="danger" effect="plain">超级管理员</el-tag>
          <el-tag v-else type="primary" effect="plain">{{ profile?.roleName || '管理员' }}</el-tag>
        </div>
      </template>

      <div v-loading="loading">
        <el-descriptions v-if="profile" :column="2" border>
          <el-descriptions-item label="登录账号">{{ profile.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ profile.nickname || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="所属角色">{{ profile.roleName || profile.roleCode }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <el-tag :type="profile.status === 1 ? 'success' : 'info'">
              {{ profile.status === 1 ? '正常' : '已停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ fmtTime(profile.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="登录账号由超级管理员分配，如需变更请联系超级管理员"
          style="margin-top: 12px"
        />
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>修改昵称</template>
          <el-form label-width="100px" label-position="left">
            <el-form-item label="新昵称">
              <el-input v-model="nickname" maxlength="32" show-word-limit placeholder="用于看板与日志展示" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingNickname" @click="saveNickname">保存昵称</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" style="margin-bottom: 16px">
          <template #header>修改登录密码</template>
          <el-form label-width="100px" label-position="left">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="pwdForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="6-50 位"
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPassword" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
          <el-alert
            type="warning"
            show-icon
            :closable="false"
            title="修改密码需要校验原密码；修改后当前登录仍然有效，下次登录请使用新密码"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
