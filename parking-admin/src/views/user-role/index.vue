<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import {
  assignUserRolePerms,
  getUserRolePerms,
  getUserRoles,
  updateUserRole
} from '@/api/admin'

interface UserRoleItem {
  id: string
  roleCode: string
  roleName: string
  identityType: number
  identityLabel: string
  description: string
  builtin: number
  zones: string[]
  zoneNames: string[]
  userCount: number
}

const loading = ref(false)
const rows = ref<UserRoleItem[]>([])
const keyword = ref('')

// 停车区域字典（后端下发）：zone_code -> 中文名
const zoneOptions = ref<Record<string, string>>({})

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getUserRoles({ keyword: keyword.value || undefined })
    rows.value = res.data?.records || []
    zoneOptions.value = res.data?.zoneOptions || {}
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

/* ============ 编辑角色 ============ */
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editId = ref('')
const editRow = ref<UserRoleItem | null>(null)
const form = reactive<{ roleName: string; description: string }>({
  roleName: '',
  description: ''
})
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const openEdit = (row: any) => {
  editId.value = row.id
  editRow.value = row
  form.roleName = row.roleName
  form.description = row.description
  dialogVisible.value = true
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await updateUserRole(editId.value, { roleName: form.roleName, description: form.description })
      ElMessage.success('修改成功')
      dialogVisible.value = false
      loadData()
    } catch (e) {
      // 拦截器已弹错误提示
    }
  })
}

/* ============ 配置可见区域 ============ */
const permDialogVisible = ref(false)
const permRoleId = ref('')
const permRoleName = ref('')
const checkedZones = ref<string[]>([])
const permLoading = ref(false)

// 区域选项列表：label 用中文名，value 用 zone_code
const zoneList = computed(() =>
  Object.entries(zoneOptions.value).map(([code, name]) => ({ code, name }))
)

const openAssignZones = async (row: any) => {
  permRoleId.value = row.id
  permRoleName.value = row.roleName
  checkedZones.value = []
  permDialogVisible.value = true
  permLoading.value = true
  try {
    const res: any = await getUserRolePerms(row.id)
    checkedZones.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    checkedZones.value = []
  } finally {
    permLoading.value = false
  }
}

const submitPerms = async () => {
  try {
    await assignUserRolePerms(permRoleId.value, { zoneCodes: checkedZones.value })
    ElMessage.success('保存成功')
    permDialogVisible.value = false
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
          <span>用户角色管理（小程序端 RBAC）</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        title="用户角色决定小程序用户可预约/查看的停车区域；在此维护角色的名称、描述与可见区域，并可调整用户所属角色。"
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
        <el-table-column label="角色编码" prop="roleCode" min-width="150" show-overflow-tooltip />
        <el-table-column label="角色名称" prop="roleName" min-width="140" show-overflow-tooltip />
        <el-table-column label="校园身份" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.identityLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可见停车区域" min-width="260">
          <template #default="{ row }">
            <el-tag v-for="(n, i) in row.zoneNames" :key="i" type="success" size="small" style="margin: 2px">
              {{ n }}
            </el-tag>
            <span v-if="!row.zoneNames || row.zoneNames.length === 0" style="color: #909399">未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="用户数" prop="userCount" width="90" align="center" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: wrap; gap: 4px">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="success" @click="openAssignZones(row)">配置可见区域</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑角色 -->
    <el-dialog v-model="dialogVisible" title="编辑用户角色" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色编码">
          <el-input :model-value="editRow?.roleCode" disabled />
        </el-form-item>
        <el-form-item label="校园身份">
          <el-input :model-value="editRow?.identityLabel" disabled />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 学生用户" />
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

    <!-- 配置可见区域 -->
    <el-dialog
      v-model="permDialogVisible"
      :title="`配置可见停车区域：${permRoleName}`"
      width="520px"
      append-to-body
    >
      <div style="margin-bottom: 12px; color: #909399; font-size: 12px">
        勾选该角色可以预约/查看的停车区域。保存后将全量覆盖该角色已有的可见区域配置。
      </div>
      <div v-loading="permLoading">
        <el-checkbox-group v-model="checkedZones">
          <el-checkbox v-for="z in zoneList" :key="z.code" :value="z.code" style="display: block; margin: 6px 0">
            {{ z.name }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPerms">保存</el-button>
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
</style>
