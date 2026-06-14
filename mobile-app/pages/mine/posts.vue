<template>
  <view class="page">
    <page-header title="我的发布" />
    <view class="section-title">我的发布</view>
    <view v-if="loading" class="state-box">正在加载...</view>
    <view v-else-if="error" class="state-box" @tap="load">{{ error }}，点击重试</view>
    <view v-else>
      <view v-if="!items.length" class="state-box">还没有发布记录</view>
      <view v-if="items.length" class="post-list">
        <feed-card v-for="item in items" :key="item.id" :item="normalizePost(item)" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import FeedCard from '../../components/feed-card/feed-card.vue';
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

function normalizePost(item) {
  return {
    ...item,
    feedType: 'POST',
    categoryName: item.topicName || item.categoryName || '球友社区',
    summary: item.summary || item.content || ''
  };
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const all = itemsOf(await api.getMyPosts());
    items.value = all.filter((item) => item.status !== 'DRAFT');
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

onShow(load);
</script>

<style scoped>
.post-list {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

</style>
