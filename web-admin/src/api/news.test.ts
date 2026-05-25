import { beforeEach, describe, expect, it, vi } from 'vitest';
import { deleteNews, getNewsDetail, listAdminNews } from './news';
import { saveToken } from '../stores/session';

describe('news API', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    saveToken('admin.token');
  });

  it('requests the admin news list with pagination and keyword', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({
        success: true,
        data: { items: [], total: 0, page: 2, pageSize: 20 },
        msg: 'ok'
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);

    await listAdminNews({ page: 2, pageSize: 20, keyword: '羽毛球' });

    const [url] = fetchSpy.mock.calls[0];
    expect(String(url)).toBe('http://127.0.0.1:8080/api/admin/news?page=2&pageSize=20&keyword=%E7%BE%BD%E6%AF%9B%E7%90%83');
  });

  it('requests a public news detail for read-only preview', async () => {
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

    const detail = await getNewsDetail(9);

    expect(detail.id).toBe(9);
    expect(String(fetchSpy.mock.calls[0][0])).toBe('http://127.0.0.1:8080/api/news/9');
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
});
