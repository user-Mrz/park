<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getRoleMenus,
  getRoles,
  updateRole
} from '@/api/admin'

interface RoleRow {
  id: string
  roleCode: string
  roleName: string
  description: string
  builtin: number
  menus: string[]
  adminCount: number
  createdAt: string
}

const loading = ref(false)
const rows = ref<RoleRow[]>([])
const keyword = ref('')

// 菜单字典（后端下发）
const allMenuCodes = ref<string[]>([])
const allMenuLabels = ref<Record<string, string>>({})
const superOnlyMenus = ref<string[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getRoles({ keyword: keyword.value || undefined })
    rows.value = res.data?.records || []
    allMenuCodes.value = res.data?.allMenuCodes || []
    allMenuLabels.value = res.data?.allMenuLabels || {}
    superOnlyMenus.value = res.data?.superOnlyMenus || []
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())

const onSearch = () => loadData()
const onReset = () => {
  keyword.value = ''
  loadData()
}

const fmtTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

const menuLabel = (code: string) => allMenuLabels.value[code] || code

/* ============ 新建 / 编辑 ============ */
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
const form = reactive<{ roleCode: string; roleName: string; description: string }>({
  roleCode: '',
  roleName: '',
  description: ''
})
const rules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{2,50}$/, message: '2-50 位字母/数字/下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}
const rulesEdit = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const openCreate = () => {
  isEdit.value = false
  form.roleCode = ''
  form.roleName = ''
  form.description = ''
  dialogVisible.value = true
}

const openEdit = (row: any) => {
  isEdit.value = true
  editId.value = row.id
  form.roleCode = row.roleCode
  form.roleName = row.roleName
  form.description = row.description
  dialogVisible.value = true
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await updateRole(editId.value, { roleName: form.roleName, description: form.description })
        ElMessage.success('修改成功')
      } else {
        await createRole({ roleCode: form.roleCode, roleName: form.roleName, description: form.description })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      // 拦截器已弹错误提示
    }
  })
}

/* ============ 分配菜单 ============ */
const menuDialogVisible = ref(false)
const menuRoleId = ref('')
const menuRoleName = ref('')
const menuTreeRef = ref<any>(null)
const menuTreeData = ref<any[]>([])
const menuTreeProps = ref<{ label: string; children: string }>({ label: 'label', children: 'children' })

const buildTree = () =>
  allMenuCodes.value.map((code: string) => ({
    code,
    label: allMenuLabels.value[code] || code,
    disabled: superOnlyMenus.value.includes(code)
  }))

const openAssignMenus = async (row: any) => {
  menuRoleId.value = row.id
  menuRoleName.value = row.roleName
  menuDialogVisible.value = true
  menuTreeData.value = buildTree()
  let menus: string[] = []
  try {
    const res: any = await getRoleMenus(row.id)
    menus = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    menus = []
  }
  setTimeout(() => {
    menuTreeRef.value?.setCheckedKeys(menus)
  }, 50)
}

const submitMenus = async () => {
  const checked = menuTreeRef.value?.getCheckedKeys(false) || []
  try {
    await assignRoleMenus(menuRoleId.value, { menuCodes: checked })
    ElMessage.success('分配成功')
    menuDialogVisible.value = false
    loadData()
  } catch (e) {
    // 拦截器已弹错误提示
  }
}

/* ============ 删除 ============ */
const onDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定删除角色「${row.roleName}」？删除后该角色的菜单权限一并清除`,
      '危险操作',
      { type: 'error' }
    )
  } catch {
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    // 拦截器已弹错误提示
  }
}
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>角色管理（RBAC）</span>
          <el-button type="primary" size="small" @click="openCreate">新建角色</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        title="管理员账号通过绑定角色获得菜单权限；角色的菜单权限在此维护。超级管理员角色拥有全部权限，无需分配。"
      />

      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="角色编码 / 名称 模糊搜索"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" size="default" style="width: 100%; margin-top: 12px">
        <el-table-column label="角色名称" prop="roleName" min-width="140" show-overflow-tooltip />
        <el-table-column label="角色编码" prop="roleCode" min-width="150" show-overflow-tooltip />
        <el-table-column label="描述" prop="description" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.builtin === 1 ? 'warning' : 'success'">
              {{ row.builtin === 1 ? '内置' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号数" prop="adminCount" width="90" align="center" />
        <el-table-column label="菜单权限" min-width="260">
          <template #default="{ row }">
            <template v-if="row.roleCode === 'super_admin'">
              <el-tag type="danger" size="small">全部权限</el-tag>
            </template>
            <template v-else>
              <el-tag v-for="c in row.menus" :key="c" size="small" style="margin: 2px">
                {{ menuLabel(c) }}
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
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: wrap; gap: 4px">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button
                size="small"
                type="success"
                @click="openAssignMenus(row)"
                :disabled="row.roleCode === 'super_admin'"
              >分配菜单</el-button>
              <el-button
                size="small"
                type="danger"
                @click="onDelete(row)"
                :disabled="row.builtin === 1"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新建角色'" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="isEdit ? rulesEdit : rules" label-width="80px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isEdit" placeholder="如 parking_manager，创建后不可修改" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 停车管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选，最长 255 字符" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="menuDialogVisible" :title="`分配菜单权限：${menuRoleName}`" width="520px" append-to-body>
      <div style="margin-bottom: 8px; color: #909399; font-size: 12px">
        灰色禁用项为超管专属菜单，不可分配给普通角色。保存后将全量覆盖该角色已有的菜单权限，已绑定该角色的账号下次请求即生效。
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        show-checkbox
        node-key="code"
        :props="menuTreeProps"
        default-expand-all
        style="max-height: 400px; overflow-y: auto"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMenus">保存</el-button>
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
.time-text {
  color: #606266;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
</style>
