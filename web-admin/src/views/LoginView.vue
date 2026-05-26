<script setup lang="ts">
import { ref, watch } from 'vue';
import { ArrowRight, ShieldCheck } from 'lucide-vue-next';
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
      <div class="login-brand">
        <span class="brand-mark">羽</span>
        <div>
          <p class="eyebrow">badminton operations</p>
          <h1>羽球在线管理台</h1>
        </div>
      </div>
      <div class="court-lines" aria-hidden="true">
        <i></i>
        <i></i>
        <i></i>
      </div>
      <div class="login-stats">
        <span>News</span>
        <span>Users</span>
        <span>Moderation</span>
      </div>
    </section>

    <form class="login-card" @submit.prevent="submit">
      <div class="login-card-heading">
        <ShieldCheck :size="22" />
        <h2>管理员登录</h2>
      </div>
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
      <button type="submit" class="primary-button login-submit" :disabled="submitting">
        <span>{{ submitting ? '登录中' : '进入管理台' }}</span>
        <ArrowRight :size="18" />
      </button>
    </form>
  </main>
</template>
