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
  getTopics,
  getCommunityPosts,
  getPostDetail,
  createPost,
  favoritePost,
  unfavoritePost,
  likePost,
  wxLogin
};
