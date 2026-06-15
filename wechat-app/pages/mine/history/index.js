const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

Page({
  data: {
    items: [],
    loading: true,
    error: '',
    showAuth: false
  },

  onLoad() {
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
    return api.getHistory()
      .then((data) => this.setData({ items: itemsOf(data) }))
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true });
          return;
        }
        this.setData({ error: error.msg || '加载失败' });
      })
      .finally(() => this.setData({ loading: false }));
  },

  goTarget(event) {
    const item = event.currentTarget.dataset.item;
    if (!item || !item.targetId) return;
    const path = item.targetType === 'POST' ? '/pages/post/detail/index' : '/pages/news/detail/index';
    wx.navigateTo({ url: `${path}?id=${item.targetId}` });
  },

  hideAuth() {
    this.setData({ showAuth: false });
  },

  afterLogin() {
    this.setData({ showAuth: false });
    this.load();
  }
});
