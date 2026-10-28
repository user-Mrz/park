<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getDepartments,
  addDepartment,
  updateDepartment,
  updateDepartmentStatus,
  deleteDepartment
} from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const open = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const keyword = ref('')

const formRef = ref<FormInstance>()

const form = reactive<any>({
  name: '',
  code: '',
  contact: '',
  phone: '',
  remark: '',
  sortOrder: 99
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入学院/部门名称', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res: any = await getDepartments()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)

const openAdd = async () => {
  isEdit.value = false
  editId.value = 0
  open.value = true
  await nextTick()
  formRef.value?.resetFields()
  form.name = ''
  form.code = ''
  form.contact = ''
  form.phone = ''
  form.remark = ''
  form.sortOrder = 99
}

const openEdit = async (row: any) => {
  isEdit.value = true
  editId.value = row.id
  open.value = true
  await nextTick()
  formRef.value?.clearValidate()
  form.name = row.name
  form.code = row.code || ''
  form.contact = row.contact || ''
  form.phone = row.phone || ''
  form.remark = row.remark || ''
  form.sortOrder = row.sortOrder ?? 99
}

const save = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const values: any = {
      name: form.name,
      code: form.code,
      contact: form.contact,
      phone: form.phone,
      remark: form.remark,
      sortOrder: form.sortOrder
    }
    if (isEdit.value) {
      await updateDepartment(editId.value, values)
      ElMessage.success('已保存（该学院下用户的院系名称已同步更新）')
    } else {
      await addDepartment(values)
      ElMessage.success('已新增学院/部门')
    }
    open.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggle = async (row: any) => {
  await updateDepartmentStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停用（注册与资料页将不再显示）' : '已启用')
  load()
}

const remove = async (row: any) => {
  await deleteDepartment(row.id)
  ElMessage.success('已删除')
  load()
}

/** 客户端筛选：名称包含关键字，或学院代码包含大写后的关键字 */
const filtered = computed(() =>
  rows.value.filter(
    (r) =>
      !keyword.value ||
      String(r.name).includes(keyword.value) ||
      String(r.code || '').includes(keyword.value.toUpperCase())
  )
)

/** 与原 antd Table pagination={{ pageSize: 10 }} 一致：前端分页 */
const clientPage = ref(1)
const pagedRows = computed(() => {
  const start = (clientPage.value - 1) * 10
  return filtered.value.slice(start, start + 10)
})

// 筛选条件变化时回到第一页，避免停在空白页
watch(keyword, () => {
  clientPage.value = 1
})

const totalText = computed(() => `共 ${filtered.value.length} 个学院/部门`)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>学院 / 部门管理</span>
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
            <el-input
              v-model="keyword"
              placeholder="搜索学院名称或代码"
              clearable
              style="width: 220px"
            />
            <el-button @click="load()">刷新</el-button>
            <el-button type="primary" @click="openAdd">
              <el-icon><Plus /></el-icon>
              <span style="margin-left: 4px">新增学院</span>
            </el-button>
          </div>
        </div>
      </template>

      <div style="font-size: 12px; color: #606266; margin-bottom: 12px">
        学生、教师、职工在注册与账号信息页只能从**启用**的学院/部门中选择；停用后不再出现在下拉列表，已有用户不受影响。
      </div>

      <el-table v-loading="loading" :data="pagedRows" row-key="id" size="default" style="width: 100%">
        <el-table-column label="学院 / 部门" min-width="200">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column">
              <span style="font-weight: 600">{{ row.name }}</span>
              <span v-if="row.remark" style="font-size: 12px; color: #606266">{{ row.remark }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="学院代码" width="110">
          <template #default="{ row }">{{ row.code || '—' }}</template>
        </el-table-column>

        <el-table-column label="联系人" width="120">
          <template #default="{ row }">{{ row.contact || '—' }}</template>
        </el-table-column>

        <el-table-column label="联系电话" width="150">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>

        <el-table-column label="排序" prop="sortOrder" width="80" align="center" />

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="info">停用</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div style="display: flex; gap: 4px; align-items: center; justify-content: center">
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm
                :title="
                  row.status === 1
                    ? '停用后注册与资料页将不再显示该学院，确定？'
                    : '确定启用该学院？'
                "
                width="240"
                @confirm="toggle(row)"
              >
                <template #reference>
                  <el-button type="primary" link size="small">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm
                title="确定删除该学院？若仍有用户属于该学院将无法删除"
                width="260"
                confirm-button-type="danger"
                @confirm="remove(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无数据" :image-size="70" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="clientPage"
          background
          layout="total, prev, pager, next"
          :total="filtered.length"
          :page-size="10"
          :hide-on-single-page="false"
          :total-text="totalText"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="open"
      :title="isEdit ? '编辑学院 / 部门' : '新增学院 / 部门'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right">
        <el-form-item label="学院/部门" prop="name">
          <el-input v-model="form.name" placeholder="如 计算机与信息工程学院" />
        </el-form-item>
        <el-form-item label="学院代码" prop="code">
          <el-input v-model="form.code" placeholder="如 CS（选填）" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="如 王辅导员（选填）" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="如 0731-88880001（选填）" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
          <div style="font-size: 12px; color: #606266; line-height: 1.5">数字越小越靠前</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 16px;
}
</style>
