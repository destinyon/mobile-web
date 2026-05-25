const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

function toNodes(detail) {
  const content = detail.content || detail.body || detail.summary || '';
  return content.replace(/\n/g, '<br/>');
}

Page({
  data: {
    id: '',
    detail: {},
    contentNodes: '',
    comments: [],
    commentContent: '',
    replyContent: '',
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
      this.setData({ loading: false, error: '缺少新闻 ID' });
      return Promise.resolve();
    }
    this.setData({ loading: true, error: '' });
    return api.getNewsDetail(this.data.id)
      .then((detail) => {
        this.setData({
          detail,
          contentNodes: toNodes(detail),
          comments: detail.comments || []
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

  startReply(event) {
    this.setData({ replyTarget: event.currentTarget.dataset.comment || {}, replyContent: '' });
  },

  cancelReply() {
    this.setData({ replyTarget: {}, replyContent: '' });
  },

  runProtected(action) {
    const taskMap = {
      like: () => api.likeNews(this.data.id),
      favorite: () => {
        const detail = this.data.detail;
        return detail.favorited || detail.favorite
          ? api.unfavoriteNews(this.data.id)
          : api.favoriteNews(this.data.id);
      },
      comment: () => {
        this.setData({ submitting: true });
        return api.createComment({
          targetType: 'NEWS',
          targetId: this.data.id,
          content: this.data.commentContent.trim()
        });
      },
      reply: () => {
        this.setData({ submitting: true });
        return api.createComment({
          targetType: 'NEWS',
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
    if (action) {
      this.runProtected(action);
    }
  }
});
