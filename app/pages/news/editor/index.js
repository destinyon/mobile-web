const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

Page({
  data: {
    form: {
      topicId: '',
      title: '',
      content: '',
      images: []
    },
    topics: [],
    imageInput: '',
    submitting: false,
    showAuth: false,
    pendingSubmit: false
  },

  onLoad() {
    this.loadTopics();
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
  },

  loadTopics() {
    return api.getTopics()
      .then((topics) => {
        const list = Array.isArray(topics) ? topics : [];
        this.setData({
          topics: list,
          'form.topicId': this.data.form.topicId || (list[0] && list[0].id) || ''
        });
      })
      .catch((error) => wx.showToast({ title: error.msg || '话题加载失败', icon: 'none' }));
  },

  onField(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [`form.${key}`]: event.detail.value });
  },

  onImageInput(event) {
    this.setData({ imageInput: event.detail.value });
  },

  selectTopic(event) {
    this.setData({ 'form.topicId': event.currentTarget.dataset.id });
  },

  addImage() {
    const url = this.data.imageInput.trim();
    if (!url) {
      wx.showToast({ title: '请输入图片 URL', icon: 'none' });
      return;
    }
    this.setData({
      'form.images': this.data.form.images.concat(url),
      imageInput: ''
    });
  },

  removeImage(event) {
    const index = event.currentTarget.dataset.index;
    const images = this.data.form.images.slice();
    images.splice(index, 1);
    this.setData({ 'form.images': images });
  },

  submit() {
    const form = this.data.form;
    if (!form.topicId) {
      wx.showToast({ title: '请选择话题', icon: 'none' });
      return;
    }
    if (!form.title.trim() || !form.content.trim()) {
      wx.showToast({ title: '请填写标题和正文', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    api.createPost({
      topicId: Number(form.topicId),
      title: form.title.trim(),
      content: form.content.trim(),
      images: form.images
    }).then((post) => {
      wx.showToast({ title: '发布成功', icon: 'success' });
      this.setData({
        form: { topicId: form.topicId, title: '', content: '', images: [] },
        imageInput: ''
      });
      setTimeout(() => wx.navigateTo({ url: `/pages/post/detail/index?id=${post.id}` }), 600);
    }).catch((error) => {
      if (isAuthError(error)) {
        this.setData({ showAuth: true, pendingSubmit: true });
        return;
      }
      wx.showToast({ title: error.msg || '发布失败', icon: 'none' });
    }).finally(() => {
      this.setData({ submitting: false });
    });
  },

  hideAuth() {
    this.setData({ showAuth: false, pendingSubmit: false });
  },

  afterLogin() {
    const shouldSubmit = this.data.pendingSubmit;
    this.setData({ showAuth: false, pendingSubmit: false });
    if (shouldSubmit) {
      this.submit();
    }
  }
});
