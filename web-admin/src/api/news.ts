import { apiRequest } from './client';
import type { NewsDetail, NewsSummary, PageResult } from '../types/api';

export interface ListAdminNewsParams {
  page: number;
  pageSize: number;
  keyword?: string;
}

export function listAdminNews(params: ListAdminNewsParams): Promise<PageResult<NewsSummary>> {
  return apiRequest<PageResult<NewsSummary>>('/api/admin/news', {
    query: {
      page: params.page,
      pageSize: params.pageSize,
      keyword: params.keyword
    }
  });
}

export function getNewsDetail(id: number): Promise<NewsDetail> {
  return apiRequest<NewsDetail>(`/api/news/${id}`);
}

export function deleteNews(id: number): Promise<void> {
  return apiRequest<void>(`/api/admin/news/${id}/status`, {
    method: 'PUT',
    query: {
      status: 'OFFLINE'
    }
  });
}
