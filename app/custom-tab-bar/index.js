Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/home/index', text: '首页', icon: '⌂' },
      { pagePath: '/pages/news/editor/index', text: '发布', icon: '+', center: true },
      { pagePath: '/pages/mine/index/index', text: '我的', icon: '☻' }
    ]
  },

  methods: {
    switchTab(event) {
      const index = event.currentTarget.dataset.index;
      const item = this.data.list[index];
      if (!item) return;
      wx.switchTab({ url: item.pagePath });
    }
  }
});
