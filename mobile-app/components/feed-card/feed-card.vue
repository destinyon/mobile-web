<template>
  <view class="feed-card" @tap="handleTap">
    <image
      v-if="coverUrl"
      class="cover"
      mode="aspectFill"
      :src="coverUrl"
    />
    <view v-else class="cover cover-placeholder">羽</view>
    <view class="feed-body">
      <view class="feed-head">
        <text class="topic">{{ categoryName }}</text>
        <text class="status">{{ statusText }}</text>
      </view>
      <rich-text
        v-if="item.highlightedTitle"
        class="title"
        :nodes="item.highlightedTitle"
      />
      <text v-else class="title">{{ item.title }}</text>
      <text v-if="summary" class="summary">{{ summary }}</text>
      <view class="meta">
        <text>浏览 {{ item.viewCount || 0 }}</text>
        <text>点赞 {{ item.likeCount || 0 }}</text>
        <text>收藏 {{ item.favoriteCount || 0 }}</text>
        <text>{{ item.updatedAt || '' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  item: {
    type: Object,
    default: () => ({})
  }
});

const emit = defineEmits(['cardtap']);

const coverUrl = computed(() => props.item.coverUrl || props.item.imageUrl || props.item.image || props.item.cover || '');
const categoryName = computed(() => props.item.categoryName || props.item.topicName || (props.item.feedType === 'NEWS' ? '外部新闻' : '球友社区'));
const statusText = computed(() => (props.item.status === 'PUBLISHED' ? '已发布' : props.item.status || ''));
const summary = computed(() => props.item.summary || props.item.content || '');

function handleTap() {
  const id = props.item.targetId || props.item.postId || props.item.newsId || props.item.linkTarget || props.item.id;
  if (id) {
    const type = String(props.item.feedType || props.item.targetType || props.item.linkType || '').toUpperCase();
    const url = type === 'NEWS' ? `/pages/news/detail?id=${id}` : `/pages/post/detail?id=${id}`;
    uni.navigateTo({ url });
  }
  emit('cardtap', props.item);
}
</script>

<style scoped>
.feed-card {
  display: flex;
  gap: 16rpx;
  padding: 16rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #ffffff;
  box-shadow: 6rpx 6rpx 0 rgba(23, 33, 23, 0.16);
}

.cover {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 176rpx;
  width: 176rpx;
  height: 136rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  background: #e7f6d8;
}

.cover-placeholder {
  color: #172117;
  font-size: 46rpx;
  font-weight: 900;
}

.feed-body {
  flex: 1;
  min-width: 0;
}

.feed-head,
.meta {
  display: flex;
  align-items: center;
  gap: 18rpx;
  color: #65705f;
  font-size: 23rpx;
}

.topic {
  max-width: 210rpx;
  padding: 4rpx 10rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  overflow: hidden;
  background: #baf279;
  color: #172117;
  font-size: 21rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  margin-left: auto;
}

.title {
  display: -webkit-box;
  margin-top: 10rpx;
  overflow: hidden;
  color: #172117;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.summary {
  display: -webkit-box;
  margin-top: 8rpx;
  overflow: hidden;
  color: #65705f;
  font-size: 23rpx;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.meta {
  gap: 8rpx 14rpx;
  flex-wrap: wrap;
  margin-top: 12rpx;
  font-size: 21rpx;
}
</style>
