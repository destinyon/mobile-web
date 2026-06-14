import { API_BASE_URL } from './config';
import { getToken } from './auth';

export const UPLOAD_MAX_SIZE_MB = 5;
export const UPLOAD_MAX_SIZE_BYTES = UPLOAD_MAX_SIZE_MB * 1024 * 1024;

function unwrap(response) {
  const body = response.data || {};
  if (response.statusCode >= 200 && response.statusCode < 300 && body.success) {
    return body.data;
  }
  const message = body.msg || `请求失败(${response.statusCode})`;
  throw { statusCode: response.statusCode, msg: message };
}

export function isUploadTooLarge(file) {
  return Number(file?.size || 0) > UPLOAD_MAX_SIZE_BYTES;
}

export function uploadTooLargeMessage() {
  return `图片不能超过 ${UPLOAD_MAX_SIZE_MB}MB`;
}

export function request(options) {
  const token = getToken();
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (response) => {
        try {
          resolve(unwrap(response));
        } catch (error) {
          reject(error);
        }
      },
      fail: () => reject({ msg: '网络连接失败' })
    });
  });
}

export function uploadFile(filePath) {
  const token = getToken();
  if (!token) {
    return Promise.reject({ statusCode: 401, msg: '请先登录' });
  }
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/api/upload`,
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${token}` },
      success: (response) => {
        try {
          resolve(unwrap({
            statusCode: response.statusCode,
            data: typeof response.data === 'string' ? JSON.parse(response.data) : response.data
          }));
        } catch (error) {
          reject(error);
        }
      },
      fail: () => reject({ msg: '上传失败' })
    });
  });
}

export const api = {
  sendEmailCode: (email) => request({ url: '/api/auth/email-code', method: 'POST', data: { email } }),
  emailLogin: (email, code) => request({ url: '/api/auth/email-login', method: 'POST', data: { email, code } }),
  getBanners: () => request({ url: '/api/banners' }),
  getTopics: () => request({ url: '/api/topics' }),
  getNews: (params) => request({ url: '/api/news', data: params || {} }),
  getNewsDetail: (id) => request({ url: `/api/news/${id}` }),
  likeNews: (id) => request({ url: `/api/news/${id}/like`, method: 'POST' }),
  favoriteNews: (id) => request({ url: `/api/news/${id}/favorite`, method: 'POST' }),
  unfavoriteNews: (id) => request({ url: `/api/news/${id}/favorite`, method: 'DELETE' }),
  getPosts: (params) => request({ url: '/api/posts', data: params || {} }),
  getPostDetail: (id) => request({ url: `/api/posts/${id}` }),
  createPost: (data) => request({ url: '/api/posts', method: 'POST', data }),
  updatePost: (id, data) => request({ url: `/api/posts/${id}`, method: 'PUT', data }),
  getPostDraft: () => request({ url: '/api/posts/draft' }),
  savePostDraft: (data) => request({ url: '/api/posts/draft', method: 'PUT', data }),
  publishPostDraft: () => request({ url: '/api/posts/draft/publish', method: 'POST' }),
  likePost: (id) => request({ url: `/api/posts/${id}/like`, method: 'POST' }),
  favoritePost: (id) => request({ url: `/api/posts/${id}/favorite`, method: 'POST' }),
  unfavoritePost: (id) => request({ url: `/api/posts/${id}/favorite`, method: 'DELETE' }),
  createComment: (data) => request({ url: '/api/comments', method: 'POST', data }),
  getProfile: () => request({ url: '/api/user/profile' }),
  updateProfile: (data) => request({ url: '/api/user/profile', method: 'PUT', data }),
  getFavorites: () => request({ url: '/api/user/favorites' }),
  getComments: () => request({ url: '/api/user/comments' }),
  getHistory: () => request({ url: '/api/user/history' }),
  getMyPosts: () => request({ url: '/api/user/posts' }),
  deleteUpload: (objectKey) => request({ url: `/api/upload?objectKey=${encodeURIComponent(objectKey)}`, method: 'DELETE' })
};
