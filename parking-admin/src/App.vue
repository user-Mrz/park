<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif; }

/* 管理页面通用样式 */
.page-container .el-card__header {
  font-weight: bold;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
