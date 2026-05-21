const api = require('../../services/api');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

Page({
  data: {
    keyword: '',
    newsList: [],
    loading: false,
    searched: false,
    error: ''
  },

  onInput(event) {
    this.setData({ keyword: event.detail.value });
  },

  onSearch() {
    const keyword = this.data.keyword.trim();
    if (!keyword) {
      wx.showToast({ title: '请输入关键词', icon: 'none' });
      return;
    }
    this.setData({ loading: true, searched: true, error: '' });
    api.getNews({ keyword, page: 1, pageSize: 20 })
      .then((data) => this.setData({ newsList: itemsOf(data) }))
      .catch((error) => this.setData({ error: error.msg || '搜索失败' }))
      .finally(() => this.setData({ loading: false }));
  }
});
