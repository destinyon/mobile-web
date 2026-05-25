const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

Page({
  data: {
    id: '',
    detail: {},
    comments: [],
    commentContent: '',
    replyContent: '',
    replyTarget: {},
    loading: true,
    submitting: false,
    error: '',
    showAuth: false,
    pendingAction: '',
    isOwner: false
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
      .then((detail) => {
        const userInfo = wx.getStorageSync('userInfo') || {};
        this.setData({
          detail,
          comments: detail.comments || [],
          isOwner: Boolean(userInfo.id && detail.userId === userInfo.id)
        });
      })
      .catch((error) => this.setData({ error: error.msg || '加载失败' }))
      .finally(() => this.setData({ loading: false }));
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/home/index' });
    }
  },

  goEdit() {
    wx.setStorageSync('editingPostId', this.data.id);
    wx.switchTab({ url: '/pages/news/editor/index' });
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

  onReplyInput(event) {
    this.setData({ replyContent: event.detail.value });
  },

  startReply(event) {
    this.setData({ replyTarget: event.currentTarget.dataset.comment || {}, replyContent: '' });
  },

  cancelReply() {
    this.setData({ replyTarget: {}, replyContent: '' });
  },

  submitComment() {
    if (!this.data.commentContent.trim()) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' });
      return;
    }
    this.runProtected('comment');
  },

  submitReply() {
    if (!this.data.replyContent.trim()) {
      wx.showToast({ title: '请输入回复内容', icon: 'none' });
      return;
    }
    this.runProtected('reply');
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
          content: this.data.commentContent.trim()
        });
      },
      reply: () => {
        this.setData({ submitting: true });
        return api.createComment({
          targetType: 'POST',
          targetId: this.data.id,
          parentId: this.data.replyTarget.id,
          content: this.data.replyContent.trim()
        });
      }
    };

    taskMap[action]()
      .then((result) => {
        if (action === 'like' || action === 'favorite') {
          this.setData({
            'detail.likeCount': result.likeCount,
            'detail.favoriteCount': result.favoriteCount,
            'detail.liked': result.liked,
            'detail.favorited': result.favorited
          });
          wx.showToast({ title: action === 'like' ? (result.liked ? '已点赞' : '已取消点赞') : (result.favorited ? '已收藏' : '已取消收藏'), icon: 'none' });
          return;
        }
        wx.showToast({ title: '已发布', icon: 'success' });
        this.setData({ commentContent: '', replyContent: '', replyTarget: {} });
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
    this.loadDetail();
    if (action) {
      this.runProtected(action);
    }
  }
});
