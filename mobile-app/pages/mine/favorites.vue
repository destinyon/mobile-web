<template>
  <view class="page favorites-page">
    <view class="brand-strip">
      <view class="back" @tap="goBack">‹</view>
      <text class="brand-title-mini">我的收藏</text>
    </view>

    <view class="section-title">我的收藏</view>
    <view v-if="loading" class="state-box">正在加载...</view>
    <view v-else-if="error" class="state-box" @tap="load">{{ error }}，点击重试</view>
    <view v-else-if="!items.length" class="state-box">还没有收藏内容</view>
    <view v-else class="favorite-list">
      <feed-card
        v-for="item in items"
        :key="item.feedKey"
        :item="item"
      />
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import FeedCard from '../../components/feed-card/feed-card.vue';
import { api } from '../../utils/api';
import { isAuthError } from '../../utils/auth';

const items = ref([]);
const loading = ref(true);
const error = ref('');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.records || data?.list || [];
}

function normalizeFavorite(item) {
  const feedType = item.feedType || item.targetType || 'NEWS';
  return {
    ...item,
    feedType,
    feedKey: `${feedType}-${item.id}`,
    categoryName: item.categoryName || item.topicName || (feedType === 'POST' ? '球友社区' : '外部新闻'),
    summary: item.summary || item.content || ''
  };
}

function goBack() {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
    return;
  }
  uni.switchTab({ url: '/pages/mine/index' });
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await api.getFavorites();
    items.value = itemsOf(data).map(normalizeFavorite);
  } catch (err) {
    if (isAuthError(err)) {
      uni.navigateTo({ url: '/pages/login/index' });
      return;
    }
    error.value = err.msg || '加载失败';
  } finally {
    loading.value = false;
  }
}

onLoad(load);
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

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}
</style>
