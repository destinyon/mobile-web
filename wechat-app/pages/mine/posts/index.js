const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

Page({
  data: {
    items: [],
    draft: null,
    loading: true,
    error: '',
    showAuth: false
  },

  onShow() {
    this.load();
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/mine/index/index' });
    }
  },

  load() {
    this.setData({ loading: true, error: '' });
    return api.getPosts()
      .then((data) => {
        const all = itemsOf(data);
        this.setData({
          draft: all.find((item) => item.status === 'DRAFT') || null,
          items: all.filter((item) => item.status !== 'DRAFT')
        });
      })
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true });
          return;
        }
        this.setData({ error: error.msg || '加载失败' });
      })
      .finally(() => this.setData({ loading: false }));
  },

  goDraft() {
    wx.switchTab({ url: '/pages/news/editor/index' });
  },

  hideAuth() {
    this.setData({ showAuth: false });
  },

  afterLogin() {
    this.setData({ showAuth: false });
    this.load();
  }
});
