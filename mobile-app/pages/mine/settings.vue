<template>
  <view class="page settings-page">
    <page-header title="设置" />

    <view class="about-card card">
      <view class="mascot">羽</view>
      <view class="about-main">
        <view class="about-title">羽球在线</view>
        <view class="about-desc">和球友一起看资讯、聊比赛、记下每一次热爱。</view>
      </view>
    </view>

    <view class="intro-list">
      <view class="intro-item">
        <view class="intro-icon">✦</view>
        <view>
          <view class="intro-title">新鲜资讯</view>
          <view class="intro-desc">聚合羽毛球动态，碎片时间也能跟上赛场节奏。</view>
        </view>
      </view>
      <view class="intro-item">
        <view class="intro-icon">☘</view>
        <view>
          <view class="intro-title">球友社区</view>
          <view class="intro-desc">发布训练心得、装备体验和比赛小故事。</view>
        </view>
      </view>
      <view class="intro-item">
        <view class="intro-icon">♡</view>
        <view>
          <view class="intro-title">个人记录</view>
          <view class="intro-desc">收藏、评论和浏览记录都放在这里，回看更方便。</view>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import PageHeader from '../../components/page-header/page-header.vue';
</script>

<style scoped>
.about-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 26rpx;
  background: #fffdf4;
}

.mascot {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 104rpx;
  width: 104rpx;
  height: 104rpx;
  border: 3rpx solid #172117;
  border-radius: 50%;
  background: #ffe59a;
  color: #172117;
  font-size: 42rpx;
  font-weight: 900;
  box-shadow: 5rpx 5rpx 0 #172117;
}

.about-main {
  min-width: 0;
}

.about-title {
  color: #172117;
  font-size: 34rpx;
  font-weight: 900;
}

.about-desc {
  margin-top: 10rpx;
  color: #566250;
  font-size: 25rpx;
  line-height: 1.55;
}

.intro-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 24rpx;
}

.intro-item {
  display: flex;
  gap: 18rpx;
  padding: 22rpx;
  border: 2rpx solid #d6dfcf;
  border-radius: 16rpx;
  background: #ffffff;
}

.intro-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 54rpx;
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  background: #e4f8ce;
  color: #2b7f56;
  font-size: 28rpx;
  font-weight: 900;
}

.intro-title {
  color: #172117;
  font-size: 28rpx;
  font-weight: 900;
}

.intro-desc {
  margin-top: 6rpx;
  color: #65705f;
  font-size: 24rpx;
  line-height: 1.5;
}

</style>
