const { request } = require('../utils/request');

function getBanners() {
  return request({ url: '/api/banners' });
}

function getCategories() {
  return request({ url: '/api/categories' });
}

function getNews(params) {
  return request({ url: '/api/news', data: params || {} });
}

function getNewsDetail(id) {
  return request({ url: `/api/news/${id}` });
}

function favoriteNews(id) {
  return request({ url: `/api/news/${id}/favorite`, method: 'POST', needAuth: true });
}

function unfavoriteNews(id) {
  return request({ url: `/api/news/${id}/favorite`, method: 'DELETE', needAuth: true });
}

function likeNews(id) {
  return request({ url: `/api/news/${id}/like`, method: 'POST', needAuth: true });
}

function createComment(data) {
  return request({ url: '/api/comments', method: 'POST', data, needAuth: true });
}

function getProfile() {
  return request({ url: '/api/user/profile', needAuth: true });
}

function getFavorites(params) {
  return request({ url: '/api/user/favorites', data: params || {}, needAuth: true });
}

function getComments(params) {
  return request({ url: '/api/user/comments', data: params || {}, needAuth: true });
}

function getPosts(params) {
  return request({ url: '/api/user/posts', data: params || {}, needAuth: true });
}

function getHistory(params) {
  return request({ url: '/api/user/history', data: params || {}, needAuth: true });
}

function getTopics() {
  return request({ url: '/api/topics' });
}

function getCommunityPosts(params) {
  return request({ url: '/api/posts', data: params || {} });
}

function getPostDetail(id) {
  return request({ url: `/api/posts/${id}` });
}

function createPost(data) {
  return request({ url: '/api/posts', method: 'POST', data, needAuth: true });
}

function updatePost(id, data) {
  return request({ url: `/api/posts/${id}`, method: 'PUT', data, needAuth: true });
}

function getPostDraft() {
  return request({ url: '/api/posts/draft', needAuth: true });
}

function savePostDraft(data) {
  return request({ url: '/api/posts/draft', method: 'PUT', data, needAuth: true });
}

function publishPostDraft() {
  return request({ url: '/api/posts/draft/publish', method: 'POST', needAuth: true });
}

function favoritePost(id) {
  return request({ url: `/api/posts/${id}/favorite`, method: 'POST', needAuth: true });
}

function unfavoritePost(id) {
  return request({ url: `/api/posts/${id}/favorite`, method: 'DELETE', needAuth: true });
}

function likePost(id) {
  return request({ url: `/api/posts/${id}/like`, method: 'POST', needAuth: true });
}

function wxLogin(data) {
  return request({ url: '/api/auth/wx-login', method: 'POST', data });
}

function updateProfile(data) {
  return request({ url: '/api/user/profile', method: 'PUT', data, needAuth: true });
}

function uploadFile(filePath) {
  const { baseUrl } = require('../utils/constants');
  const token = wx.getStorageSync('token') || '';
  if (!token) {
    return Promise.reject({ code: 'AUTH_REQUIRED', msg: '请先登录后继续操作' });
  }
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${baseUrl}/api/upload`,
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${token}` },
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data || '{}');
        } catch (error) {
          reject({ code: 'API_ERROR', msg: '上传响应解析失败' });
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.success) {
          resolve(body.data);
          return;
        }
        reject({ code: res.statusCode === 401 ? 'AUTH_REQUIRED' : 'API_ERROR', msg: body.msg || '上传失败' });
      },
      fail() {
        reject({ code: 'NETWORK_ERROR', msg: '网络连接失败，请检查后重试' });
      }
    });
  });
}

function deleteUpload(objectKey) {
  return request({ url: `/api/upload?objectKey=${encodeURIComponent(objectKey)}`, method: 'DELETE', needAuth: true });
}

module.exports = {
  getBanners,
  getCategories,
  getNews,
  getNewsDetail,
  favoriteNews,
  unfavoriteNews,
  likeNews,
  createComment,
  getProfile,
  getFavorites,
  getComments,
  getPosts,
  getHistory,
  getTopics,
  getCommunityPosts,
  getPostDetail,
  createPost,
  updatePost,
  getPostDraft,
  savePostDraft,
  publishPostDraft,
  favoritePost,
  unfavoritePost,
  likePost,
  wxLogin,
  updateProfile,
  uploadFile,
  deleteUpload
};
