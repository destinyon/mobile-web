const api = require('../../services/api');

function itemsOf(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

function timeValue(value) {
  if (!value) return 0;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function asNewsItem(item) {
  return {
    ...item,
    feedType: 'NEWS',
    feedKey: `NEWS-${item.id}`,
    categoryName: '外部新闻',
    status: '',
    updatedAt: item.updatedAt || item.createdAt || '',
    sortTime: timeValue(item.updatedAt || item.createdAt)
  };
}

function asPostItem(item) {
  return {
    ...item,
    feedType: 'POST',
    feedKey: `POST-${item.id}`,
    categoryName: item.topicName || item.categoryName || '球友社区',
    updatedAt: item.updatedAt || item.createdAt || '',
    sortTime: timeValue(item.updatedAt || item.createdAt)
  };
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function withHighlight(items, keyword) {
  const trimmed = keyword.trim();
  if (!trimmed) return items;
  const pattern = new RegExp(escapeRegExp(trimmed), 'ig');
  return items.map((item) => ({
    ...item,
    highlightedTitle: escapeHtml(item.title).replace(pattern, (match) => `<span style="color:#d92d20;">${match}</span>`)
  }));
}

Page({
  data: {
    keyword: '',
    filters: [
      { type: 'all', label: '全部' },
      { type: 'news', label: '外部新闻' }
    ],
    activeFilter: 'all',
    items: [],
    loading: false,
    searched: false,
    error: ''
  },

  onLoad() {
    api.getTopics()
      .then((topics) => {
        const topicFilters = itemsOf(topics).map((item) => ({
          type: `topic-${item.id}`,
          label: item.name,
          topicId: item.id
        }));
        this.setData({
          filters: [
            { type: 'all', label: '全部' },
            { type: 'news', label: '外部新闻' }
          ].concat(topicFilters)
        });
      })
      .catch((error) => wx.showToast({ title: error.msg || '类别加载失败', icon: 'none' }));
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/home/index' });
    }
  },

  onInput(event) {
    this.setData({ keyword: event.detail.value });
  },

  selectFilter(event) {
    this.setData({ activeFilter: event.currentTarget.dataset.type || 'all' });
    if (this.data.searched || this.data.keyword.trim()) {
      this.onSearch();
    }
  },

  onSearch() {
    const filter = this.data.filters.find((item) => item.type === this.data.activeFilter) || this.data.filters[0];
    const keyword = this.data.keyword.trim();
    const params = {
      keyword: keyword || undefined,
      page: 1,
      pageSize: 30,
      sort: 'latest'
    };
    this.setData({ loading: true, searched: true, error: '' });

    const request = filter.type === 'news'
      ? api.getNews(params).then((data) => itemsOf(data).map(asNewsItem))
      : filter.topicId
        ? api.getCommunityPosts({ ...params, topicId: filter.topicId }).then((data) => itemsOf(data).map(asPostItem))
        : Promise.all([api.getNews(params), api.getCommunityPosts(params)]).then(([news, posts]) => itemsOf(news).map(asNewsItem)
          .concat(itemsOf(posts).map(asPostItem))
          .sort((left, right) => right.sortTime - left.sortTime));

    request
      .then((items) => this.setData({ items: withHighlight(items, keyword) }))
      .catch((error) => this.setData({ error: error.msg || '搜索失败' }))
      .finally(() => this.setData({ loading: false }));
  }
});
