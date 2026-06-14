<template>
  <view class="page search-page">
    <view class="brand-strip">
      <view class="back" @tap="goBack">‹</view>
      <text class="brand-title-mini">搜索内容</text>
    </view>

    <view class="search-card">
      <view class="search-bar">
        <text class="search-icon">⌕</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索新闻和投稿"
          confirm-type="search"
          @confirm="onSearch"
        />
        <button class="search-btn" @tap="onSearch">搜索</button>
      </view>
      <view class="topic-row">
        <view
          v-for="item in filters"
          :key="item.type"
          class="topic"
          :class="{ active: activeFilter === item.type }"
          @tap="selectFilter(item)"
        >
          {{ item.label }}
        </view>
      </view>
    </view>

    <view v-if="loading" class="state-box">正在搜索...</view>
    <view v-else-if="error" class="state-box" @tap="onSearch">{{ error }}，点击重试</view>
    <view v-else-if="searched && !items.length" class="state-box">没有找到相关内容</view>
    <view v-else-if="!searched" class="state-box">选择类别或输入关键词开始搜索</view>

    <view v-else class="post-list">
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

const baseFilters = [
  { type: 'all', label: '全部' },
  { type: 'news', label: '外部新闻' }
];

const keyword = ref('');
const filters = ref(baseFilters);
const activeFilter = ref('all');
const items = ref([]);
const loading = ref(false);
const searched = ref(false);
const error = ref('');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.records || data?.list || [];
}

function timeValue(value) {
  const time = new Date(value || '').getTime();
  return Number.isNaN(time) ? 0 : time;
}

function asNewsItem(item) {
  return {
    ...item,
    feedType: 'NEWS',
    feedKey: `NEWS-${item.id}`,
    categoryName: '外部新闻',
    status: '',
    updatedAt: item.updatedAt || item.createdAt || '',
    sortTime: timeValue(item.updatedAt || item.createdAt)
  };
}

function asPostItem(item) {
  return {
    ...item,
    feedType: 'POST',
    feedKey: `POST-${item.id}`,
    categoryName: item.topicName || item.categoryName || '球友社区',
    updatedAt: item.updatedAt || item.createdAt || '',
    sortTime: timeValue(item.updatedAt || item.createdAt)
  };
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function withHighlight(nextItems, searchWord) {
  const trimmed = searchWord.trim();
  if (!trimmed) return nextItems;
  const pattern = new RegExp(escapeRegExp(trimmed), 'ig');
  return nextItems.map((item) => ({
    ...item,
    highlightedTitle: escapeHtml(item.title).replace(pattern, (match) => `<span style="color:#d92d20;">${match}</span>`)
  }));
}

async function loadTopics() {
  try {
    const topics = await api.getTopics();
    filters.value = baseFilters.concat(itemsOf(topics).map((item) => ({
      type: `topic-${item.id}`,
      label: item.name,
      topicId: item.id
    })));
  } catch (err) {
    uni.showToast({ title: err.msg || '类别加载失败', icon: 'none' });
  }
}

function goBack() {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
    return;
  }
  uni.switchTab({ url: '/pages/home/index' });
}

function selectFilter(item) {
  activeFilter.value = item.type;
  if (searched.value || keyword.value.trim()) {
    onSearch();
  }
}

async function onSearch() {
  const filter = filters.value.find((item) => item.type === activeFilter.value) || filters.value[0];
  const searchWord = keyword.value.trim();
  const params = {
    keyword: searchWord || undefined,
    page: 1,
    pageSize: 30,
    sort: 'latest'
  };

  loading.value = true;
  searched.value = true;
  error.value = '';

  try {
    let nextItems = [];
    if (filter.type === 'news') {
      nextItems = itemsOf(await api.getNews(params)).map(asNewsItem);
    } else if (filter.topicId) {
      nextItems = itemsOf(await api.getPosts({ ...params, topicId: filter.topicId })).map(asPostItem);
    } else {
      const [news, posts] = await Promise.all([api.getNews(params), api.getPosts(params)]);
      nextItems = itemsOf(news)
        .map(asNewsItem)
        .concat(itemsOf(posts).map(asPostItem))
        .sort((left, right) => right.sortTime - left.sortTime);
    }
    items.value = withHighlight(nextItems, searchWord);
  } catch (err) {
    error.value = err.msg || '搜索失败';
  } finally {
    loading.value = false;
  }
}

onLoad(loadTopics);
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

.search-card {
  padding: 18rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #ffffff;
  box-shadow: 6rpx 6rpx 0 rgba(23, 33, 23, 0.16);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.search-icon {
  color: #172117;
  font-size: 34rpx;
  line-height: 1;
}

.search-input {
  flex: 1;
  min-width: 0;
  min-height: 72rpx;
  padding: 0 18rpx;
  border: 2rpx solid #d6dfcf;
  border-radius: 8rpx;
  background: #fafff3;
  font-size: 28rpx;
}

.search-btn {
  flex: 0 0 96rpx;
  width: 96rpx;
  height: 64rpx;
  min-height: 0;
  padding: 0;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #baf279;
  color: #172117;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 60rpx;
}

.topic-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6rpx;
  width: 100%;
  margin-top: 16rpx;
}

.topic {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 46rpx;
  padding: 0 2rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  overflow: hidden;
  background: #ffffff;
  color: #172117;
  font-size: 18rpx;
  font-weight: 800;
  line-height: 1;
  text-align: center;
  white-space: nowrap;
}

.topic.active {
  background: #172117;
  color: #ffffff;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 22rpx;
}
</style>
