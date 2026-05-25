export const TOKEN_STORAGE_KEY = 'web-admin-token';

export function normalizeToken(input: string): string {
  const token = input.trim();
  if (!token) {
    throw new Error('请输入管理员 Token');
  }
  return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

export function loadToken(): string {
  return localStorage.getItem(TOKEN_STORAGE_KEY) ?? '';
}

export function saveToken(input: string): string {
  const token = normalizeToken(input);
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
  return token;
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}
