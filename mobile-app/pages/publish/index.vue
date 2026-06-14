<template>
  <view class="page editor-shell with-tabbar">
    <view v-if="!loggedIn" class="login-needed card">
      <text class="login-title">登录后发布球友动态</text>
      <text class="login-desc">分享训练心得、约球信息或装备体验。</text>
      <button class="primary-btn" @tap="goLogin">邮箱登录</button>
    </view>

    <view v-else>
      <view class="brand-bar">
        <view class="brand-title"><text class="brand-mark">{{ mode === 'edit' ? '改' : '写' }}</text>{{ mode === 'edit' ? '编辑投稿' : '发布投稿' }}</view>
        <view class="draft-state">{{ mode === 'draft' ? (savingDraft ? '保存中' : '已暂存') : '作者编辑' }}</view>
      </view>

      <view class="form card">
        <view class="field-label">投稿类别</view>
        <view class="topic-grid">
          <view
            v-for="item in topics"
            :key="item.id"
            class="topic"
            :class="{ active: form.topicId === item.id }"
            @tap="selectTopic(item.id)"
          >
            <text class="topic-name">{{ item.name }}</text>
          </view>
        </view>

        <view class="field-label">标题</view>
        <input v-model="form.title" class="field title-field" placeholder="写一个清楚的投稿标题" maxlength="120" @input="scheduleDraftSave" />

        <view class="field-label">封面图</view>
        <view v-if="form.coverUrl" class="cover-preview">
          <image :src="form.coverUrl" mode="aspectFill" />
          <view class="icon-btn danger" @tap="removeCover">×</view>
        </view>
        <button v-else class="upload-box" :loading="uploadingCover" @tap="chooseCover">
          <text class="upload-icon">▧</text>
          <text>{{ uploadingCover ? '上传中' : '上传封面' }}</text>
        </button>

        <view class="field-label">文章内容</view>
        <textarea v-model="form.content" class="textarea content" placeholder="写下你的观点、经验或装备体验" maxlength="5000" @input="scheduleDraftSave" />

        <view class="field-label row-label">
          <text>上传配图</text>
          <button class="small-action" :loading="uploadingImage" @tap="chooseImages">＋ 图片 {{ form.images.length }}/9</button>
        </view>
        <view v-if="form.images.length" class="image-grid">
          <view v-for="(item, index) in form.images" :key="item.url || item" class="image-item">
            <image :src="item.url || item" mode="aspectFill" @tap="preview(item.url || item)" />
            <view class="image-order">{{ index + 1 }}</view>
            <view class="icon-mini danger" @tap.stop="removeImage(index)">×</view>
          </view>
        </view>
        <view v-else class="empty-inline">配图会回显在正文下方，长按图片可拖拽换位置</view>
      </view>

      <view class="bottom-actions">
        <button v-if="mode === 'draft'" class="secondary-btn" :loading="savingDraft" @tap="saveDraft">暂存草稿</button>
        <button class="primary-btn" :loading="submitting" @tap="submit">{{ mode === 'edit' ? '保存修改' : '发布投稿' }}</button>
      </view>
    </view>

    <app-tab-bar :selected="1" />
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { onShow, onUnload } from '@dcloudio/uni-app';
import AppTabBar from '../../components/app-tab-bar/app-tab-bar.vue';
import { api, uploadFile, isUploadTooLarge, uploadTooLargeMessage } from '../../utils/api';
import { getToken, isAuthError } from '../../utils/auth';

const emptyForm = {
  topicId: '',
  title: '',
  coverUrl: '',
  coverKey: '',
  content: '',
  images: []
};

const loggedIn = ref(false);
const mode = ref('draft');
const postId = ref('');
const topics = ref([]);
const savingDraft = ref(false);
const submitting = ref(false);
const uploadingCover = ref(false);
const uploadingImage = ref(false);
const form = reactive({ ...emptyForm });
let autosaveTimer = null;

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.records || data?.list || [];
}

function resetForm(keepTopic = true) {
  const topicId = keepTopic ? form.topicId : '';
  Object.assign(form, { ...emptyForm, topicId });
}

function fillFormFromPost(post) {
  Object.assign(form, {
    topicId: post.topicId || form.topicId,
    title: post.title || '',
    coverUrl: post.coverUrl || '',
    coverKey: '',
    content: post.content || '',
    images: (post.images || []).map((url) => ({ url, objectKey: '' }))
  });
}

async function loadTopics() {
  try {
    const data = await api.getTopics();
    topics.value = itemsOf(data);
    if (!form.topicId && topics.value[0]) {
      form.topicId = topics.value[0].id;
    }
  } catch (err) {
    uni.showToast({ title: err.msg || '话题加载失败', icon: 'none' });
  }
}

async function loadDraft() {
  try {
    const draft = await api.getPostDraft();
    if (!draft) return;
    Object.assign(form, {
      topicId: draft.topicId || form.topicId,
      title: draft.title === '未命名草稿' ? '' : (draft.title || ''),
      coverUrl: draft.coverUrl || '',
      coverKey: '',
      content: draft.content || '',
      images: (draft.images || []).map((url) => ({ url, objectKey: '' }))
    });
  } catch (err) {
    if (isAuthError(err)) return;
    uni.showToast({ title: err.msg || '草稿加载失败', icon: 'none' });
  }
}

async function loadEditingPost(nextPostId) {
  mode.value = 'edit';
  postId.value = String(nextPostId);
  clearTimeout(autosaveTimer);
  try {
    fillFormFromPost(await api.getPostDetail(nextPostId));
  } catch (err) {
    mode.value = 'draft';
    postId.value = '';
    uni.removeStorageSync('editingPostId');
    uni.showToast({ title: err.msg || '编辑内容加载失败', icon: 'none' });
  }
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' });
}

function selectTopic(id) {
  form.topicId = id;
  scheduleDraftSave();
}

function chooseMedia(count) {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles || [];
        const paths = res.tempFilePaths || [];
        resolve(paths.map((path, index) => ({ path, file: files[index] || {} })));
      },
      fail: reject
    });
  });
}

async function chooseCover() {
  try {
    const [picked] = await chooseMedia(1);
    if (!picked?.path) return;
    if (isUploadTooLarge(picked.file)) {
      uni.showToast({ title: uploadTooLargeMessage(), icon: 'none' });
      return;
    }
    uploadingCover.value = true;
    const result = await uploadFile(picked.path);
    form.coverUrl = result.url;
    form.coverKey = result.objectKey || '';
    scheduleDraftSave();
  } catch (err) {
    if (err?.errMsg?.includes('cancel')) return;
    uni.showToast({ title: err.msg || '封面上传失败', icon: 'none' });
  } finally {
    uploadingCover.value = false;
  }
}

async function chooseImages() {
  const remaining = 9 - form.images.length;
  if (remaining <= 0) {
    uni.showToast({ title: '最多上传 9 张配图', icon: 'none' });
    return;
  }
  try {
    const pickedFiles = await chooseMedia(remaining);
    if (!pickedFiles.length) return;
    const validFiles = pickedFiles.filter((item) => !isUploadTooLarge(item.file));
    if (validFiles.length !== pickedFiles.length) {
      uni.showToast({ title: uploadTooLargeMessage(), icon: 'none' });
    }
    if (!validFiles.length) return;
    uploadingImage.value = true;
    const uploads = [];
    for (const item of validFiles.slice(0, remaining)) {
      uploads.push(await uploadFile(item.path));
    }
    form.images = form.images.concat(uploads.map((item) => ({
      url: item.url,
      objectKey: item.objectKey || ''
    }))).slice(0, 9);
    scheduleDraftSave();
  } catch (err) {
    if (err?.errMsg?.includes('cancel')) return;
    uni.showToast({ title: err.msg || '配图上传失败', icon: 'none' });
  } finally {
    uploadingImage.value = false;
  }
}

function removeCover() {
  const key = form.coverKey;
  form.coverUrl = '';
  form.coverKey = '';
  if (key) api.deleteUpload(key).catch(() => {});
  scheduleDraftSave();
}

function removeImage(index) {
  const item = form.images[index];
  form.images.splice(index, 1);
  if (item?.objectKey) api.deleteUpload(item.objectKey).catch(() => {});
  scheduleDraftSave();
}

function preview(url) {
  uni.previewImage({ urls: form.images.map((item) => item.url || item), current: url });
}

function scheduleDraftSave() {
  if (mode.value !== 'draft' || !loggedIn.value) return;
  clearTimeout(autosaveTimer);
  autosaveTimer = setTimeout(() => saveDraft(false), 800);
}

async function saveDraft(showToast = true) {
  if (mode.value !== 'draft' || savingDraft.value || !form.topicId) return;
  savingDraft.value = true;
  try {
    await api.savePostDraft(payload(false));
    if (showToast) uni.showToast({ title: '草稿已暂存', icon: 'none' });
  } catch (err) {
    if (!isAuthError(err)) {
      uni.showToast({ title: err.msg || '草稿保存失败', icon: 'none' });
    }
  } finally {
    savingDraft.value = false;
  }
}

async function submit() {
  const data = payload(true);
  if (!data.topicId) {
    uni.showToast({ title: '请选择话题', icon: 'none' });
    return;
  }
  if (!data.title || !data.content) {
    uni.showToast({ title: '请填写标题和正文', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const post = mode.value === 'edit'
      ? await api.updatePost(postId.value, data)
      : await api.savePostDraft(data).then(() => api.publishPostDraft());
    uni.showToast({ title: mode.value === 'edit' ? '已更新' : '发布成功', icon: 'success' });
    resetForm(true);
    mode.value = 'draft';
    postId.value = '';
    uni.removeStorageSync('editingPostId');
    setTimeout(() => uni.navigateTo({ url: `/pages/post/detail?id=${post.id}` }), 600);
  } catch (err) {
    uni.showToast({ title: err.msg || '发布失败', icon: 'none' });
  } finally {
    submitting.value = false;
  }
}

function payload(strict) {
  const title = form.title.trim();
  const content = form.content.trim();
  return {
    topicId: form.topicId ? Number(form.topicId) : undefined,
    title: strict ? title : (title || '未命名草稿'),
    coverUrl: form.coverUrl.trim(),
    content,
    images: form.images.map((item) => item.url || item).filter(Boolean)
  };
}

onShow(async () => {
  if (uni.hideTabBar) {
    uni.hideTabBar({ animation: false });
  }
  loggedIn.value = !!getToken();
  if (!loggedIn.value) return;
  await loadTopics();
  const editingPostId = uni.getStorageSync('editingPostId');
  if (editingPostId) {
    await loadEditingPost(editingPostId);
    return;
  }
  if (mode.value === 'draft') {
    await loadDraft();
  }
});

onUnload(() => {
  clearTimeout(autosaveTimer);
  if (mode.value === 'draft' && loggedIn.value) {
    saveDraft(false);
  }
});
</script>

<style scoped>
.editor-shell {
  padding-bottom: 286rpx;
}

.login-needed {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  margin-top: 88rpx;
  padding: 28rpx;
}

.login-title {
  font-size: 38rpx;
  font-weight: 900;
}

.login-desc,
.draft-state {
  color: #65705f;
  font-size: 24rpx;
  font-weight: 700;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 34rpx;
  padding: 24rpx;
}

.field-label {
  color: #172117;
  font-size: 25rpx;
  font-weight: 900;
}

.row-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topic-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6rpx;
}

.topic {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 46rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  background: #ffffff;
  color: #172117;
}

.topic.active {
  background: #172117;
  color: #ffffff;
}

.topic-name {
  max-width: 100%;
  overflow: hidden;
  font-size: 19rpx;
  font-weight: 900;
  line-height: 1;
  text-overflow: clip;
  white-space: nowrap;
}

.title-field {
  font-size: 31rpx;
  font-weight: 800;
}

.content {
  min-height: 360rpx;
  line-height: 1.65;
}

.cover-preview {
  position: relative;
  height: 286rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  overflow: hidden;
}

.cover-preview image {
  width: 100%;
  height: 100%;
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 176rpx;
  border: 2rpx dashed #9baa93;
  border-radius: 8rpx;
  background: #fafff3;
  color: #3d493a;
  font-size: 27rpx;
  font-weight: 800;
}

.upload-icon {
  font-size: 40rpx;
  line-height: 1;
}

.small-action {
  min-width: 156rpx;
  min-height: 52rpx;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  background: #baf279;
  color: #172117;
  font-size: 24rpx;
  font-weight: 800;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10rpx;
}

.image-item {
  position: relative;
  height: 0;
  padding-bottom: 100%;
  border: 2rpx solid #172117;
  border-radius: 8rpx;
  overflow: hidden;
  background: #e7f6d8;
}

.image-item image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.image-order {
  position: absolute;
  left: 8rpx;
  top: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38rpx;
  height: 38rpx;
  border: 2rpx solid #172117;
  border-radius: 6rpx;
  background: #baf279;
  color: #172117;
  font-size: 22rpx;
  font-weight: 900;
}

.icon-btn,
.icon-mini {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #a13220;
  font-weight: 900;
  text-shadow: 0 1rpx 0 #ffffff, 1rpx 0 0 #ffffff, 0 -1rpx 0 #ffffff, -1rpx 0 0 #ffffff;
}

.icon-btn {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  width: 58rpx;
  height: 58rpx;
  font-size: 36rpx;
}

.icon-mini {
  position: absolute;
  right: 8rpx;
  top: 8rpx;
  width: 38rpx;
  height: 38rpx;
  font-size: 28rpx;
}

.empty-inline {
  padding: 24rpx;
  border: 2rpx dashed #d6dfcf;
  border-radius: 8rpx;
  color: #65705f;
  font-size: 24rpx;
  text-align: center;
}

.bottom-actions {
  position: fixed;
  right: 0;
  bottom: 112rpx;
  left: 0;
  z-index: 50;
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 14rpx;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  border-top: 2rpx solid #172117;
  background: #ffffff;
}

.bottom-actions .primary-btn,
.bottom-actions .secondary-btn {
  min-height: 78rpx;
  box-shadow: none;
}
</style>
