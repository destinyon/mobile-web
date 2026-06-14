<script setup lang="ts">
import { Trophy } from 'lucide-vue-next';
import type { AdminNewsRankingItem } from '../types/api';

defineProps<{
  items: AdminNewsRankingItem[];
  loading?: boolean;
}>();

defineEmits<{
  view: [item: AdminNewsRankingItem];
}>();
</script>

<template>
  <section class="ranking-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">热度排行</p>
        <h2>新闻/帖子 Top 10</h2>
      </div>
      <Trophy :size="20" />
    </div>
    <div v-if="loading" class="panel-state">加载中</div>
    <ol v-else-if="items.length" class="ranking-list">
      <li v-for="(item, index) in items" :key="`${item.targetType}-${item.id}`">
        <button type="button" class="ranking-row-button" title="查看内容详情" @click="$emit('view', item)">
          <span class="rank-index">{{ index + 1 }}</span>
          <img v-if="item.coverUrl" :src="item.coverUrl" alt="" />
          <span v-else class="ranking-cover-empty"></span>
          <span class="ranking-title-wrap">
            <strong>{{ item.title }}</strong>
            <small>{{ item.targetType === 'POST' ? '帖子' : '新闻' }} / {{ item.categoryName || '未分类' }}</small>
          </span>
          <em>{{ item.heatScore }}</em>
        </button>
      </li>
    </ol>
    <div v-else class="panel-state">暂无排行数据</div>
  </section>
</template>
