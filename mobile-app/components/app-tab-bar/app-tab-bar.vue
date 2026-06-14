<template>
  <view class="tabbar">
    <view
      v-for="(item, index) in list"
      :key="item.pagePath"
      class="tab-item"
      :class="{ active: selected === index, center: item.center }"
      @tap="switchTab(item)"
    >
      <view class="tab-icon">{{ selected === index ? item.activeIcon : item.icon }}</view>
      <view class="tab-text">{{ item.text }}</view>
    </view>
  </view>
</template>

<script setup>
defineProps({
  selected: {
    type: Number,
    default: 0
  }
});

const list = [
  { pagePath: '/pages/home/index', text: '首页', icon: '⌂', activeIcon: '⌂' },
  { pagePath: '/pages/publish/index', text: '发布', icon: '＋', activeIcon: '+', center: true },
  { pagePath: '/pages/mine/index', text: '我的', icon: '♙', activeIcon: '♟' }
];

function switchTab(item) {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1]?.route || '';
  const target = item.pagePath.replace(/^\//, '');
  if (current === target) return;
  uni.switchTab({ url: item.pagePath });
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 112rpx;
  padding-bottom: env(safe-area-inset-bottom);
  border-top: 2rpx solid #172117;
  background: #ffffff;
  box-shadow: 0 -8rpx 0 rgba(23, 33, 23, 0.08);
}

.tab-item {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 112rpx;
  color: #65705f;
  font-size: 22rpx;
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46rpx;
  height: 46rpx;
  border-radius: 50%;
  color: #687264;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1;
}

.tab-text {
  margin-top: 4rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.tab-item.active {
  color: #172117;
}

.tab-item.active .tab-icon {
  background: #ffe59a;
  color: #172117;
  box-shadow: 0 0 0 2rpx #172117 inset;
}

.tab-item.center {
  transform: translateY(-26rpx);
}

.tab-item.center .tab-icon {
  width: 92rpx;
  height: 92rpx;
  border: 4rpx solid #172117;
  border-radius: 50%;
  background: #ffffff;
  color: #5d6657;
  font-size: 52rpx;
  box-shadow: 6rpx 6rpx 0 #172117;
}

.tab-item.center.active .tab-icon {
  background: #baf279;
  color: #172117;
  box-shadow: 6rpx 6rpx 0 #172117;
}

.tab-item.center .tab-text {
  margin-top: 8rpx;
  color: #172117;
}
</style>
