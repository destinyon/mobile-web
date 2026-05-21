const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

Page({
  data: {
    form: {
      title: '',
      coverUrl: '',
      summary: '',
      content: ''
    },
    submitting: false,
    showAuth: false,
    pendingSubmit: false
  },

  onField(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [`form.${key}`]: event.detail.value });
  },

  submit() {
    const form = this.data.form;
    if (!form.title.trim() || !form.content.trim()) {
      wx.showToast({ title: '请填写标题和正文', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    api.createNews({
      title: form.title.trim(),
      coverUrl: form.coverUrl.trim(),
      summary: form.summary.trim(),
      content: form.content.trim()
    }).then(() => {
      wx.showToast({ title: '发布成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 600);
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
