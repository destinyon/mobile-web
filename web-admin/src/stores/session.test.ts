import { beforeEach, describe, expect, it } from 'vitest';
import { clearToken, loadToken, normalizeToken, saveToken } from './session';

describe('session token storage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('normalizes a raw token into a Bearer token', () => {
    expect(normalizeToken('abc.def')).toBe('Bearer abc.def');
  });

  it('preserves an existing Bearer prefix', () => {
    expect(normalizeToken('Bearer abc.def')).toBe('Bearer abc.def');
  });

  it('rejects blank tokens', () => {
    expect(() => normalizeToken('   ')).toThrow('请输入管理员 Token');
  });

  it('saves, loads, and clears the normalized token', () => {
    saveToken('abc.def');

    expect(loadToken()).toBe('Bearer abc.def');

    clearToken();

    expect(loadToken()).toBe('');
  });
});
