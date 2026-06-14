import { apiRequest } from './client';
import type {
  AdminNewsRankingItem,
  AdminSummary,
  AdminUserDetail,
  AdminUserItem,
  CategoryItem,
  NewsDetail,
  NewsSyncResult,
  NewsSummary,
  PageResult,
  PostDetail
} from '../types/api';

export interface ListAdminNewsParams {
  page: number;
  pageSize: number;
  keyword?: string;
  categoryId?: number;
}

export interface ListAdminUsersParams {
  page: number;
  pageSize: number;
  keyword?: string;
}

export function listAdminNews(params: ListAdminNewsParams): Promise<PageResult<NewsSummary>> {
  return apiRequest<PageResult<NewsSummary>>('/api/admin/news', {
    query: {
      page: params.page,
      pageSize: params.pageSize,
      keyword: params.keyword,
      categoryId: params.categoryId
    }
  });
}

export function getAdminSummary(): Promise<AdminSummary> {
  return apiRequest<AdminSummary>('/api/admin/summary');
}

export function getAdminNewsDetail(id: number): Promise<NewsDetail> {
  return apiRequest<NewsDetail>(`/api/admin/news/${id}`);
}

export function getAdminPostDetail(id: number): Promise<PostDetail> {
  return apiRequest<PostDetail>(`/api/admin/posts/${id}`);
}

export function getAdminNewsRankings(limit = 10): Promise<AdminNewsRankingItem[]> {
  return apiRequest<AdminNewsRankingItem[]>('/api/admin/news/rankings', {
    query: { limit }
  });
}

export function listAdminUsers(params: ListAdminUsersParams): Promise<PageResult<AdminUserItem>> {
  return apiRequest<PageResult<AdminUserItem>>('/api/admin/users', {
    query: {
      page: params.page,
      pageSize: params.pageSize,
      keyword: params.keyword
    }
  });
}

export function getAdminUserDetail(id: number): Promise<AdminUserDetail> {
  return apiRequest<AdminUserDetail>(`/api/admin/users/${id}`);
}

export function listCategories(): Promise<CategoryItem[]> {
  return apiRequest<CategoryItem[]>('/api/categories');
}

export function syncNews(pages?: number): Promise<NewsSyncResult> {
  return apiRequest<NewsSyncResult>('/api/admin/news/sync', {
    method: 'POST',
    query: { pages }
  });
}

export function deleteNews(id: number): Promise<void> {
  return apiRequest<void>(`/api/admin/news/${id}/status`, {
    method: 'PUT',
    query: {
      status: 'OFFLINE'
    }
  });
}
