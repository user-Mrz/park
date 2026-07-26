<script>
export default {
  onLaunch() {
    console.log('App Launch')
    const token = uni.getStorageSync('token')
    if (!token) {
      // Login required, handled per-page
    }
  }
}
</script>

<style>
page { background-color: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
</style>
