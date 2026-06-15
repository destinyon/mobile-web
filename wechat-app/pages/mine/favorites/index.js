const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

function normalizeFavorite(item) {
  const feedType = item.feedType || item.targetType || 'NEWS';
  return {
    ...item,
    feedType,
    feedKey: `${feedType}-${item.id}`,
    categoryName: item.categoryName || item.topicName || (feedType === 'POST' ? '球友社区' : '外部新闻'),
    summary: item.summary || item.content || ''
  };
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
    return api.getFavorites()
      .then((data) => this.setData({ items: itemsOf(data).map(normalizeFavorite) }))
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true });
          return;
        }
        this.setData({ error: error.msg || '加载失败' });
      })
      .finally(() => this.setData({ loading: false }));
  },

  hideAuth() {
    this.setData({ showAuth: false });
  },

  afterLogin() {
    this.setData({ showAuth: false });
    this.load();
  }
});
