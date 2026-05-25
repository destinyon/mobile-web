Component({
  properties: {
    item: {
      type: Object,
      value: {}
    }
  },

  methods: {
    onTap() {
      const id = this.data.item.id || this.data.item.postId;
      if (!id) return;
      const target = this.data.item.feedType === 'NEWS'
        ? `/pages/news/detail/index?id=${id}`
        : `/pages/post/detail/index?id=${id}`;
      wx.navigateTo({ url: target });
    }
  }
});
