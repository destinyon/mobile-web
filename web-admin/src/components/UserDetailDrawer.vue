<script setup lang="ts">
import { computed } from 'vue';
import { X } from 'lucide-vue-next';
import type { AdminUserDetail } from '../types/api';

const props = defineProps<{
  detail: AdminUserDetail | null;
  loading?: boolean;
  error?: string;
}>();

defineEmits<{
  close: [];
}>();

const initials = computed(() => props.detail?.nickname?.slice(0, 1) || '用');

function formatDate(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<template>
  <div v-if="detail || loading || error" class="drawer-backdrop" @click.self="$emit('close')">
    <aside class="detail-drawer user-detail-drawer">
      <header>
        <div>
          <p class="eyebrow">用户详情</p>
          <h2>{{ detail?.nickname || '加载中' }}</h2>
        </div>
        <button type="button" class="icon-button" title="关闭" aria-label="关闭详情" @click="$emit('close')">
          <X :size="18" />
        </button>
      </header>

      <div v-if="loading" class="drawer-state">加载中</div>
      <div v-else-if="error" class="drawer-state error-state">{{ error }}</div>
      <article v-else-if="detail" class="user-detail-content">
        <section class="user-hero">
          <img v-if="detail.avatarUrl" :src="detail.avatarUrl" alt="" />
          <span v-else>{{ initials }}</span>
          <div>
            <strong>{{ detail.nickname }}</strong>
            <small>ID {{ detail.id }} / {{ detail.role }} / {{ detail.status }}</small>
          </div>
        </section>

        <dl class="detail-meta">
          <div><dt>手机号</dt><dd>{{ detail.phone || '-' }}</dd></div>
          <div><dt>性别</dt><dd>{{ detail.gender || '-' }}</dd></div>
          <div><dt>年龄</dt><dd>{{ detail.age ?? '-' }}</dd></div>
          <div><dt>球龄</dt><dd>{{ detail.playYears ?? '-' }}</dd></div>
          <div><dt>创建时间</dt><dd>{{ formatDate(detail.createdAt) }}</dd></div>
          <div><dt>更新时间</dt><dd>{{ formatDate(detail.updatedAt) }}</dd></div>
        </dl>

        <section class="user-activity-grid">
          <div><span>发帖</span><strong>{{ detail.postCount }}</strong></div>
          <div><span>评论</span><strong>{{ detail.commentCount }}</strong></div>
          <div><span>收藏</span><strong>{{ detail.favoriteCount }}</strong></div>
          <div><span>点赞</span><strong>{{ detail.likeCount }}</strong></div>
          <div><span>浏览记录</span><strong>{{ detail.browseCount }}</strong></div>
        </section>
      </article>
    </aside>
  </div>
</template>
