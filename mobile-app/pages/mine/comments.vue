<template>
  <view class="page">
    <page-header title="我的评论" />
    <view class="section-title">我的评论</view>
    <view v-if="loading" class="state-box">正在加载...</view>
    <view v-else-if="error" class="state-box" @tap="load">{{ error }}，点击重试</view>
    <view v-else-if="!items.length" class="state-box">还没有评论记录</view>
    <view v-else class="comment-list">
      <view v-for="item in items" :key="item.id" class="comment-card" @tap="goTarget(item)">
        <view class="target-row">
          <image v-if="item.targetCoverUrl" class="target-cover" :src="item.targetCoverUrl" mode="aspectFill" />
          <view class="target-main">
            <view class="comment-title">{{ item.targetTitle || '内容已不可见' }}</view>
            <view class="muted">{{ item.targetType === 'POST' ? '帖子' : '新闻' }} · {{ item.targetOwnerName || '羽球在线' }}</view>
          </view>
        </view>
        <view v-if="item.parentContent" class="parent-content">回复：{{ item.parentContent }}</view>
        <view class="comment-content">{{ item.content }}</view>
        <view class="muted">{{ item.createdAt || '' }}</view>
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
    items.value = itemsOf(await api.getComments());
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
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.comment-card {
  padding: 20rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #ffffff;
  box-shadow: 6rpx 6rpx 0 rgba(23, 33, 23, 0.18);
}

.target-row {
  display: flex;
  gap: 14rpx;
  align-items: center;
}

.target-cover {
  flex: 0 0 118rpx;
  width: 118rpx;
  height: 86rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  background: #e7f6d8;
}

.target-main {
  flex: 1;
  min-width: 0;
}

.comment-title {
  color: #172117;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.35;
}

.comment-content {
  margin: 14rpx 0;
  color: #3d493a;
  font-size: 27rpx;
  line-height: 1.6;
}

.parent-content {
  margin-top: 16rpx;
  padding: 12rpx;
  border-radius: 8rpx;
  background: #f4fee9;
  color: #65705f;
  font-size: 24rpx;
  line-height: 1.5;
}
</style>
