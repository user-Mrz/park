<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  createAccount,
  deleteAccount,
  getAccounts,
  getRoles,
  resetAccountPassword,
  updateAccount,
  updateAccountStatus
} from '@/api/admin'

interface Row {
  id: string
  username: string
  nickname: string
  role: string
  roleName: string
  status: number
  createdAt: string
  menus: string[]
}

const loading = ref(false)
const rows = ref<Row[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const status = ref<number | ''>('')

// RBAC：可分配的角色（排除超管角色，超管由系统内置账号独占）
const roleOptions = ref<Array<{ roleCode: string; roleName: string }>>([])
// 菜单 code -> 中文名，用于展示角色带来的菜单权限
const allMenuLabels = ref<Record<string, string>>({})

const loadRoles = async () => {
  try {
    const res: any = await getRoles()
    const records: any[] = res.data?.records || []
    roleOptions.value = records
      .filter((r) => r.roleCode !== 'super_admin')
      .map((r) => ({ roleCode: r.roleCode, roleName: r.roleName }))
    allMenuLabels.value = res.data?.allMenuLabels || {}
  } catch (e) {
    roleOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getAccounts({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      status: status.value === '' ? undefined : status.value
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadRoles()
  await loadData()
})

const onSearch = () => {
  page.value = 1
  loadData()
}
const onReset = () => {
  keyword.value = ''
  status.value = ''
  page.value = 1
  loadData()
}

const fmtTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

/* ============ 创建 / 编辑 ============ */
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
const form = reactive<{ username: string; password: string; nickname: string; roleCode: string }>({
  username: '',
  password: '',
  nickname: '',
  roleCode: ''
})
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,30}$/, message: '3-30 位字母/数字/下划线', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
}
const rulesEdit = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const openCreate = () => {
  isEdit.value = false
  form.username = ''
  form.password = ''
  form.nickname = ''
  // 默认选中内置普通管理员角色
  form.roleCode = roleOptions.value.find((r) => r.roleCode === 'admin')?.roleCode || roleOptions.value[0]?.roleCode || ''
  dialogVisible.value = true
}

const openEdit = (row: any) => {
  isEdit.value = true
  editId.value = row.id
  form.username = row.username
  form.password = ''
  form.nickname = row.nickname
  form.roleCode = row.role
  dialogVisible.value = true
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await updateAccount(editId.value, { nickname: form.nickname, roleCode: form.roleCode })
        ElMessage.success('修改成功')
      } else {
        await createAccount({
          username: form.username,
          password: form.password,
          nickname: form.nickname,
          roleCode: form.roleCode
        })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      // 拦截器已弹错误提示
    }
  })
}

/* ============ 分配角色 ============ */
const roleDialogVisible = ref(false)
const roleId = ref('')
const roleUsername = ref('')
const roleForm = reactive<{ roleCode: string }>({ roleCode: '' })

const openAssignRole = (row: any) => {
  roleId.value = row.id
  roleUsername.value = row.username
  roleForm.roleCode = row.role
  roleDialogVisible.value = true
}

const submitRole = async () => {
  if (!roleForm.roleCode) {
    ElMessage.warning('请选择角色')
    return
  }
  try {
    const row = rows.value.find((r) => r.id === roleId.value)
    await updateAccount(roleId.value, { nickname: row?.nickname || '', roleCode: roleForm.roleCode })
    ElMessage.success('角色已更新')
    roleDialogVisible.value = false
    loadData()
  } catch (e) {
    // 拦截器已弹错误提示
  }
}

/* ============ 启停 / 删除 ============ */
const onToggleStatus = async (row: any) => {
  const next = row.status === 1 ? 0 : 1
  const label = next === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${label}账号「${row.username}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateAccountStatus(row.id, next)
    ElMessage.success(`${label}成功`)
    loadData()
  } catch (e) {
    // ignore
  }
}

const onDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.username}」？此操作不可恢复`, '危险操作', { type: 'error' })
  } catch {
    return
  }
  try {
    await deleteAccount(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    // ignore
  }
}

/* ============ 重置密码 ============ */
const pwdDialogVisible = ref(false)
const pwdId = ref('')
const pwdUsername = ref('')
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive<{ newPassword: string }>({ newPassword: '' })
const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度 6-50', trigger: 'blur' }
  ]
}

const openResetPwd = (row: any) => {
  pwdId.value = row.id
  pwdUsername.value = row.username
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}

const submitPwd = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await resetAccountPassword(pwdId.value, { newPassword: pwdForm.newPassword })
      ElMessage.success('重置成功')
      pwdDialogVisible.value = false
    } catch (e) {
      // ignore
    }
  })
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>管理员账号管理</span>
          <el-button type="primary" size="small" @click="openCreate">新建管理员</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="用户名 / 昵称 模糊搜索"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        />
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="停用" />
        </el-select>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
        <span class="search-tip">支持任意一项独立搜索</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%; margin-top: 12px">
        <el-table-column label="ID" prop="id" width="180" show-overflow-tooltip />
        <el-table-column label="用户名" prop="username" min-width="140" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickname" min-width="140" show-overflow-tooltip />
        <el-table-column label="角色" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'super_admin' ? 'danger' : 'primary'">
              {{ row.roleName || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色菜单权限" min-width="240">
          <template #default="{ row }">
            <template v-if="row.role === 'super_admin'">
              <el-tag type="danger" size="small">全部权限</el-tag>
            </template>
            <template v-else>
              <el-tag v-for="c in row.menus" :key="c" size="small" style="margin: 2px">
                {{ allMenuLabels[c] || c }}
              </el-tag>
              <span v-if="!row.menus || row.menus.length === 0" style="color: #909399">无</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ fmtTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: wrap; gap: 4px">
              <el-button size="small" @click="openEdit(row)" :disabled="row.role === 'super_admin'">编辑</el-button>
              <el-button size="small" type="warning" @click="openResetPwd(row)" :disabled="row.role === 'super_admin'">重置密码</el-button>
              <el-button size="small" type="success" @click="openAssignRole(row)" :disabled="row.role === 'super_admin'">分配角色</el-button>
              <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain @click="onToggleStatus(row)" :disabled="row.role === 'super_admin'">
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="onDelete(row)" :disabled="row.role === 'super_admin'">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        background
        layout="total, prev, pager, next, jumper"
        :total="total"
        style="margin-top: 16px; justify-content: flex-end; display: flex"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新建/编辑 对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑管理员' : '新建管理员'" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="isEdit ? rulesEdit : rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="3-30 位字母/数字/下划线" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-50 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="最长 50 字符" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="form.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.roleCode" :value="r.roleCode" :label="r.roleName" />
          </el-select>
          <div class="form-tip">菜单权限由所选角色决定，可在「角色管理」中调整</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 对话框 -->
    <el-dialog v-model="pwdDialogVisible" :title="`重置「${pwdUsername}」的密码`" width="460px" append-to-body>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-50 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 对话框 -->
    <el-dialog v-model="roleDialogVisible" :title="`分配角色：${roleUsername}`" width="480px" append-to-body>
      <div style="margin-bottom: 12px; color: #909399; font-size: 12px">
        账号的菜单权限完全来自所选角色（RBAC）。如需调整权限，请到「角色管理」中修改该角色的菜单。
      </div>
      <el-form label-width="60px">
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.roleCode" :value="r.roleCode" :label="r.roleName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.search-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
.time-text {
  color: #606266;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
</style>
