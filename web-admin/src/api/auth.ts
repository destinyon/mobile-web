import { apiRequest } from './client';
import { saveToken } from '../stores/session';
import type { LoginResult } from '../types/api';

export interface AdminLoginPayload {
  username: string;
  password: string;
}

export async function adminLogin(payload: AdminLoginPayload): Promise<LoginResult> {
  const result = await apiRequest<LoginResult>('/api/auth/admin-login', {
    method: 'POST',
    body: payload
  });
  saveToken(result.token);
  return result;
}
