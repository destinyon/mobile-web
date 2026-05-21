App({
  globalData: {
    token: '',
    userInfo: null
  },

  onLaunch() {
    const token = wx.getStorageSync('token') || '';
    const userInfo = wx.getStorageSync('userInfo') || null;
    this.globalData.token = token;
    this.globalData.userInfo = userInfo;
  },

  setAuth(token, userInfo) {
    this.globalData.token = token || '';
    this.globalData.userInfo = userInfo || null;
    if (token) {
      wx.setStorageSync('token', token);
    }
    if (userInfo) {
      wx.setStorageSync('userInfo', userInfo);
    }
  },

  clearAuth() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  }
});
