<script setup lang="ts">
import { ref, watch } from 'vue';
import { adminLogin } from '../api/auth';

const props = defineProps<{
  initialError?: string;
}>();

const emit = defineEmits<{
  login: [];
}>();

const username = ref('');
const password = ref('');
const error = ref(props.initialError ?? '');
const submitting = ref(false);

watch(() => props.initialError, (next) => {
  error.value = next ?? '';
});

async function submit(): Promise<void> {
  try {
    submitting.value = true;
    error.value = '';
    await adminLogin({
      username: username.value.trim(),
      password: password.value
    });
    emit('login');
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败';
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-visual">
      <span class="brand-mark">羽</span>
      <p class="eyebrow">web-admin</p>
      <h1>羽球在线管理端</h1>
      <p>使用唯一管理员账号登录后台，登录成功后由后端签发管理员 Token。当前版本只提供新闻查看与删除/下架能力。</p>
    </section>

    <form class="login-card" @submit.prevent="submit">
      <h2>管理员登录</h2>
      <label for="admin-username">账号</label>
      <input
        id="admin-username"
        v-model="username"
        type="text"
        autocomplete="username"
        placeholder="请输入管理员账号"
        required
      />
      <label for="admin-password">密码</label>
      <input
        id="admin-password"
        v-model="password"
        type="password"
        autocomplete="current-password"
        placeholder="请输入管理员密码"
        required
      />
      <p v-if="error" class="form-error">{{ error }}</p>
      <button type="submit" class="primary-button" :disabled="submitting">
        {{ submitting ? '登录中...' : '进入管理台' }}
      </button>
      <p class="form-hint">前端不注册管理员账号；若后端返回 401/403，会清除本地登录状态。</p>
    </form>
  </main>
</template>
