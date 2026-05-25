<script setup lang="ts">
import type { NewsSummary } from '../types/api';

defineProps<{
  items: NewsSummary[];
  loading?: boolean;
}>();

defineEmits<{
  view: [item: NewsSummary];
  delete: [item: NewsSummary];
}>();

function formatDate(value: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<template>
  <section class="news-table-panel">
    <table class="news-table">
      <thead>
        <tr>
          <th>新闻标题</th>
          <th>分类</th>
          <th>作者</th>
          <th>互动</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td colspan="6" class="table-state">正在加载真实后端新闻...</td>
        </tr>
        <tr v-else-if="items.length === 0">
          <td colspan="6" class="table-state">暂无可展示新闻</td>
        </tr>
        <tr v-for="item in items" v-else :key="item.id">
          <td class="title-cell">
            <img v-if="item.coverUrl" :src="item.coverUrl" alt="" />
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.summary || '暂无摘要' }}</p>
            </div>
          </td>
          <td>{{ item.categoryName || '-' }}</td>
          <td>{{ item.author || '-' }}</td>
          <td>
            <div class="count-stack">
              <span>浏览 {{ item.viewCount }}</span>
              <span>赞 {{ item.likeCount }} / 藏 {{ item.favoriteCount }} / 评 {{ item.commentCount }}</span>
            </div>
          </td>
          <td>{{ formatDate(item.updatedAt) }}</td>
          <td>
            <div class="row-actions">
              <button type="button" class="ghost-button" @click="$emit('view', item)">查看</button>
              <button type="button" class="danger-button" @click="$emit('delete', item)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
