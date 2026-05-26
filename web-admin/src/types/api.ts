export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  msg: string;
}

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface UserProfile {
  id: number;
  nickname: string;
  avatarUrl: string;
  phone: string | null;
  age: number | null;
  playYears: number | null;
  gender: string | null;
  role: string;
}

export interface LoginResult {
  token: string;
  user: UserProfile;
}

export interface NewsSummary {
  id: number;
  categoryId: number;
  categoryName: string;
  title: string;
  coverUrl: string;
  summary: string;
  author: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  favorited: boolean;
  updatedAt: string;
}

export interface CategoryItem {
  id: number;
  name: string;
  sortNo: number;
}

export interface AdminCategoryStat {
  categoryId: number;
  categoryName: string;
  newsCount: number;
  totalViews: number;
  totalLikes: number;
  totalFavorites: number;
}

export interface AdminSummary {
  userCount: number;
  activeUserCount: number;
  newsCount: number;
  publishedNewsCount: number;
  offlineNewsCount: number;
  postCount: number;
  commentCount: number;
  totalViews: number;
  totalLikes: number;
  totalFavorites: number;
  categoryStats: AdminCategoryStat[];
}

export interface AdminNewsRankingItem {
  id: number;
  title: string;
  categoryName: string;
  coverUrl: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  heatScore: number;
}

export interface AdminUserItem {
  id: number;
  nickname: string;
  avatarUrl: string;
  phone: string | null;
  role: string;
  status: string;
  postCount: number;
  commentCount: number;
  favoriteCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserDetail extends AdminUserItem {
  age: number | null;
  playYears: number | null;
  gender: string | null;
  likeCount: number;
  browseCount: number;
}

export interface NewsSyncResult {
  pages: number;
  fetched: number;
  inserted: number;
  skipped: number;
}

export interface CommentNode {
  id: number;
  targetType: string;
  targetId: number;
  parentId: number | null;
  userId: number;
  nickname: string;
  avatarUrl: string;
  content: string;
  createdAt: string;
  children: CommentNode[];
}

export interface NewsDetail extends NewsSummary {
  content: string;
  mediaUrl: string | null;
  mediaType: string;
  liked: boolean;
  comments: CommentNode[];
}
