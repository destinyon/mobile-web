const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

Page({
  data: {
    profile: {},
    showAuth: false,
    pendingUrl: '',
    error: ''
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
    if (wx.getStorageSync('token')) {
      this.loadProfile();
    } else {
      this.resetProfile();
    }
  },

  loadProfile() {
    this.setData({ error: '' });
    return api.getProfile()
      .then((profile) => this.setData({ profile }))
      .catch((error) => {
        if (isAuthError(error)) return;
        this.setData({ error: error.msg || '资料加载失败' });
      });
  },

  resetProfile() {
    this.setData({
      profile: {},
      showAuth: false,
      pendingUrl: '',
      error: ''
    });
  },

  ensureLogin() {
    if (wx.getStorageSync('token')) {
      wx.navigateTo({ url: '/pages/mine/profile/index' });
      return;
    }
    this.setData({ showAuth: true });
  },

  goPage(event) {
    const url = event.currentTarget.dataset.url;
    if (!wx.getStorageSync('token')) {
      this.setData({ showAuth: true, pendingUrl: url });
      return;
    }
    wx.navigateTo({ url });
  },

  goSettings() {
    wx.navigateTo({ url: '/pages/mine/settings/index' });
  },

  hideAuth() {
    this.setData({ showAuth: false, pendingUrl: '' });
  },

  afterLogin() {
    const url = this.data.pendingUrl;
    this.setData({ showAuth: false, pendingUrl: '' });
    this.loadProfile();
    if (url) {
      wx.navigateTo({ url });
    }
  }
});
