<script setup lang="ts">
import { computed } from 'vue';
import type { AdminSummary } from '../types/api';

const props = defineProps<{
  summary: AdminSummary | null;
  loading?: boolean;
}>();

const categoryStats = computed(() => props.summary?.categoryStats ?? []);
const totalCategoryNews = computed(() => categoryStats.value.reduce((sum, stat) => sum + stat.newsCount, 0));
const categoryGradient = computed(() => {
  if (totalCategoryNews.value <= 0) {
    return '#d9e4df 0 100%';
  }
  const colors = ['#13795b', '#c8f24e', '#f3b343', '#6b9fd8', '#9f7aea'];
  let start = 0;
  return categoryStats.value
    .map((stat, index) => {
      const span = stat.newsCount / totalCategoryNews.value * 100;
      const end = start + span;
      const segment = `${colors[index % colors.length]} ${start}% ${end}%`;
      start = end;
      return segment;
    })
    .join(', ');
});

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
      <div v-else-if="categoryStats.length === 0" class="panel-state">暂无分类数据</div>
      <div v-else class="bar-chart">
        <div
          v-for="stat in categoryStats"
          :key="stat.categoryId"
          class="bar-row"
        >
          <span class="bar-label">{{ stat.categoryName }}</span>
          <div class="bar-track">
            <i :style="{ width: `${Math.max(7, stat.newsCount / maxValue(categoryStats.map(item => item.newsCount)) * 100)}%` }"></i>
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
      <div v-else-if="categoryStats.length === 0" class="panel-state">暂无分类数据</div>
      <div v-else class="heat-chart">
        <div
          v-for="stat in categoryStats"
          :key="stat.categoryId"
          class="heat-cell"
          :style="{
            '--heat': `${Math.max(8, heatValue(stat) / maxValue(categoryStats.map(item => heatValue(item))) * 100)}%`
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
          <p class="eyebrow">讨论分类</p>
          <h2>分类占比</h2>
        </div>
      </div>
      <div v-if="loading" class="panel-state">加载中</div>
      <div v-else-if="categoryStats.length === 0" class="panel-state">暂无分类数据</div>
      <div v-else class="donut-wrap">
        <div
          class="donut"
          :style="{
            '--category-gradient': categoryGradient
          }"
        >
          <span>{{ totalCategoryNews }} 篇</span>
        </div>
        <div class="legend-list">
          <span
            v-for="(stat, index) in categoryStats"
            :key="stat.categoryId"
          >
            <i :class="`legend-category-${index % 5}`"></i>{{ stat.categoryName }} {{ stat.newsCount }} 篇
          </span>
        </div>
      </div>
    </article>
  </section>
</template>
