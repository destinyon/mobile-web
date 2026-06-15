const api = require('../../services/api');

function pageItems(data) {
  if (Array.isArray(data)) return data;
  return data.items || data.records || data.list || [];
}

function totalOf(data, fallback) {
  return data && typeof data.total === 'number' ? data.total : fallback;
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

Page({
  data: {
    banners: [],
    filters: [
      { type: 'all', label: '全部' },
      { type: 'news', label: '外部新闻' }
    ],
    feedItems: [],
    activeFilter: 'all',
    page: 1,
    pageSize: 8,
    total: 0,
    hasMore: true,
    loading: true,
    error: ''
  },

  onLoad() {
    this.loadAll();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.loadFeed(false);
  },

  loadAll() {
    this.setData({ loading: true, error: '', page: 1, hasMore: true });
    return Promise.all([api.getBanners(), api.getTopics(), this.loadFeed(true)])
      .then(([banners, topics]) => {
        const topicFilters = pageItems(topics).map((item) => ({
          type: `topic-${item.id}`,
          label: item.name,
          topicId: item.id
        }));
        this.setData({
          banners: pageItems(banners),
          filters: [
            { type: 'all', label: '全部' },
            { type: 'news', label: '外部新闻' }
          ].concat(topicFilters)
        });
      })
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  loadFeed(reset) {
    const page = reset ? 1 : this.data.page;
    const filter = this.data.filters.find((item) => item.type === this.data.activeFilter) || this.data.filters[0];
    const params = { page, pageSize: this.data.pageSize, sort: 'latest' };

    let request;
    if (filter.type === 'news') {
      request = api.getNews(params).then((data) => {
        const items = pageItems(data);
        return {
          items: items.map(asNewsItem),
          total: totalOf(data, items.length),
          hasMore: items.length >= this.data.pageSize
        };
      });
    } else if (filter.topicId) {
      request = api.getCommunityPosts({ ...params, topicId: filter.topicId }).then((data) => {
        const items = pageItems(data);
        return {
          items: items.map(asPostItem),
          total: totalOf(data, items.length),
          hasMore: items.length >= this.data.pageSize
        };
      });
    } else {
      request = Promise.all([api.getNews(params), api.getCommunityPosts(params)]).then(([news, posts]) => {
        const newsItems = pageItems(news);
        const postItems = pageItems(posts);
        return {
          items: newsItems.map(asNewsItem)
            .concat(postItems.map(asPostItem))
            .sort((left, right) => right.sortTime - left.sortTime),
          total: totalOf(news, newsItems.length) + totalOf(posts, postItems.length),
          hasMore: newsItems.length >= this.data.pageSize || postItems.length >= this.data.pageSize
        };
      });
    }

    return request.then((data) => {
      this.setData({
        feedItems: reset ? data.items : this.data.feedItems.concat(data.items),
        page: page + 1,
        total: data.total,
        hasMore: data.hasMore
      });
    });
  },

  selectFilter(event) {
    this.setData({
      activeFilter: event.currentTarget.dataset.type || 'all',
      loading: true,
      error: '',
      page: 1,
      hasMore: true
    });
    this.loadFeed(true)
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/index' });
  },

  onBannerTap(event) {
    const item = event.currentTarget.dataset.item || {};
    const id = item.linkType === 'NEWS' ? item.linkTarget : (item.newsId || item.targetId || item.id);
    if (id) {
      wx.navigateTo({ url: `/pages/news/detail/index?id=${id}` });
    }
  }
});
