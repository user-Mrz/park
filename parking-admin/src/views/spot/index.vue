<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="page-header">
          <span>车位管理</span>
          <el-button type="primary" @click="openAddDialog">新增车位</el-button>
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="spotCode" label="车位编号" width="120" />
        <el-table-column prop="area" label="区域" width="100" />
        <el-table-column prop="floor" label="楼层" width="100" />
        <el-table-column prop="pricePerHour" label="单价(元/小时)" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status===0" type="success">空闲</el-tag>
            <el-tag v-else-if="row.status===1" type="warning">占用</el-tag>
            <el-tag v-else-if="row.status===2" type="info">预约</el-tag>
            <el-tag v-else type="danger">维护</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status===3?'success':'warning'" @click="toggleStatus(row)">
              {{ row.status===3?'恢复':'维护' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车位' : '新增车位'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="车位编号"><el-input v-model="form.spotCode" placeholder="如 A-001" /></el-form-item>
        <el-form-item label="区域"><el-input v-model="form.area" placeholder="如 A区" /></el-form-item>
        <el-form-item label="楼层"><el-input v-model="form.floor" placeholder="如 B1" /></el-form-item>
        <el-form-item label="单价(元/时)"><el-input-number v-model="form.pricePerHour" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSpots, addSpot, updateSpot, updateSpotStatus } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false), saving = ref(false), tableData = ref([])
const page = ref(1), pageSize = ref(50), total = ref(0)
const dialogVisible = ref(false), isEdit = ref(false), editId = ref<number>(0)
const form = reactive({ spotCode: '', area: '', floor: '', pricePerHour: 5, remark: '' })

async function loadData() {
  loading.value = true
  try {
    const res: any = await getSpots({ page: page.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function openAddDialog() {
  isEdit.value = false; editId.value = 0
  Object.assign(form, { spotCode: '', area: '', floor: '', pricePerHour: 5, remark: '' })
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) {
      await updateSpot(editId.value, form)
    } else {
      await addSpot(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 3 ? 0 : 3
  await updateSpotStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  loadData()
}

onMounted(() => { loadData() })
</script>
