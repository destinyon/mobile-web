<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import MetricTile from '../components/MetricTile.vue';
import NewsDetailDrawer from '../components/NewsDetailDrawer.vue';
import NewsTable from '../components/NewsTable.vue';
import PaginationBar from '../components/PaginationBar.vue';
import { deleteNews, getNewsDetail, listAdminNews } from '../api/news';
import { ApiError } from '../api/client';
import { clearToken } from '../stores/session';
import type { NewsDetail, NewsSummary, PageResult } from '../types/api';

const emit = defineEmits<{
  logout: [reason?: string];
}>();

const keyword = ref('');
const appliedKeyword = ref('');
const page = ref(1);
const pageSize = 20;
const loading = ref(false);
const deletingId = ref<number | null>(null);
const error = ref('');
const result = ref<PageResult<NewsSummary>>({ items: [], total: 0, page: 1, pageSize });
const detail = ref<NewsDetail | null>(null);
const detailLoading = ref(false);
const detailError = ref('');

const totalViews = computed(() => result.value.items.reduce((sum, item) => sum + item.viewCount, 0));
const totalInteractions = computed(() => result.value.items.reduce(
  (sum, item) => sum + item.likeCount + item.favoriteCount + item.commentCount,
  0
));

onMounted(() => {
  void loadNews();
});

async function loadNews(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    result.value = await listAdminNews({
      page: page.value,
      pageSize,
      keyword: appliedKeyword.value
    });
  } catch (err) {
    handleError(err);
  } finally {
    loading.value = false;
  }
}

function search(): void {
  appliedKeyword.value = keyword.value.trim();
  page.value = 1;
  void loadNews();
}

function resetSearch(): void {
  keyword.value = '';
  appliedKeyword.value = '';
  page.value = 1;
  void loadNews();
}

function changePage(nextPage: number): void {
  page.value = nextPage;
  void loadNews();
}

async function openDetail(item: NewsSummary): Promise<void> {
  detail.value = null;
  detailError.value = '';
  detailLoading.value = true;
  try {
    detail.value = await getNewsDetail(item.id);
  } catch (err) {
    detailError.value = err instanceof Error ? err.message : '新闻详情加载失败';
  } finally {
    detailLoading.value = false;
  }
}

async function confirmDelete(item: NewsSummary): Promise<void> {
  const confirmed = window.confirm(`确认删除/下架这条新闻？\n\n${item.title}`);
  if (!confirmed) {
    return;
  }
  deletingId.value = item.id;
  error.value = '';
  try {
    await deleteNews(item.id);
    await loadNews();
  } catch (err) {
    handleError(err);
  } finally {
    deletingId.value = null;
  }
}

function handleError(err: unknown): void {
  error.value = err instanceof Error ? err.message : '请求失败';
  if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
    clearToken();
    emit('logout', `${error.value}，请重新登录管理员账号`);
  }
}

function logout(): void {
  clearToken();
  emit('logout');
}
</script>

<template>
  <main class="admin-layout">
    <aside class="nav-rail">
      <span class="rail-logo">羽</span>
      <button class="rail-item active" type="button" title="新闻管理">新闻</button>
      <button class="rail-item" type="button" title="退出登录" @click="logout">退出</button>
    </aside>

    <section class="workbench">
      <header class="topbar">
        <div>
          <p class="eyebrow">赛事运营中控台</p>
          <h1>新闻管理</h1>
        </div>
        <button class="ghost-button" type="button" :disabled="loading" @click="loadNews">刷新</button>
      </header>

      <section class="metric-grid">
        <MetricTile label="新闻总量" :value="result.total" caption="来自 /api/admin/news" />
        <MetricTile label="当前页" :value="result.items.length" :caption="`每页 ${pageSize} 条`" />
        <MetricTile label="当前页浏览" :value="totalViews" caption="真实 viewCount 汇总" />
        <MetricTile label="当前页互动" :value="totalInteractions" caption="赞 / 藏 / 评汇总" />
      </section>

      <section class="toolbar">
        <form class="search-form" @submit.prevent="search">
          <input v-model="keyword" type="search" placeholder="按标题或摘要搜索" />
          <button class="primary-button" type="submit" :disabled="loading">查询</button>
          <button class="ghost-button" type="button" :disabled="loading" @click="resetSearch">重置</button>
        </form>
        <span v-if="appliedKeyword" class="filter-chip">关键词：{{ appliedKeyword }}</span>
      </section>

      <p v-if="error" class="page-error">{{ error }}</p>
      <p v-if="deletingId" class="page-note">正在删除/下架 ID {{ deletingId }}...</p>

      <NewsTable
        :items="result.items"
        :loading="loading"
        @view="openDetail"
        @delete="confirmDelete"
      />

      <PaginationBar
        :page="page"
        :page-size="pageSize"
        :total="result.total"
        :disabled="loading"
        @change="changePage"
      />
    </section>

    <NewsDetailDrawer
      :detail="detail"
      :loading="detailLoading"
      :error="detailError"
      @close="detail = null; detailError = ''"
    />
  </main>
</template>
