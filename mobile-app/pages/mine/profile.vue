<template>
  <view class="page profile-edit-page">
    <page-header title="个人资料" />

    <view v-if="loading" class="state-box">正在加载资料...</view>
    <view v-else class="profile-form card">
      <button class="avatar-edit" :loading="uploading" @tap="chooseAvatar">
        <view class="avatar-frame">
          <image v-if="form.avatarUrl" :src="form.avatarUrl" mode="aspectFill" />
          <view v-else class="avatar-placeholder">羽</view>
        </view>
        <view class="avatar-mark">✎</view>
      </button>

      <view class="field-label">昵称</view>
      <input v-model="form.nickname" class="field" maxlength="80" placeholder="填写昵称" />

      <view class="field-label">邮箱</view>
      <view class="field readonly-field">{{ form.email || '未绑定邮箱' }}</view>

      <view class="field-label">手机号</view>
      <input v-model="form.phone" class="field" maxlength="40" placeholder="可选" />

      <view class="field-grid">
        <view>
          <view class="field-label">年龄</view>
          <picker mode="selector" :range="ageOptions" @change="onPickerChange('age', ageOptions, $event)">
            <view class="field select-field">{{ form.age ? `${form.age} 岁` : '请选择' }}</view>
          </picker>
        </view>
        <view>
          <view class="field-label">球龄</view>
          <picker mode="selector" :range="playYearOptions" @change="onPickerChange('playYears', playYearOptions, $event)">
            <view class="field select-field">{{ form.playYears !== '' ? `${form.playYears} 年` : '请选择' }}</view>
          </picker>
        </view>
      </view>

      <view class="field-label">性别</view>
      <picker mode="selector" :range="genderOptions" @change="onPickerChange('gender', genderOptions, $event)">
        <view class="field select-field">{{ form.gender || '请选择' }}</view>
      </picker>

      <button class="primary-btn save-btn" :loading="saving" @tap="save">保存资料</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import PageHeader from '../../components/page-header/page-header.vue';
import { api, uploadFile, isUploadTooLarge, uploadTooLargeMessage } from '../../utils/api';
import { isAuthError } from '../../utils/auth';

const ageOptions = Array.from({ length: 63 }, (_, index) => String(index + 8));
const playYearOptions = Array.from({ length: 41 }, (_, index) => String(index));
const genderOptions = ['男', '女', '不透露'];

const form = reactive({
  nickname: '',
  avatarUrl: '',
  email: '',
  phone: '',
  age: '',
  playYears: '',
  gender: ''
});
const loading = ref(true);
const saving = ref(false);
const uploading = ref(false);

async function load() {
  loading.value = true;
  try {
    const profile = await api.getProfile();
    Object.assign(form, {
      nickname: profile.nickname || '',
      avatarUrl: profile.avatarUrl || '',
      email: profile.email || '',
      phone: profile.phone || '',
      age: profile.age || '',
      playYears: profile.playYears === 0 ? 0 : (profile.playYears || ''),
      gender: profile.gender || ''
    });
  } catch (err) {
    if (isAuthError(err)) {
      uni.navigateTo({ url: '/pages/login/index' });
      return;
    }
    uni.showToast({ title: err.msg || '资料加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function onPickerChange(key, options, event) {
  form[key] = options[Number(event.detail.value)] || '';
}

function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths && res.tempFilePaths[0];
      if (!path) return;
      const file = res.tempFiles && res.tempFiles[0];
      if (isUploadTooLarge(file)) {
        uni.showToast({ title: uploadTooLargeMessage(), icon: 'none' });
        return;
      }
      uploading.value = true;
      try {
        const result = await uploadFile(path);
        form.avatarUrl = result.url;
      } catch (err) {
        uni.showToast({ title: err.msg || '头像上传失败', icon: 'none' });
      } finally {
        uploading.value = false;
      }
    }
  });
}

async function save() {
  if (!form.nickname.trim()) {
    uni.showToast({ title: '请填写昵称', icon: 'none' });
    return;
  }
  saving.value = true;
  try {
    const profile = await api.updateProfile({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl,
      phone: form.phone,
      age: form.age === '' ? undefined : Number(form.age),
      playYears: form.playYears === '' ? undefined : Number(form.playYears),
      gender: form.gender
    });
    uni.setStorageSync('userInfo', profile);
    uni.showToast({ title: '已保存', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 600);
  } catch (err) {
    if (isAuthError(err)) {
      uni.navigateTo({ url: '/pages/login/index' });
      return;
    }
    uni.showToast({ title: err.msg || '保存失败', icon: 'none' });
  } finally {
    saving.value = false;
  }
}

onLoad(load);
</script>

<style scoped>
.profile-form {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding: 26rpx;
}

.avatar-edit {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: center;
  width: 146rpx;
  height: 146rpx;
  min-height: 146rpx;
  margin-bottom: 12rpx;
  padding: 0;
  border-radius: 50%;
  line-height: 1;
  overflow: visible;
}

.avatar-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 146rpx;
  width: 146rpx;
  height: 146rpx;
  aspect-ratio: 1 / 1;
  border: 2rpx solid #172117;
  border-radius: 50%;
  overflow: hidden;
  background: #dff5c9;
}

.avatar-frame image,
.avatar-placeholder {
  display: block;
  flex: 0 0 146rpx;
  width: 100%;
  height: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 50%;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #172117;
  font-size: 54rpx;
  font-weight: 900;
}

.avatar-mark {
  position: absolute;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46rpx;
  height: 46rpx;
  border: 2rpx solid #172117;
  border-radius: 50%;
  background: #baf279;
  color: #172117;
  font-size: 24rpx;
  font-weight: 900;
}

.field-label {
  color: #172117;
  font-size: 25rpx;
  font-weight: 900;
}

.select-field {
  color: #172117;
}

.readonly-field {
  background: #f4fee9;
  color: #65705f;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.save-btn {
  margin-top: 18rpx;
}
</style>
