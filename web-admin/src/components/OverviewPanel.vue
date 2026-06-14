<script setup lang="ts">
import { computed } from 'vue';
import type { AdminCategoryStat, AdminSummary } from '../types/api';

const props = defineProps<{
  summary: AdminSummary | null;
  loading?: boolean;
}>();

const categoryStats = computed(() => props.summary?.categoryStats ?? []);
const topCategories = computed(() => [...categoryStats.value]
  .sort((left, right) => right.contentCount - left.contentCount)
  .slice(0, 3));
const totalNews = computed(() => props.summary?.newsCount ?? sumBy(categoryStats.value, 'newsCount'));
const totalPosts = computed(() => props.summary?.postCount ?? sumBy(categoryStats.value, 'postCount'));
const totalContent = computed(() => totalNews.value + totalPosts.value);
const totalEngagement = computed(() =>
  (props.summary?.totalLikes ?? 0) + (props.summary?.totalFavorites ?? 0) + (props.summary?.commentCount ?? 0)
);
const maxContent = computed(() => maxValue(topCategories.value.map((stat) => stat.contentCount)));
const maxHeat = computed(() => maxValue(topCategories.value.map((stat) => heatValue(stat))));
const contentGradient = computed(() => {
  if (totalContent.value <= 0) {
    return '#dde7e1 0 100%';
  }
  const newsEnd = percent(totalNews.value, totalContent.value);
  return `#11684f 0 ${newsEnd}%, #f2b84b ${newsEnd}% 100%`;
});

function maxValue(values: number[]): number {
  return Math.max(1, ...values);
}

function sumBy(stats: AdminCategoryStat[], key: keyof Pick<AdminCategoryStat, 'newsCount' | 'postCount'>): number {
  return stats.reduce((sum, stat) => sum + stat[key], 0);
}

function heatValue(stat: AdminCategoryStat): number {
  return stat.totalLikes + stat.totalFavorites + stat.totalViews;
}

function percent(value: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, value / total * 100));
}

function categoryPercent(stat: AdminCategoryStat): number {
  return percent(stat.contentCount, maxContent.value);
}

function formatPercent(value: number, total: number): string {
  return `${Math.round(percent(value, total))}%`;
}
</script>

<template>
  <section class="overview-dashboard">
    <article class="chart-panel overview-hero-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">内容结构</p>
          <h2>新闻与帖子总览</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else class="content-mix-layout">
        <div class="content-donut" :style="{ '--content-gradient': contentGradient }">
          <span>{{ totalContent }} 篇</span>
        </div>
        <div class="content-mix-copy">
          <div class="mix-total">
            <strong>{{ totalEngagement }}</strong>
            <span>赞藏评互动</span>
          </div>
          <div class="mix-legend">
            <div class="legend-row">
              <span><i class="news-dot"></i>新闻</span>
              <strong>{{ totalNews }} 篇</strong>
              <small>{{ formatPercent(totalNews, totalContent) }}</small>
            </div>
            <div class="legend-row">
              <span><i class="post-dot"></i>帖子</span>
              <strong>{{ totalPosts }} 篇</strong>
              <small>{{ formatPercent(totalPosts, totalContent) }}</small>
            </div>
          </div>
        </div>
      </div>
    </article>

    <article class="chart-panel category-split-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">三类统计</p>
          <h2>分类新闻/帖子分布</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else-if="topCategories.length === 0" class="panel-state">暂无分类数据</div>
      <div v-else class="category-card-grid">
        <div
          v-for="(stat, index) in topCategories"
          :key="stat.categoryId"
          class="category-data-card"
          :class="`category-tone-${index}`"
        >
          <div class="category-card-head">
            <span>{{ stat.categoryName }}</span>
            <strong>{{ stat.contentCount }}</strong>
          </div>
          <div class="category-progress" :style="{ '--category-size': `${categoryPercent(stat)}%` }">
            <i class="news-segment" :style="{ width: formatPercent(stat.newsCount, stat.contentCount) }"></i>
            <i class="post-segment" :style="{ width: formatPercent(stat.postCount, stat.contentCount) }"></i>
          </div>
          <div class="category-card-meta">
            <span>新闻 {{ stat.newsCount }}</span>
            <span>帖子 {{ stat.postCount }}</span>
            <span>浏览 {{ stat.totalViews }}</span>
          </div>
        </div>
      </div>
    </article>

    <article class="chart-panel engagement-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">互动热力</p>
          <h2>三类分类热度</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else-if="topCategories.length === 0" class="panel-state">暂无互动数据</div>
      <div v-else class="heat-chart">
        <div
          v-for="stat in topCategories"
          :key="stat.categoryId"
          class="heat-cell"
          :style="{ '--heat': `${Math.max(10, percent(heatValue(stat), maxHeat))}%` }"
        >
          <span>{{ stat.categoryName }}</span>
          <strong>{{ heatValue(stat) }}</strong>
          <small>浏览/赞/藏合计</small>
        </div>
      </div>
    </article>
  </section>
</template>
