<script setup lang="ts">
import type { AdminSummary } from '../types/api';

defineProps<{
  summary: AdminSummary | null;
  loading?: boolean;
}>();

function maxValue(values: number[]): number {
  return Math.max(1, ...values);
}

function heatValue(stat: { totalLikes: number; totalFavorites: number }): number {
  return stat.totalLikes + stat.totalFavorites;
}
</script>

<template>
  <section class="overview-grid expanded">
    <article class="chart-panel category-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">分类分布</p>
          <h2>文章与互动</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else-if="!summary || summary.categoryStats.length === 0" class="panel-state">暂无分类数据</div>
      <div v-else class="bar-chart">
        <div
          v-for="stat in summary.categoryStats"
          :key="stat.categoryId"
          class="bar-row"
        >
          <span class="bar-label">{{ stat.categoryName }}</span>
          <div class="bar-track">
            <i :style="{ width: `${Math.max(7, stat.newsCount / maxValue(summary.categoryStats.map(item => item.newsCount)) * 100)}%` }"></i>
          </div>
          <strong>{{ stat.newsCount }}</strong>
        </div>
      </div>
    </article>

    <article class="chart-panel interaction-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">互动结构</p>
          <h2>分类热度</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else-if="!summary || summary.categoryStats.length === 0" class="panel-state">暂无分类数据</div>
      <div v-else class="heat-chart">
        <div
          v-for="stat in summary.categoryStats"
          :key="stat.categoryId"
          class="heat-cell"
          :style="{
            '--heat': `${Math.max(8, heatValue(stat) / maxValue(summary.categoryStats.map(item => heatValue(item))) * 100)}%`
          }"
        >
          <span>{{ stat.categoryName }}</span>
          <strong>{{ heatValue(stat) }}</strong>
          <small>赞藏合计</small>
        </div>
      </div>
    </article>

    <article class="chart-panel mix-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">内容状态</p>
          <h2>发布结构</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else class="donut-wrap">
        <div
          class="donut"
          :style="{
            '--published': `${summary ? Math.round(summary.publishedNewsCount / Math.max(1, summary.newsCount) * 100) : 0}%`
          }"
        >
          <span>{{ summary?.newsCount ?? 0 }} 篇</span>
        </div>
        <div class="legend-list">
          <span><i class="legend-published"></i>已发布 {{ summary?.publishedNewsCount ?? 0 }} 篇</span>
          <span><i class="legend-offline"></i>已下架 {{ summary?.offlineNewsCount ?? 0 }} 篇</span>
          <span><i class="legend-post"></i>社区帖 {{ summary?.postCount ?? 0 }} 篇</span>
        </div>
      </div>
    </article>
  </section>
</template>
