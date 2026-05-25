import { clearToken, loadToken } from '../stores/session';
import type { ApiEnvelope } from '../types/api';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8080').replace(/\/$/, '');

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  query?: Record<string, string | number | boolean | null | undefined>;
  body?: unknown;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? 'GET',
    headers: buildHeaders(options.body),
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });
  const envelope = await readEnvelope<T>(response);

  if (!response.ok || !envelope.success) {
    if (response.status === 401 || response.status === 403) {
      clearToken();
    }
    throw new ApiError(envelope.msg || `请求失败（${response.status}）`, response.status);
  }

  return envelope.data;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(path, API_BASE_URL);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      url.searchParams.set(key, String(value));
    }
  });
  return url.toString();
}

function buildHeaders(body: unknown): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: 'application/json'
  };
  const token = loadToken();
  if (token) {
    headers.Authorization = token;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
}

async function readEnvelope<T>(response: Response): Promise<ApiEnvelope<T>> {
  try {
    return await response.json() as ApiEnvelope<T>;
  } catch {
    return {
      success: false,
      data: null as T,
      msg: `服务响应格式异常（${response.status}）`
    };
  }
}
