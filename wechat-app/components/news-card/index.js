Component({
  properties: {
    item: {
      type: Object,
      value: {}
    }
  },

  methods: {
    onTap() {
      const id = this.data.item.id || this.data.item.newsId;
      if (!id) return;
      wx.navigateTo({ url: `/pages/news/detail/index?id=${id}` });
    }
  }
});
