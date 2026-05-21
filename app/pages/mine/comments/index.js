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

  load() {
    this.setData({ loading: true, error: '' });
    return api.getComments()
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

  goNews(event) {
    const id = event.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: `/pages/news/detail/index?id=${id}` });
  },

  hideAuth() {
    this.setData({ showAuth: false });
  },

  afterLogin() {
    this.setData({ showAuth: false });
    this.load();
  }
});
