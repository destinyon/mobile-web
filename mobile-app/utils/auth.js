export function getToken() {
  return uni.getStorageSync('token') || '';
}

export function saveLogin(result) {
  const token = result.token || '';
  const user = result.user || {};
  uni.setStorageSync('token', token);
  uni.setStorageSync('userInfo', user);
}

export function logout() {
  uni.removeStorageSync('token');
  uni.removeStorageSync('userInfo');
}

export function isAuthError(error) {
  return error?.statusCode === 401 || error?.statusCode === 403;
}

export function requireLogin() {
  if (getToken()) {
    return true;
  }
  uni.navigateTo({ url: '/pages/login/index' });
  return false;
}
