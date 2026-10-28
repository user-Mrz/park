<script setup lang="ts">
/** 根组件：通过 ConfigProvider 提供 Element Plus 中文语言包（组件按需引入） */
import { onMounted } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'


const onPageShow = (e: PageTransitionEvent) => {
  if (e.persisted && !localStorage.getItem('admin_token')) {
    window.location.replace('/login')
  }
}

onMounted(() => {
  window.addEventListener('pageshow', onPageShow)
})
</script>

<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>
