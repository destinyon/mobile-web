const { clearLogin } = require('../../../utils/auth');

Page({
  data: {
    loggedIn: false
  },

  onShow() {
    this.setData({ loggedIn: !!wx.getStorageSync('token') });
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后仍可浏览资讯和社区内容。',
      confirmText: '退出',
      confirmColor: '#c74632',
      success: (res) => {
        if (!res.confirm) return;
        clearLogin();
        wx.showToast({ title: '已退出登录', icon: 'success' });
        setTimeout(() => wx.switchTab({ url: '/pages/mine/index/index' }), 500);
      }
    });
  },

  goMine() {
    wx.switchTab({ url: '/pages/mine/index/index' });
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      this.goMine();
    }
  }
});
