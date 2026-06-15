const { baseUrl } = require('./constants');

function getToken() {
  const app = getApp();
  return (app.globalData && app.globalData.token) || wx.getStorageSync('token') || '';
}

function cleanValue(value) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  if (Array.isArray(value)) {
    return value.map(cleanValue).filter((item) => item !== undefined);
  }
  if (typeof value === 'object') {
    const result = {};
    Object.keys(value).forEach((key) => {
      const next = cleanValue(value[key]);
      if (next !== undefined) {
        result[key] = next;
      }
    });
    return result;
  }
  return value;
}

function normalizeUrl(url) {
  if (/^https?:\/\//.test(url)) {
    return url;
  }
  return `${baseUrl}${url}`;
}

function request(options) {
  const token = getToken();
  const needAuth = Boolean(options.needAuth);

  if (needAuth && !token) {
    return Promise.reject({ code: 'AUTH_REQUIRED', msg: '请先登录后继续操作' });
  }

  return new Promise((resolve, reject) => {
    const data = cleanValue(options.data || {});
    wx.request({
      url: normalizeUrl(options.url),
      method: options.method || 'GET',
      data,
      header: {
        'content-type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.header || {})
      },
      success(res) {
        const body = res.data || {};
        if (res.statusCode === 401 || res.statusCode === 403) {
          reject({ code: 'AUTH_REQUIRED', msg: body.msg || '请先登录后继续操作' });
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject({ code: 'HTTP_ERROR', msg: body.msg || '请求失败，请稍后重试' });
          return;
        }
        if (body.success) {
          resolve(body.data);
          return;
        }
        reject({ code: 'API_ERROR', msg: body.msg || '操作失败' });
      },
      fail() {
        reject({ code: 'NETWORK_ERROR', msg: '网络连接失败，请检查后重试' });
      }
    });
  });
}

module.exports = {
  request
};
