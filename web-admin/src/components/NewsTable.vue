<script setup lang="ts">
import { Eye, Trash2 } from 'lucide-vue-next';
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
  <section class="table-panel news-table-panel">
    <table class="admin-table news-table">
      <thead>
        <tr>
          <th>新闻</th>
          <th>分类</th>
          <th>作者</th>
          <th>浏览</th>
          <th>互动</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td colspan="7" class="table-state">加载中</td>
        </tr>
        <tr v-else-if="items.length === 0">
          <td colspan="7" class="table-state">暂无新闻</td>
        </tr>
        <tr v-for="item in items" v-else :key="item.id">
          <td class="title-cell">
            <img v-if="item.coverUrl" :src="item.coverUrl" alt="" />
            <div>
              <strong>{{ item.title }}</strong>
              <span>ID {{ item.id }}</span>
            </div>
          </td>
          <td><span class="category-pill">{{ item.categoryName || '未分类' }}</span></td>
          <td>{{ item.author || '-' }}</td>
          <td>{{ item.viewCount }}</td>
          <td>
            <div class="count-stack">
              <span>赞 {{ item.likeCount }}</span>
              <span>藏 {{ item.favoriteCount }}</span>
              <span>评 {{ item.commentCount }}</span>
            </div>
          </td>
          <td>{{ formatDate(item.updatedAt) }}</td>
          <td>
            <div class="row-actions">
              <button type="button" class="icon-action" title="查看详情" @click="$emit('view', item)">
                <Eye :size="16" />
              </button>
              <button type="button" class="icon-action danger" title="下架新闻" @click="$emit('delete', item)">
                <Trash2 :size="16" />
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
