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
