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
      wx.navigateTo({ url: `/pages/post/detail/index?id=${id}` });
    }
  }
});
