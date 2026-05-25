<script setup lang="ts">
import { ref } from 'vue';
import DashboardView from './views/DashboardView.vue';
import LoginView from './views/LoginView.vue';
import { loadToken } from './stores/session';

const authenticated = ref(Boolean(loadToken()));
const authError = ref('');

function handleLogin(): void {
  authError.value = '';
  authenticated.value = true;
}

function handleLogout(reason?: string): void {
  authenticated.value = false;
  authError.value = reason ?? '';
}
</script>

<template>
  <DashboardView v-if="authenticated" @logout="handleLogout" />
  <LoginView v-else :initial-error="authError" @login="handleLogin" />
</template>
