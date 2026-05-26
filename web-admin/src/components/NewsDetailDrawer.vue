<script setup lang="ts">
import { computed } from 'vue';
import { X } from 'lucide-vue-next';
import type { NewsDetail } from '../types/api';

const props = defineProps<{
  detail: NewsDetail | null;
  loading?: boolean;
  error?: string;
}>();

defineEmits<{
  close: [];
}>();

function formatDate(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

const safeContent = computed(() => {
  if (!props.detail?.content) {
    return '';
  }
  return sanitizeHtml(props.detail.content);
});

function sanitizeHtml(html: string): string {
  const template = document.createElement('template');
  template.innerHTML = html;
  template.content.querySelectorAll('script, iframe, object, embed, link, style').forEach((node) => node.remove());
  template.content.querySelectorAll('*').forEach((node) => {
    Array.from(node.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase();
      const value = attr.value.trim().toLowerCase();
      if (name.startsWith('on') || (['href', 'src'].includes(name) && value.startsWith('javascript:'))) {
        node.removeAttribute(attr.name);
      }
    });
  });
  return template.innerHTML;
}
</script>

<template>
  <div v-if="detail || loading || error" class="drawer-backdrop" @click.self="$emit('close')">
    <aside class="detail-drawer">
      <header>
        <div>
          <p class="eyebrow">文章详情</p>
          <h2>{{ detail?.title || '加载中' }}</h2>
        </div>
        <button type="button" class="icon-button" title="关闭" aria-label="关闭详情" @click="$emit('close')">
          <X :size="18" />
        </button>
      </header>

      <div v-if="loading" class="drawer-state">加载中</div>
      <div v-else-if="error" class="drawer-state error-state">{{ error }}</div>
      <article v-else-if="detail" class="detail-content">
        <dl class="detail-meta">
          <div><dt>分类</dt><dd>{{ detail.categoryName || '-' }}</dd></div>
          <div><dt>作者</dt><dd>{{ detail.author || '-' }}</dd></div>
          <div><dt>更新时间</dt><dd>{{ formatDate(detail.updatedAt) }}</dd></div>
          <div><dt>互动</dt><dd>浏览 {{ detail.viewCount }} / 赞 {{ detail.likeCount }} / 藏 {{ detail.favoriteCount }} / 评 {{ detail.commentCount }}</dd></div>
        </dl>
        <section class="html-preview" v-html="safeContent"></section>
      </article>
    </aside>
  </div>
</template>
