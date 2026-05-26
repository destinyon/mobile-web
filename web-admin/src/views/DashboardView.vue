<script setup lang="ts">
import {
  BarChart3,
  Newspaper,
  RefreshCw,
  Search,
  CloudDownload,
  Star,
  Users,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen
} from 'lucide-vue-next';
import MetricTile from '../components/MetricTile.vue';
import NewsDetailDrawer from '../components/NewsDetailDrawer.vue';
import NewsTable from '../components/NewsTable.vue';
import OverviewPanel from '../components/OverviewPanel.vue';
import PaginationBar from '../components/PaginationBar.vue';
import RankingPanel from '../components/RankingPanel.vue';
import UserDetailDrawer from '../components/UserDetailDrawer.vue';
import UserTable from '../components/UserTable.vue';
import { useAdminDashboard } from '../composables/useAdminDashboard';

const emit = defineEmits<{
  logout: [reason?: string];
}>();

const navItems = [
  { key: 'overview' as const, label: '概览', icon: BarChart3 },
  { key: 'news' as const, label: '文章', icon: Newspaper },
  { key: 'rankings' as const, label: '排行', icon: Star },
  { key: 'users' as const, label: '用户', icon: Users }
];

const dashboard = useAdminDashboard((reason?: string) => emit('logout', reason));

const {
  activeSection,
  collapsed,
  keyword,
  selectedCategoryId,
  page,
  pageSize,
  newsLoading,
  syncingNews,
  deletingId,
  pageError,
  newsResult,
  usersKeyword,
  usersPage,
  usersPageSize,
  usersLoading,
  usersResult,
  categories,
  summary,
  rankings,
  overviewLoading,
  rankingsLoading,
  detail,
  detailLoading,
  detailError,
  userDetail,
  userDetailLoading,
  userDetailError,
  currentTitle,
  selectSection,
  searchNews,
  resetNewsSearch,
  runNewsSync,
  changeNewsPage,
  searchUsers,
  resetUsersSearch,
  changeUsersPage,
  openDetail,
  openUserDetail,
  confirmDelete,
  refreshCurrent,
  closeDetail,
  closeUserDetail,
  logout
} = dashboard;
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
      <p v-if="deletingId" class="page-note">正在下架 ID {{ deletingId }}</p>

      <section v-if="activeSection === 'overview'" class="section-stack">
        <section class="metric-grid">
          <MetricTile label="用户总数" :value="summary?.userCount ?? 0" :caption="`活跃 ${summary?.activeUserCount ?? 0}`" />
          <MetricTile label="文章总数" :value="summary?.newsCount ?? 0" :caption="`已发布 ${summary?.publishedNewsCount ?? 0}`" />
          <MetricTile label="社区帖子" :value="summary?.postCount ?? 0" :caption="`评论 ${summary?.commentCount ?? 0}`" />
          <MetricTile label="总浏览" :value="summary?.totalViews ?? 0" caption="文章浏览量" />
          <MetricTile label="总互动" :value="(summary?.totalLikes ?? 0) + (summary?.totalFavorites ?? 0) + (summary?.commentCount ?? 0)" caption="赞/藏/评" />
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
        <section class="ranking-table">
          <button
            v-for="item in rankings"
            :key="item.id"
            type="button"
            class="ranking-card"
            @click="openDetail(item)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.categoryName || '未分类' }}</span>
            <em>热度 {{ item.heatScore }}</em>
          </button>
        </section>
      </section>

      <section v-else class="section-stack">
        <section class="toolbar">
          <form class="search-form" @submit.prevent="searchUsers">
            <div class="search-box">
              <Search :size="16" />
              <input v-model="usersKeyword" type="search" placeholder="按昵称、手机号或角色搜索" />
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
