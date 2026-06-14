<template>
  <view class="page detail-page">
    <view v-if="loading" class="state-box">正在加载详情...</view>
    <view v-else-if="error" class="state-box" @tap="loadDetail">{{ error }}，点击重试</view>
    <view v-else>
      <view class="brand-strip">
        <view class="back" @tap="goBack">‹</view>
        <text class="brand-title-mini">文章详情</text>
      </view>

      <view class="article">
        <view class="title">{{ detail.title }}</view>
        <view class="meta">
          <text>{{ detail.categoryName || detail.category || '羽球资讯' }}</text>
          <text>{{ detail.author || '羽球在线' }}</text>
          <text>◉ {{ detail.viewCount || 0 }}</text>
          <text>赞 {{ detail.likeCount || detail.likes || 0 }}</text>
        </view>
        <rich-text class="content" :nodes="contentNodes" />
      </view>

      <view v-if="replyTarget.id" class="reply-panel">
        <view class="reply-tip">
          <text>回复 {{ replyTarget.nickname || '球友' }}</text>
          <view class="reply-close" @tap="cancelReply">×</view>
        </view>
        <textarea v-model="replyContent" class="comment-input" placeholder="写下回复" maxlength="300" />
        <button class="primary-btn" :loading="submitting" @tap="submitReply">发表回复</button>
      </view>

      <view class="comment-header">
        <text>评论区：</text>
        <view class="comment-tools">
          <text class="comment-count">▣ {{ detail.commentCount || comments.length || 0 }}</text>
        </view>
      </view>
      <view v-if="comments.length" class="comments">
        <view v-for="item in comments" :key="item.id" class="comment-item">
          <view class="comment-head">
            <image v-if="item.avatarUrl" class="comment-avatar" :src="item.avatarUrl" mode="aspectFill" />
            <view v-else class="comment-avatar avatar-placeholder">羽</view>
            <view class="comment-main">
              <view class="comment-name-row">
                <text>{{ item.nickname || item.userName || '球友' }}</text>
                <view class="reply-action" @tap="startReply(item)">↩</view>
              </view>
              <view class="comment-content">{{ item.content }}</view>
            </view>
          </view>
          <view v-if="item.replies && item.replies.length" class="reply-list">
            <view v-for="reply in item.replies" :key="reply.id" class="reply-item">
              <text class="reply-name">{{ reply.nickname || '球友' }}：</text>{{ reply.content }}
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty-comments">暂无评论</view>
    </view>

    <view v-if="!loading && !error" class="comment-toolbar">
      <view class="toolbar-comment">▣</view>
      <input
        v-model="commentContent"
        class="toolbar-input"
        placeholder="不要忘记友善评论哦！"
        confirm-type="send"
        @confirm="submitComment"
      />
      <view class="toolbar-send" @tap="submitComment">↗</view>
      <view class="toolbar-action like-action" :class="{ active: detail.liked }" @tap="onLike">
        {{ detail.liked ? '♥' : '♡' }}
      </view>
      <view class="toolbar-action favorite-action" :class="{ active: detail.favorited || detail.favorite }" @tap="onFavorite">
        {{ detail.favorited || detail.favorite ? '★' : '☆' }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { api } from '../../utils/api';
import { getToken, isAuthError } from '../../utils/auth';

const id = ref('');
const detail = ref({});
const comments = ref([]);
const commentContent = ref('');
const replyContent = ref('');
const replyTarget = ref({});
const loading = ref(true);
const submitting = ref(false);
const error = ref('');

const contentNodes = computed(() => {
  const content = detail.value.content || detail.value.body || detail.value.summary || '';
  return String(content).replace(/\n/g, '<br/>');
});

async function loadDetail() {
  if (!id.value) {
    loading.value = false;
    error.value = '缺少新闻 ID';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await api.getNewsDetail(id.value);
    detail.value = data || {};
    comments.value = data?.comments || [];
  } catch (err) {
    error.value = err.msg || '加载失败';
  } finally {
    loading.value = false;
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

function onLike() {
  runProtected('like');
}

function onFavorite() {
  runProtected('favorite');
}

function startReply(item) {
  replyTarget.value = item || {};
  replyContent.value = '';
}

function cancelReply() {
  replyTarget.value = {};
  replyContent.value = '';
}

function submitComment() {
  if (!commentContent.value.trim()) {
    uni.showToast({ title: '请输入评论内容', icon: 'none' });
    return;
  }
  runProtected('comment');
}

function submitReply() {
  if (!replyContent.value.trim()) {
    uni.showToast({ title: '请输入回复内容', icon: 'none' });
    return;
  }
  runProtected('reply');
}

async function runProtected(action) {
  if (!getToken()) {
    uni.navigateTo({ url: '/pages/login/index' });
    return;
  }

  try {
    if (action === 'like') {
      patchActionState(await api.likeNews(id.value), 'like');
      return;
    }
    if (action === 'favorite') {
      const result = detail.value.favorited || detail.value.favorite
        ? await api.unfavoriteNews(id.value)
        : await api.favoriteNews(id.value);
      patchActionState(result, 'favorite');
      return;
    }

    submitting.value = true;
    await api.createComment({
      targetType: 'NEWS',
      targetId: Number(id.value),
      parentId: action === 'reply' ? replyTarget.value.id : undefined,
      content: action === 'reply' ? replyContent.value.trim() : commentContent.value.trim()
    });
    uni.showToast({ title: '已发布', icon: 'success' });
    commentContent.value = '';
    replyContent.value = '';
    replyTarget.value = {};
    await loadDetail();
  } catch (err) {
    if (isAuthError(err)) {
      uni.navigateTo({ url: '/pages/login/index' });
      return;
    }
    uni.showToast({ title: err.msg || '操作失败', icon: 'none' });
  } finally {
    submitting.value = false;
  }
}

function patchActionState(result, action) {
  detail.value = {
    ...detail.value,
    likeCount: result.likeCount,
    favoriteCount: result.favoriteCount,
    liked: result.liked,
    favorited: result.favorited,
    favorite: result.favorited
  };
  uni.showToast({
    title: action === 'like'
      ? (result.liked ? '已点赞' : '已取消点赞')
      : (result.favorited ? '已收藏' : '已取消收藏'),
    icon: 'none'
  });
}

onLoad((query) => {
  id.value = query.id || '';
  loadDetail();
});
</script>

<style scoped>
.detail-page {
  padding-bottom: 136rpx;
}

.brand-strip {
  display: flex;
  align-items: center;
  height: 64rpx;
  margin-bottom: 14rpx;
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

.article {
  padding: 18rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  background: #ffffff;
}

.title {
  color: #172117;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.35;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 22rpx;
  margin: 14rpx 0 16rpx;
  color: #65705f;
  font-size: 24rpx;
}

.content {
  color: #283227;
  font-size: 29rpx;
  line-height: 1.75;
}

.comment-input {
  width: 100%;
  min-height: 150rpx;
  padding: 18rpx;
  border: 2rpx solid #d6dfcf;
  border-radius: 6rpx;
  font-size: 28rpx;
}

.comments {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-bottom: 118rpx;
  border-top: 2rpx solid #d6dfcf;
}

.comment-item {
  padding: 14rpx 0;
  border-bottom: 2rpx solid #d6dfcf;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 14rpx;
  color: #172117;
  font-size: 26rpx;
  font-weight: 800;
}

.comment-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 64rpx;
  width: 64rpx;
  height: 64rpx;
  border: 2rpx solid #172117;
  border-radius: 50%;
  background: #dff5c9;
}

.avatar-placeholder {
  color: #172117;
  font-size: 26rpx;
  font-weight: 900;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reply-action,
.reply-close {
  color: #172117;
  font-size: 28rpx;
  font-weight: 900;
}

.comment-content {
  margin-top: 10rpx;
  color: #3d493a;
  font-size: 27rpx;
  line-height: 1.6;
}

.reply-panel {
  margin-top: 16rpx;
  margin-bottom: 16rpx;
  padding: 16rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #fafff3;
}

.reply-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
  color: #65705f;
  font-size: 24rpx;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin: 12rpx 0 0 78rpx;
  padding: 12rpx;
  border-radius: 8rpx;
  background: #f4fee9;
}

.reply-item {
  color: #3d493a;
  font-size: 25rpx;
  line-height: 1.5;
}

.reply-name {
  color: #172117;
  font-weight: 800;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 22rpx 0 8rpx;
  color: #21638a;
  font-size: 30rpx;
  font-weight: 900;
}

.comment-tools {
  display: flex;
  align-items: center;
  gap: 18rpx;
  color: #3d493a;
  font-size: 24rpx;
  font-weight: 700;
}

.empty-comments {
  margin-bottom: 118rpx;
  padding: 40rpx 0;
  color: #65705f;
  font-size: 25rpx;
  text-align: center;
}

.comment-toolbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 14rpx calc(12rpx + env(safe-area-inset-bottom));
  border-top: 2rpx solid #d6dfcf;
  background: #ffffff;
}

.toolbar-comment,
.toolbar-send,
.toolbar-action {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 48rpx;
  width: 48rpx;
  height: 48rpx;
  color: #172117;
  border-radius: 50%;
  font-size: 34rpx;
  font-weight: 900;
}

.toolbar-input {
  flex: 1;
  min-width: 0;
  height: 58rpx;
  padding: 0 18rpx;
  border: 2rpx solid #d6dfcf;
  border-radius: 6rpx;
  background: #fafff3;
  color: #172117;
  font-size: 25rpx;
}

.toolbar-send {
  color: #2b7f56;
}

.like-action.active {
  background: #ffe4df;
  color: #e0563f;
}

.favorite-action.active {
  background: #fff0b8;
  color: #d08b00;
}
</style>
