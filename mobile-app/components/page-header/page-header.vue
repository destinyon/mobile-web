<template>
  <view class="brand-strip">
    <view class="back" @tap="goBack">‹</view>
    <text class="brand-title-mini">{{ title }}</text>
  </view>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  fallback: {
    type: String,
    default: '/pages/mine/index'
  }
});

function goBack() {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
    return;
  }
  if (isTabPage(props.fallback)) {
    uni.switchTab({ url: props.fallback });
    return;
  }
  uni.redirectTo({ url: props.fallback });
}

function isTabPage(url) {
  return ['/pages/home/index', '/pages/publish/index', '/pages/mine/index'].includes(url);
}
</script>

<style scoped>
.brand-strip {
  display: flex;
  align-items: center;
  height: 64rpx;
  margin-bottom: 16rpx;
}

.back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  color: #172117;
  font-size: 40rpx;
  font-weight: 900;
  line-height: 1;
}

.brand-title-mini {
  flex: 1;
  padding-right: 48rpx;
  font-size: 28rpx;
  font-weight: 800;
  text-align: center;
}
</style>
