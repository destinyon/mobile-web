const api = require('../../services/api');

function pageItems(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

Page({
  data: {
    banners: [],
    categories: [],
    newsList: [],
    activeCategory: '',
    page: 1,
    pageSize: 10,
    total: 0,
    hasMore: true,
    loading: true,
    error: ''
  },

  onLoad() {
    this.loadAll();
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.loadNews(false);
  },

  loadAll() {
    this.setData({ loading: true, error: '', page: 1, hasMore: true });
    return Promise.all([api.getBanners(), api.getCategories(), this.loadNews(true)])
      .then(([banners, categories]) => {
        this.setData({
          banners: pageItems(banners),
          categories: pageItems(categories)
        });
      })
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  loadNews(reset) {
    const page = reset ? 1 : this.data.page;
    return api.getNews({
      page,
      pageSize: this.data.pageSize,
      categoryId: this.data.activeCategory || undefined
    }).then((data) => {
      const items = pageItems(data);
      const total = data.total || items.length;
      this.setData({
        newsList: reset ? items : this.data.newsList.concat(items),
        page: page + 1,
        total,
        hasMore: items.length >= this.data.pageSize
      });
    });
  },

  selectCategory(event) {
    this.setData({ activeCategory: event.currentTarget.dataset.id || '' });
    this.setData({ loading: true, error: '', page: 1, hasMore: true });
    this.loadNews(true)
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/index' });
  },

  goPublish() {
    wx.navigateTo({ url: '/pages/news/editor/index' });
  },

  onBannerTap(event) {
    const item = event.currentTarget.dataset.item || {};
    const id = item.newsId || item.targetId || item.id;
    if (id) {
      wx.navigateTo({ url: `/pages/news/detail/index?id=${id}` });
    }
  }
});
