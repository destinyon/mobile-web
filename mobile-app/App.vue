<script>
export default {
  onLaunch() {
    const token = uni.getStorageSync('token') || '';
    this.globalData.token = token;
    if (uni.hideTabBar) {
      uni.hideTabBar({ animation: false });
    }
  },
  onShow() {
    if (uni.hideTabBar) {
      uni.hideTabBar({ animation: false });
    }
  },
  globalData: {
    token: ''
  }
};
</script>

<style>
@import './styles/app.css';
</style>
