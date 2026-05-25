import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminLogin } from './auth';
import { loadToken } from '../stores/session';

describe('admin auth api', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('posts admin credentials and saves the returned bearer token', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        data: {
          token: 'admin.jwt',
          user: {
            id: 2,
            nickname: '管理员',
            avatarUrl: '',
            phone: '',
            age: null,
            playYears: null,
            gender: '',
            role: 'ADMIN',
          },
        },
        msg: 'ok',
      }),
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await adminLogin({ username: 'admin', password: 'secret' });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe('http://127.0.0.1:8080/api/auth/admin-login');
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(JSON.stringify({ username: 'admin', password: 'secret' }));
    expect(loadToken()).toBe('Bearer admin.jwt');
    expect(result.user.role).toBe('ADMIN');
  });
});
