import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  deleteNews,
  getAdminNewsDetail,
  getAdminNewsRankings,
  getAdminUserDetail,
  getAdminSummary,
  listAdminNews,
  listAdminUsers,
  listCategories,
  syncNews
} from './news';
import { saveToken } from '../stores/session';

describe('news API', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    saveToken('admin.token');
  });

  it('requests the admin news list with pagination, keyword and category', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({
        success: true,
        data: { items: [], total: 0, page: 2, pageSize: 20 },
        msg: 'ok'
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);

    await listAdminNews({ page: 2, pageSize: 20, keyword: '羽毛球', categoryId: 3 });

    const [url] = fetchSpy.mock.calls[0];
    expect(String(url)).toBe('http://127.0.0.1:8080/api/admin/news?page=2&pageSize=20&keyword=%E7%BE%BD%E6%AF%9B%E7%90%83&categoryId=3');
  });

  it('requests an admin news detail for read-only preview without public side effects', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({
        success: true,
        data: {
          id: 9,
          categoryId: 1,
          categoryName: '赛事',
          title: 'title',
          coverUrl: '',
          summary: '',
          author: 'author',
          content: '<p>content</p>',
          mediaUrl: null,
          mediaType: 'IMAGE',
          viewCount: 1,
          likeCount: 2,
          favoriteCount: 3,
          commentCount: 4,
          liked: false,
          favorited: false,
          updatedAt: '2026-05-26T00:00:00',
          comments: []
        },
        msg: 'ok'
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);

    const detail = await getAdminNewsDetail(9);

    expect(detail.id).toBe(9);
    expect(String(fetchSpy.mock.calls[0][0])).toBe('http://127.0.0.1:8080/api/admin/news/9');
  });

  it('requests dashboard summary, rankings, categories and users from real APIs', async () => {
    const fetchSpy = vi.fn(async (input: RequestInfo | URL) => {
      const path = new URL(String(input)).pathname;
      const payloadByPath: Record<string, unknown> = {
        '/api/admin/summary': {
          userCount: 2,
          activeUserCount: 2,
          newsCount: 5,
          publishedNewsCount: 4,
          offlineNewsCount: 1,
          postCount: 3,
          commentCount: 7,
          totalViews: 99,
          totalLikes: 11,
          totalFavorites: 12,
          categoryStats: []
        },
        '/api/admin/news/rankings': [],
        '/api/categories': [],
        '/api/admin/users': { items: [], total: 0, page: 1, pageSize: 20 }
      };
      return new Response(
        JSON.stringify({ success: true, data: payloadByPath[path], msg: 'ok' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      );
    });
    vi.stubGlobal('fetch', fetchSpy);

    await getAdminSummary();
    await getAdminNewsRankings(10);
    await listCategories();
    await listAdminUsers({ page: 1, pageSize: 20, keyword: 'why' });

    expect(String(fetchSpy.mock.calls[0][0])).toBe('http://127.0.0.1:8080/api/admin/summary');
    expect(String(fetchSpy.mock.calls[1][0])).toBe('http://127.0.0.1:8080/api/admin/news/rankings?limit=10');
    expect(String(fetchSpy.mock.calls[2][0])).toBe('http://127.0.0.1:8080/api/categories');
    expect(String(fetchSpy.mock.calls[3][0])).toBe('http://127.0.0.1:8080/api/admin/users?page=1&pageSize=20&keyword=why');
  });

  it('requests an admin user detail for profile inspection', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({
        success: true,
        data: {
          id: 2,
          nickname: '管理员',
          avatarUrl: '',
          phone: '18800000000',
          age: 30,
          playYears: 10,
          gender: '男',
          role: 'ADMIN',
          status: 'ACTIVE',
          postCount: 1,
          commentCount: 2,
          favoriteCount: 3,
          likeCount: 4,
          browseCount: 5,
          createdAt: '2026-05-26T00:00:00',
          updatedAt: '2026-05-26T00:00:00'
        },
        msg: 'ok'
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);

    const detail = await getAdminUserDetail(2);

    expect(detail.nickname).toBe('管理员');
    expect(String(fetchSpy.mock.calls[0][0])).toBe('http://127.0.0.1:8080/api/admin/users/2');
  });

  it('soft-deletes news by setting status to OFFLINE', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({ success: true, data: null, msg: 'ok' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);

    await deleteNews(12);

    const [url, init] = fetchSpy.mock.calls[0];
    expect(String(url)).toBe('http://127.0.0.1:8080/api/admin/news/12/status?status=OFFLINE');
    expect(init?.method).toBe('PUT');
  });

  it('starts a real backend news sync through the admin endpoint', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({
        success: true,
        data: { pages: 1, fetched: 10, inserted: 10, skipped: 0 },
        msg: 'ok'
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);

    const result = await syncNews(1);

    const [url, init] = fetchSpy.mock.calls[0];
    expect(String(url)).toBe('http://127.0.0.1:8080/api/admin/news/sync?pages=1');
    expect(init?.method).toBe('POST');
    expect(result.inserted).toBe(10);
  });
});
