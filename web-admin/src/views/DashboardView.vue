<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  BarChart3,
  CloudDownload,
  LogOut,
  Newspaper,
  PanelLeftClose,
  PanelLeftOpen,
  RefreshCw,
  Search,
  Star,
  Users
} from 'lucide-vue-next';
import MetricTile from '../components/MetricTile.vue';
import NewsDetailDrawer from '../components/NewsDetailDrawer.vue';
import NewsTable from '../components/NewsTable.vue';
import OverviewPanel from '../components/OverviewPanel.vue';
import PaginationBar from '../components/PaginationBar.vue';
import RankingPanel from '../components/RankingPanel.vue';
import UserDetailDrawer from '../components/UserDetailDrawer.vue';
import UserTable from '../components/UserTable.vue';
import { ApiError } from '../api/client';
import {
  deleteNews,
  getAdminNewsDetail,
  getAdminNewsRankings,
  getAdminPostDetail,
  getAdminSummary,
  getAdminUserDetail,
  listAdminNews,
  listAdminUsers,
  listCategories,
  syncNews
} from '../api/news';
import { clearToken } from '../stores/session';
import type {
  AdminContentDetail,
  AdminNewsRankingItem,
  AdminSummary,
  AdminUserDetail,
  AdminUserItem,
  CategoryItem,
  NewsSummary,
  PageResult
} from '../types/api';

const emit = defineEmits<{
  logout: [reason?: string];
}>();

type SectionKey = 'overview' | 'news' | 'rankings' | 'users';

const navItems = [
  { key: 'overview' as const, label: '概览', icon: BarChart3 },
  { key: 'news' as const, label: '新闻', icon: Newspaper },
  { key: 'rankings' as const, label: '排行', icon: Star },
  { key: 'users' as const, label: '用户', icon: Users }
];

const activeSection = ref<SectionKey>('overview');
const collapsed = ref(false);
const keyword = ref('');
const appliedKeyword = ref('');
const selectedCategoryId = ref<number | ''>('');
const appliedCategoryId = ref<number | undefined>(undefined);
const page = ref(1);
const pageSize = 12;
const newsLoading = ref(false);
const syncingNews = ref(false);
const pageError = ref('');
const newsResult = ref<PageResult<NewsSummary>>({ items: [], total: 0, page: 1, pageSize });

const usersKeyword = ref('');
const usersAppliedKeyword = ref('');
const usersPage = ref(1);
const usersPageSize = 12;
const usersLoading = ref(false);
const usersResult = ref<PageResult<AdminUserItem>>({ items: [], total: 0, page: 1, pageSize: usersPageSize });

const categories = ref<CategoryItem[]>([]);
const summary = ref<AdminSummary | null>(null);
const rankings = ref<AdminNewsRankingItem[]>([]);
const overviewLoading = ref(false);
const rankingsLoading = ref(false);
const detail = ref<AdminContentDetail | null>(null);
const detailLoading = ref(false);
const detailError = ref('');
const userDetail = ref<AdminUserDetail | null>(null);
const userDetailLoading = ref(false);
const userDetailError = ref('');

const currentTitle = computed(() => {
  const titles: Record<SectionKey, string> = {
    overview: '运营概览',
    news: '新闻管理',
    rankings: '热度排行',
    users: '用户管理'
  };
  return titles[activeSection.value];
});

onMounted(() => {
  void loadInitialData();
});

async function loadInitialData(): Promise<void> {
  await Promise.all([
    loadOverview(),
    loadNews(),
    loadRankings(),
    loadUsers(),
    loadCategories()
  ]);
}

async function loadOverview(): Promise<void> {
  overviewLoading.value = true;
  pageError.value = '';
  try {
    summary.value = await getAdminSummary();
  } catch (err) {
    handleError(err);
  } finally {
    overviewLoading.value = false;
  }
}

async function loadCategories(): Promise<void> {
  try {
    categories.value = await listCategories();
  } catch (err) {
    handleError(err);
  }
}

async function loadRankings(): Promise<void> {
  rankingsLoading.value = true;
  pageError.value = '';
  try {
    rankings.value = await getAdminNewsRankings(10);
  } catch (err) {
    handleError(err);
  } finally {
    rankingsLoading.value = false;
  }
}

async function loadNews(): Promise<void> {
  newsLoading.value = true;
  pageError.value = '';
  try {
    newsResult.value = await listAdminNews({
      page: page.value,
      pageSize,
      keyword: appliedKeyword.value,
      categoryId: appliedCategoryId.value
    });
  } catch (err) {
    handleError(err);
  } finally {
    newsLoading.value = false;
  }
}

async function runNewsSync(): Promise<void> {
  syncingNews.value = true;
  pageError.value = '';
  try {
    await syncNews();
    await Promise.all([loadNews(), loadOverview(), loadRankings()]);
  } catch (err) {
    handleError(err);
  } finally {
    syncingNews.value = false;
  }
}

async function loadUsers(): Promise<void> {
  usersLoading.value = true;
  pageError.value = '';
  try {
    usersResult.value = await listAdminUsers({
      page: usersPage.value,
      pageSize: usersPageSize,
      keyword: usersAppliedKeyword.value
    });
  } catch (err) {
    handleError(err);
  } finally {
    usersLoading.value = false;
  }
}

function selectSection(section: SectionKey): void {
  activeSection.value = section;
}

function searchNews(): void {
  appliedKeyword.value = keyword.value.trim();
  appliedCategoryId.value = selectedCategoryId.value === '' ? undefined : selectedCategoryId.value;
  page.value = 1;
  void loadNews();
}

function resetNewsSearch(): void {
  keyword.value = '';
  selectedCategoryId.value = '';
  appliedKeyword.value = '';
  appliedCategoryId.value = undefined;
  page.value = 1;
  void loadNews();
}

function changeNewsPage(nextPage: number): void {
  page.value = nextPage;
  void loadNews();
}

function searchUsers(): void {
  usersAppliedKeyword.value = usersKeyword.value.trim();
  usersPage.value = 1;
  void loadUsers();
}

function resetUsersSearch(): void {
  usersKeyword.value = '';
  usersAppliedKeyword.value = '';
  usersPage.value = 1;
  void loadUsers();
}

function changeUsersPage(nextPage: number): void {
  usersPage.value = nextPage;
  void loadUsers();
}

async function openDetail(item: NewsSummary | AdminNewsRankingItem): Promise<void> {
  detail.value = null;
  detailError.value = '';
  detailLoading.value = true;
  try {
    if ('targetType' in item && item.targetType === 'POST') {
      const postDetail = await getAdminPostDetail(item.id);
      detail.value = { ...postDetail, targetType: 'POST' };
    } else {
      const newsDetail = await getAdminNewsDetail(item.id);
      detail.value = { ...newsDetail, targetType: 'NEWS' };
    }
  } catch (err) {
    detailError.value = err instanceof Error ? err.message : '新闻详情加载失败';
  } finally {
    detailLoading.value = false;
  }
}

async function openUserDetail(item: AdminUserItem): Promise<void> {
  userDetail.value = null;
  userDetailError.value = '';
  userDetailLoading.value = true;
  try {
    userDetail.value = await getAdminUserDetail(item.id);
  } catch (err) {
    userDetailError.value = err instanceof Error ? err.message : '用户详情加载失败';
  } finally {
    userDetailLoading.value = false;
  }
}

async function confirmDelete(item: NewsSummary): Promise<void> {
  const confirmed = window.confirm(`确认下架这条新闻？\n\n${item.title}`);
  if (!confirmed) {
    return;
  }
  pageError.value = '';
  try {
    await deleteNews(item.id);
    await Promise.all([loadNews(), loadOverview(), loadRankings()]);
  } catch (err) {
    handleError(err);
  }
}

async function refreshCurrent(): Promise<void> {
  if (activeSection.value === 'overview') {
    await loadOverview();
  } else if (activeSection.value === 'news') {
    await loadNews();
  } else if (activeSection.value === 'rankings') {
    await loadRankings();
  } else {
    await loadUsers();
  }
}

function closeDetail(): void {
  detail.value = null;
  detailError.value = '';
}

function closeUserDetail(): void {
  userDetail.value = null;
  userDetailError.value = '';
}

function logout(): void {
  clearToken();
  emit('logout');
}

function handleError(err: unknown): void {
  pageError.value = err instanceof Error ? err.message : '请求失败';
  if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
    clearToken();
    emit('logout', `${pageError.value}，请重新登录`);
  }
}
</script>

<template>
  <main class="admin-layout" :class="{ collapsed }">
    <aside class="nav-rail">
      <div class="rail-brand">
        <span class="rail-logo">羽</span>
        <strong>羽球在线</strong>
      </div>
      <button
        v-for="item in navItems"
        :key="item.key"
        class="rail-item"
        :class="{ active: activeSection === item.key }"
        type="button"
        :title="item.label"
        @click="selectSection(item.key)"
      >
        <component :is="item.icon" :size="18" />
        <span>{{ item.label }}</span>
      </button>
      <button class="rail-item rail-collapse" type="button" :title="collapsed ? '展开菜单' : '收起菜单'" @click="collapsed = !collapsed">
        <PanelLeftOpen v-if="collapsed" :size="18" />
        <PanelLeftClose v-else :size="18" />
        <span>{{ collapsed ? '展开' : '收起' }}</span>
      </button>
      <button class="rail-item rail-logout" type="button" title="退出登录" @click="logout">
        <LogOut :size="18" />
        <span>退出</span>
      </button>
    </aside>

    <section class="workbench">
      <header class="topbar">
        <div>
          <p class="eyebrow">运营中控台</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <button class="ghost-button icon-text-button" type="button" @click="refreshCurrent">
          <RefreshCw :size="16" />
          <span>刷新</span>
        </button>
      </header>

      <p v-if="pageError" class="page-error">{{ pageError }}</p>

      <section v-if="activeSection === 'overview'" class="section-stack">
        <section class="metric-grid">
          <MetricTile label="用户总数" :value="summary?.userCount ?? 0" :caption="`活跃 ${summary?.activeUserCount ?? 0}`" />
          <MetricTile label="新闻内容" :value="summary?.newsCount ?? 0" :caption="`已发布 ${summary?.publishedNewsCount ?? 0} / 下架 ${summary?.offlineNewsCount ?? 0}`" />
          <MetricTile label="社区帖子" :value="summary?.postCount ?? 0" :caption="`帖子评论 ${summary?.commentCount ?? 0}`" />
          <MetricTile label="内容浏览" :value="summary?.totalViews ?? 0" caption="新闻 + 帖子" />
          <MetricTile label="内容互动" :value="(summary?.totalLikes ?? 0) + (summary?.totalFavorites ?? 0) + (summary?.commentCount ?? 0)" caption="赞 / 收藏 / 评论" />
        </section>
        <section class="overview-main-grid">
          <OverviewPanel :summary="summary" :loading="overviewLoading" />
          <RankingPanel :items="rankings" :loading="rankingsLoading" @view="openDetail" />
        </section>
      </section>

      <section v-else-if="activeSection === 'news'" class="section-stack">
        <section class="toolbar">
          <form class="search-form" @submit.prevent="searchNews">
            <div class="search-box">
              <Search :size="16" />
              <input v-model="keyword" type="search" placeholder="按标题或摘要搜索" />
            </div>
            <select v-model="selectedCategoryId">
              <option value="">全部分类</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
            </select>
            <button class="primary-button" type="submit" :disabled="newsLoading">查询</button>
            <button class="ghost-button" type="button" :disabled="newsLoading" @click="resetNewsSearch">重置</button>
            <button class="ghost-button icon-text-button" type="button" :disabled="syncingNews" @click="runNewsSync">
              <CloudDownload :size="16" />
              <span>{{ syncingNews ? '同步中' : '同步新闻' }}</span>
            </button>
          </form>
        </section>

        <NewsTable
          :items="newsResult.items"
          :loading="newsLoading"
          @view="openDetail"
          @delete="confirmDelete"
        />

        <PaginationBar
          :page="page"
          :page-size="pageSize"
          :total="newsResult.total"
          :disabled="newsLoading"
          @change="changeNewsPage"
        />
      </section>

      <section v-else-if="activeSection === 'rankings'" class="section-stack">
        <RankingPanel :items="rankings" :loading="rankingsLoading" @view="openDetail" />
      </section>

      <section v-else class="section-stack">
        <section class="toolbar">
          <form class="search-form" @submit.prevent="searchUsers">
            <div class="search-box">
              <Search :size="16" />
              <input v-model="usersKeyword" type="search" placeholder="按昵称、手机号、邮箱或角色搜索" />
            </div>
            <button class="primary-button" type="submit" :disabled="usersLoading">查询</button>
            <button class="ghost-button" type="button" :disabled="usersLoading" @click="resetUsersSearch">重置</button>
          </form>
        </section>
        <UserTable :items="usersResult.items" :loading="usersLoading" @view="openUserDetail" />
        <PaginationBar
          :page="usersPage"
          :page-size="usersPageSize"
          :total="usersResult.total"
          :disabled="usersLoading"
          @change="changeUsersPage"
        />
      </section>
    </section>

    <NewsDetailDrawer
      :detail="detail"
      :loading="detailLoading"
      :error="detailError"
      @close="closeDetail"
    />
    <UserDetailDrawer
      :detail="userDetail"
      :loading="userDetailLoading"
      :error="userDetailError"
      @close="closeUserDetail"
    />
  </main>
</template>
