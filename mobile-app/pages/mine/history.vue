<template>
  <view class="page">
    <page-header title="浏览记录" />
    <view class="section-title">最近浏览</view>
    <view v-if="loading" class="state-box">正在加载...</view>
    <view v-else-if="error" class="state-box" @tap="load">{{ error }}，点击重试</view>
    <view v-else-if="!items.length" class="state-box">还没有浏览记录</view>
    <view v-else class="history-list">
      <view v-for="item in items" :key="item.id || `${item.targetType}-${item.targetId}`" class="history-card" @tap="goTarget(item)">
        <image v-if="item.coverUrl" class="history-cover" :src="item.coverUrl" mode="aspectFill" />
        <view v-else class="history-cover cover-placeholder">羽</view>
        <view class="history-main">
          <view class="history-title">{{ item.title || '内容已不可见' }}</view>
          <view class="history-meta">
            <text>{{ item.targetType === 'POST' ? '帖子' : '新闻' }}</text>
            <text>{{ item.ownerName || '羽球在线' }}</text>
          </view>
          <view class="muted">{{ item.viewedAt || '' }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import PageHeader from '../../components/page-header/page-header.vue';
import { api } from '../../utils/api';
import { isAuthError } from '../../utils/auth';

const items = ref([]);
const loading = ref(true);
const error = ref('');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.records || data?.list || [];
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    items.value = itemsOf(await api.getHistory());
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

function goTarget(item) {
  if (!item?.targetId) return;
  const path = item.targetType === 'POST' ? '/pages/post/detail' : '/pages/news/detail';
  uni.navigateTo({ url: `${path}?id=${item.targetId}` });
}

onLoad(load);
</script>

<style scoped>
.history-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.history-card {
  display: flex;
  gap: 16rpx;
  padding: 16rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #ffffff;
  box-shadow: 6rpx 6rpx 0 rgba(23, 33, 23, 0.16);
}

.history-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 150rpx;
  width: 150rpx;
  height: 110rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  background: #e7f6d8;
  color: #172117;
  font-size: 42rpx;
  font-weight: 900;
}

.history-main {
  flex: 1;
  min-width: 0;
}

.history-title {
  display: -webkit-box;
  overflow: hidden;
  color: #172117;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 1.35;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 18rpx;
  margin: 8rpx 0;
  color: #2b7f56;
  font-size: 23rpx;
  font-weight: 800;
}
</style>
