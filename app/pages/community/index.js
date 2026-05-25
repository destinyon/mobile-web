const api = require('../../services/api');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

Page({
  data: {
    topics: [],
    items: [],
    keyword: '',
    activeTopic: '',
    sort: 'latest',
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: true,
    error: ''
  },

  onLoad() {
    this.loadAll();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 });
    }
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.loadPosts(false);
  },

  loadAll() {
    this.setData({ loading: true, error: '' });
    return Promise.all([api.getTopics(), this.loadPosts(true)])
      .then(([topics]) => this.setData({ topics: itemsOf(topics) }))
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  reload() {
    this.setData({ page: 1, hasMore: true, items: [], error: '', loading: true });
    return this.loadPosts(true)
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  loadPosts(reset) {
    const page = reset ? 1 : this.data.page;
    return api.getCommunityPosts({
      page,
      pageSize: this.data.pageSize,
      topicId: this.data.activeTopic || undefined,
      keyword: this.data.keyword.trim() || undefined,
      sort: this.data.sort
    }).then((data) => {
      const items = itemsOf(data);
      this.setData({
        items: reset ? items : this.data.items.concat(items),
        page: page + 1,
        hasMore: items.length >= this.data.pageSize
      });
    });
  },

  onKeyword(event) {
    this.setData({ keyword: event.detail.value });
  },

  selectTopic(event) {
    this.setData({ activeTopic: event.currentTarget.dataset.id || '' });
    this.reload();
  },

  selectSort(event) {
    this.setData({ sort: event.currentTarget.dataset.sort || 'latest' });
    this.reload();
  },

  goPublish() {
    wx.switchTab({ url: '/pages/news/editor/index' });
  }
});
