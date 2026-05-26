<script setup lang="ts">
import { Eye } from 'lucide-vue-next';
import type { AdminUserItem } from '../types/api';

defineProps<{
  items: AdminUserItem[];
  loading?: boolean;
}>();

defineEmits<{
  view: [item: AdminUserItem];
}>();

function formatDate(value: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<template>
  <section class="table-panel user-table-panel">
    <table class="admin-table user-table">
      <thead>
        <tr>
          <th>用户</th>
          <th>角色</th>
          <th>状态</th>
          <th>内容</th>
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
          <td colspan="7" class="table-state">暂无用户</td>
        </tr>
        <tr v-for="item in items" v-else :key="item.id">
          <td>
            <div class="user-cell">
              <img v-if="item.avatarUrl" :src="item.avatarUrl" alt="" />
              <span v-else>{{ item.nickname.slice(0, 1) }}</span>
              <div>
                <strong>{{ item.nickname }}</strong>
                <small>{{ item.phone || '未填写手机号' }}</small>
              </div>
            </div>
          </td>
          <td><span class="status-pill neutral">{{ item.role }}</span></td>
          <td><span class="status-pill">{{ item.status }}</span></td>
          <td>{{ item.postCount }} 篇</td>
          <td>{{ item.commentCount }} 评 / {{ item.favoriteCount }} 藏</td>
          <td>{{ formatDate(item.updatedAt) }}</td>
          <td>
            <button type="button" class="icon-action" title="查看用户详情" @click="$emit('view', item)">
              <Eye :size="16" />
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
