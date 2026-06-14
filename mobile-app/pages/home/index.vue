<template>
  <view class="page home-page with-tabbar">
    <view class="brand-bar">
      <view class="brand-title"><text class="brand-mark">羽</text>羽球在线</view>
    </view>

    <view class="search-entry" @tap="goSearch">
      <text class="search-icon">⌕</text>
      <text class="search-text">搜索外部新闻、赛事讨论、社区交流、羽球装备</text>
    </view>

    <swiper v-if="banners.length" class="banner" indicator-dots autoplay circular>
      <swiper-item v-for="item in banners" :key="item.id">
        <view class="banner-item" @tap="openBanner(item)">
          <image class="banner-img" :src="item.imageUrl" mode="aspectFill" />
          <view class="banner-caption">{{ item.title }}</view>
        </view>
      </swiper-item>
    </swiper>

    <view class="category-row">
      <view
        v-for="item in filters"
        :key="item.type"
        class="category"
        :class="{ active: activeFilter === item.type }"
        @tap="selectFilter(item)"
      >
        {{ item.label }}
      </view>
    </view>

    <view class="section-title">
      <text>首页内容</text>
      <text class="muted">{{ total ? `${total} 条` : '' }}</text>
    </view>

    <view v-if="loading" class="state-box">正在加载内容...</view>
    <view v-else-if="error" class="state-box" @tap="loadAll">{{ error }}，点击重试</view>
    <view v-else-if="!feedItems.length" class="state-box">暂无内容，切换分类看看</view>
    <view v-else class="post-list">
      <feed-card
        v-for="item in feedItems"
        :key="item.feedKey"
        :item="item"
      />
    </view>

    <view v-if="feedItems.length" class="load-more muted">
      {{ hasMore ? '上拉加载更多' : '没有更多了' }}
    </view>
    <app-tab-bar :selected="0" />
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app';
import AppTabBar from '../../components/app-tab-bar/app-tab-bar.vue';
import FeedCard from '../../components/feed-card/feed-card.vue';
import { api } from '../../utils/api';

const baseFilters = [
  { type: 'all', label: '全部' },
  { type: 'news', label: '外部新闻' }
];

const banners = ref([]);
const filters = ref(baseFilters);
const activeFilter = ref('all');
const feedItems = ref([]);
const page = ref(1);
const pageSize = 8;
const total = ref(0);
const hasMore = ref(true);
const loading = ref(false);
const loadingMore = ref(false);
const error = ref('');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.records || data?.list || [];
}

function totalOf(data, fallback) {
  return Number(data?.total || data?.totalCount || data?.count || fallback || 0);
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
    categoryName: item.categoryName || '外部新闻',
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

async function loadAll() {
  loading.value = true;
  error.value = '';
  page.value = 1;
  hasMore.value = true;
  try {
    const [bannerData, topics] = await Promise.all([
      api.getBanners().catch(() => []),
      api.getTopics()
    ]);
    banners.value = itemsOf(bannerData);
    filters.value = baseFilters.concat(itemsOf(topics).map((item) => ({
      type: `topic-${item.id}`,
      label: item.name,
      topicId: item.id
    })));
    await loadFeed(true);
  } catch (err) {
    error.value = err.msg || '加载失败，请确认后端已启动';
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}

async function loadFeed(reset) {
  if (!reset) loadingMore.value = true;
  const currentPage = reset ? 1 : page.value;
  const filter = filters.value.find((item) => item.type === activeFilter.value) || filters.value[0];
  const params = { page: currentPage, pageSize, sort: 'latest' };

  try {
    let nextItems = [];
    let nextTotal = 0;
    if (filter.type === 'news') {
      const data = await api.getNews(params);
      nextItems = itemsOf(data).map(asNewsItem);
      nextTotal = totalOf(data, nextItems.length);
    } else if (filter.topicId) {
      const data = await api.getPosts({ ...params, topicId: filter.topicId });
      nextItems = itemsOf(data).map(asPostItem);
      nextTotal = totalOf(data, nextItems.length);
    } else {
      const [news, posts] = await Promise.all([api.getNews(params), api.getPosts(params)]);
      nextItems = itemsOf(news)
        .map(asNewsItem)
        .concat(itemsOf(posts).map(asPostItem))
        .sort((left, right) => right.sortTime - left.sortTime);
      nextTotal = totalOf(news, itemsOf(news).length) + totalOf(posts, itemsOf(posts).length);
    }
    feedItems.value = reset ? nextItems : feedItems.value.concat(nextItems);
    total.value = reset ? nextTotal : Math.max(total.value, feedItems.value.length);
    page.value = currentPage + 1;
    hasMore.value = nextItems.length >= pageSize;
  } catch (err) {
    error.value = err.msg || '加载失败，请确认后端已启动';
  } finally {
    loadingMore.value = false;
  }
}

function selectFilter(item) {
  activeFilter.value = item.type;
  page.value = 1;
  hasMore.value = true;
  loadFeed(true);
}

function openBanner(item) {
  const type = String(item.linkType || item.targetType || '').toUpperCase();
  const target = item.linkTarget || item.targetId || item.newsId || item.postId || item.id;
  if (!target) return;
  if (type === 'PAGE') {
    uni.navigateTo({ url: normalizePageUrl(target) });
    return;
  }
  const url = type === 'POST' ? `/pages/post/detail?id=${target}` : `/pages/news/detail?id=${target}`;
  uni.navigateTo({ url });
}

function normalizePageUrl(url) {
  return String(url).replace(/\/index$/, '');
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/index' });
}

onLoad(loadAll);
onShow(() => {
  if (uni.hideTabBar) {
    uni.hideTabBar({ animation: false });
  }
});
onPullDownRefresh(loadAll);
onReachBottom(() => {
  if (hasMore.value && !loadingMore.value && !loading.value) {
    loadFeed(false);
  }
});
</script>

<style scoped>
.search-entry {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 22rpx;
  padding: 18rpx 22rpx;
  border: 2rpx solid #172117;
  border-radius: 14rpx;
  background: #ffffff;
  color: #65705f;
  font-size: 26rpx;
}

.search-icon {
  width: 34rpx;
  color: #172117;
  font-size: 34rpx;
  line-height: 1;
}

.search-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner {
  height: 310rpx;
  margin-top: 20rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  overflow: hidden;
  background: #dff5c9;
}

.banner-item,
.banner-img {
  width: 100%;
  height: 100%;
}

.banner-item {
  position: relative;
}

.banner-caption {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  min-height: 68rpx;
  padding: 12rpx 18rpx;
  background: rgba(23, 33, 23, 0.78);
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1.35;
}

.category-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6rpx;
  margin-top: 18rpx;
}

.category {
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

.category.active {
  background: #172117;
  color: #ffffff;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.load-more {
  padding: 30rpx 0 8rpx;
  text-align: center;
}
</style>
