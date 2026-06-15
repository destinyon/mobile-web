function isAuthError(error) {
  return error && error.code === 'AUTH_REQUIRED';
}

function saveLogin(token, userInfo) {
  const app = getApp();
  app.setAuth(token, userInfo);
}

function clearLogin() {
  getApp().clearAuth();
}

module.exports = {
  isAuthError,
  saveLogin,
  clearLogin
};
