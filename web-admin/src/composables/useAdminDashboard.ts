import { computed, onMounted, ref } from 'vue';
import {
  deleteNews,
  getAdminNewsDetail,
  getAdminNewsRankings,
  getAdminSummary,
  getAdminUserDetail,
  listAdminNews,
  listAdminUsers,
  listCategories,
  syncNews
} from '../api/news';
import { ApiError } from '../api/client';
import { clearToken } from '../stores/session';
import type {
  AdminNewsRankingItem,
  AdminSummary,
  AdminUserDetail,
  AdminUserItem,
  CategoryItem,
  NewsDetail,
  NewsSummary,
  PageResult
} from '../types/api';

export type SectionKey = 'overview' | 'news' | 'rankings' | 'users';

export function useAdminDashboard(onLogout: (reason?: string) => void) {
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
  const detail = ref<NewsDetail | null>(null);
  const detailLoading = ref(false);
  const detailError = ref('');
  const userDetail = ref<AdminUserDetail | null>(null);
  const userDetailLoading = ref(false);
  const userDetailError = ref('');

  const currentTitle = computed(() => {
    const titles: Record<SectionKey, string> = {
      overview: '运营概览',
      news: '文章管理',
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
      const result = await syncNews();
      void result;
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
      detail.value = await getAdminNewsDetail(item.id);
    } catch (err) {
      detailError.value = err instanceof Error ? err.message : '文章详情加载失败';
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
    const confirmed = window.confirm(`确认下架这篇文章？\n\n${item.title}`);
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
    onLogout();
  }

  function handleError(err: unknown): void {
    pageError.value = err instanceof Error ? err.message : '请求失败';
    if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
      clearToken();
      onLogout(`${pageError.value}，请重新登录`);
    }
  }

  return {
    activeSection,
    collapsed,
    keyword,
    selectedCategoryId,
    page,
    pageSize,
    newsLoading,
    syncingNews,
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
  };
}
