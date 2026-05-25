const api = require('../../../services/api');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

Page({
  data: {
    title: '新闻列表',
    categoryId: '',
    newsList: [],
    page: 1,
    pageSize: 10,
    total: 0,
    hasMore: true,
    loading: false,
    error: ''
  },

  onLoad(options) {
    this.setData({
      title: options.title || '新闻列表',
      categoryId: options.categoryId || ''
    });
    this.reload();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.load(false);
  },

  reload() {
    this.setData({ page: 1, hasMore: true, newsList: [], error: '' });
    return this.load(true);
  },

  load(reset) {
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });
    return api.getNews({
      page,
      pageSize: this.data.pageSize,
      categoryId: this.data.categoryId || undefined
    }).then((data) => {
      const items = itemsOf(data);
      this.setData({
        newsList: reset ? items : this.data.newsList.concat(items),
        page: page + 1,
        total: data.total || items.length,
        hasMore: items.length >= this.data.pageSize
      });
    }).catch((error) => {
      this.setData({ error: error.msg || '加载失败' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
