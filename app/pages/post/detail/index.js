const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

Page({
  data: {
    id: '',
    detail: {},
    comments: [],
    commentContent: '',
    replyTarget: {},
    loading: true,
    submitting: false,
    error: '',
    showAuth: false,
    pendingAction: ''
  },

  onLoad(options) {
    this.setData({ id: options.id || '' });
    this.loadDetail();
  },

  loadDetail() {
    if (!this.data.id) {
      this.setData({ loading: false, error: '缺少帖子 ID' });
      return Promise.resolve();
    }
    this.setData({ loading: true, error: '' });
    return api.getPostDetail(this.data.id)
      .then((detail) => this.setData({ detail, comments: detail.comments || [] }))
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/community/index' });
    }
  },

  onLike() {
    this.runProtected('like');
  },

  onFavorite() {
    this.runProtected('favorite');
  },

  onCommentInput(event) {
    this.setData({ commentContent: event.detail.value });
  },

  startReply(event) {
    this.setData({ replyTarget: event.currentTarget.dataset.comment || {} });
  },

  cancelReply() {
    this.setData({ replyTarget: {} });
  },

  submitComment() {
    if (!this.data.commentContent.trim()) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' });
      return;
    }
    this.runProtected('comment');
  },

  runProtected(action) {
    const taskMap = {
      like: () => api.likePost(this.data.id),
      favorite: () => this.data.detail.favorited
        ? api.unfavoritePost(this.data.id)
        : api.favoritePost(this.data.id),
      comment: () => {
        this.setData({ submitting: true });
        return api.createComment({
          targetType: 'POST',
          targetId: this.data.id,
          parentId: this.data.replyTarget.id || undefined,
          content: this.data.commentContent.trim()
        });
      }
    };

    taskMap[action]()
      .then(() => {
        wx.showToast({ title: '操作成功', icon: 'success' });
        this.setData({ commentContent: '', replyTarget: {} });
        this.loadDetail();
      })
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true, pendingAction: action });
          return;
        }
        wx.showToast({ title: error.msg || '操作失败', icon: 'none' });
      })
      .finally(() => this.setData({ submitting: false }));
  },

  hideAuth() {
    this.setData({ showAuth: false, pendingAction: '' });
  },

  afterLogin() {
    const action = this.data.pendingAction;
    this.setData({ showAuth: false, pendingAction: '' });
    if (action) {
      this.runProtected(action);
    }
  }
});
