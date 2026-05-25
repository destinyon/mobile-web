import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest, ApiError } from './client';
import { saveToken } from '../stores/session';

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('unwraps successful backend envelopes', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({ success: true, data: { value: 7 }, msg: 'ok' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )));

    await expect(apiRequest<{ value: number }>('/api/demo')).resolves.toEqual({ value: 7 });
  });

  it('throws the backend message when the envelope is unsuccessful', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({ success: false, data: null, msg: '需要管理员权限' }),
      { status: 403, headers: { 'Content-Type': 'application/json' } }
    )));

    await expect(apiRequest('/api/admin/news')).rejects.toMatchObject({
      message: '需要管理员权限',
      status: 403
    });
  });

  it('sends Authorization when a token exists', async () => {
    const fetchSpy = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => new Response(
      JSON.stringify({ success: true, data: [], msg: 'ok' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    ));
    vi.stubGlobal('fetch', fetchSpy);
    saveToken('abc.def');

    await apiRequest('/api/admin/news');

    const [, init] = fetchSpy.mock.calls[0];
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer abc.def');
  });

  it('clears the token on unauthorized admin responses', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({ success: false, data: null, msg: '请先登录' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } }
    )));
    saveToken('abc.def');

    await expect(apiRequest('/api/admin/news')).rejects.toBeInstanceOf(ApiError);

    expect(localStorage.getItem('web-admin-token')).toBeNull();
  });
});
