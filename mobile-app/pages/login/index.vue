<template>
  <view class="page login-page">
    <view class="brand-block">
      <view class="logo">羽</view>
      <text class="brand-title">羽球在线</text>
      <text class="brand-subtitle">邮箱验证码登录，同步收藏、评论和发布内容</text>
    </view>

    <view class="login-card card">
      <text class="form-title">邮箱登录</text>
      <input
        v-model.trim="email"
        class="field"
        type="text"
        maxlength="120"
        placeholder="请输入邮箱"
      />
      <view class="code-row">
        <input v-model.trim="code" class="field code-input" type="number" maxlength="8" placeholder="验证码" />
        <button class="ghost-btn code-btn" :disabled="sending || countdown > 0" @tap="sendCode">
          {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
        </button>
      </view>
      <button class="primary-btn login-btn" :loading="loggingIn" @tap="login">登录 / 注册</button>
      <text v-if="debugCode" class="debug">调试验证码：{{ debugCode }}</text>
      <text class="hint">登录即代表你同意使用邮箱完成账号创建和身份校验。</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onUnmounted } from 'vue';
import { api } from '../../utils/api';
import { saveLogin } from '../../utils/auth';

const email = ref('');
const code = ref('');
const debugCode = ref('');
const sending = ref(false);
const loggingIn = ref(false);
const countdown = ref(0);
let timer = null;

function validEmail() {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    uni.showToast({ title: '请输入正确邮箱', icon: 'none' });
    return false;
  }
  return true;
}

function startCountdown(seconds) {
  countdown.value = seconds;
  clearInterval(timer);
  timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      clearInterval(timer);
      timer = null;
    }
  }, 1000);
}

async function sendCode() {
  if (!validEmail()) return;
  sending.value = true;
  try {
    const result = await api.sendEmailCode(email.value);
    debugCode.value = result.debugCode || '';
    startCountdown(Number(result.interval || 60));
    uni.showToast({ title: '验证码已发送', icon: 'success' });
  } catch (error) {
    if (error.statusCode === 429) {
      startCountdown(60);
    }
    uni.showToast({ title: error.msg || '发送失败', icon: 'none' });
  } finally {
    sending.value = false;
  }
}

async function login() {
  if (!validEmail()) return;
  if (!/^\d{4,8}$/.test(code.value)) {
    uni.showToast({ title: '请输入验证码', icon: 'none' });
    return;
  }
  loggingIn.value = true;
  try {
    const result = await api.emailLogin(email.value, code.value);
    saveLogin(result);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/mine/index' }), 400);
  } catch (error) {
    uni.showToast({ title: error.msg || '登录失败', icon: 'none' });
  } finally {
    loggingIn.value = false;
  }
}

onUnmounted(() => clearInterval(timer));
</script>

<style scoped>
.login-page {
  padding-top: 72rpx;
}

.brand-block {
  margin-bottom: 42rpx;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  margin-bottom: 18rpx;
  border: 2rpx solid #172117;
  border-radius: 10rpx;
  background: #baf279;
  color: #172117;
  font-size: 58rpx;
  font-weight: 900;
  box-shadow: 8rpx 8rpx 0 rgba(23, 33, 23, 0.16);
}

.login-card {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  padding: 24rpx;
}

.form-title {
  font-size: 32rpx;
  font-weight: 900;
}

.code-row {
  display: flex;
  gap: 14rpx;
}

.code-input {
  flex: 1;
}

.code-btn {
  flex: 0 0 218rpx;
  height: 88rpx;
  font-size: 25rpx;
}

.login-btn {
  height: 92rpx;
}

.debug {
  color: #a24f18;
  font-size: 24rpx;
}

.hint {
  color: #65705f;
  font-size: 22rpx;
  line-height: 1.5;
}
</style>
