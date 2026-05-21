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

  submitComment() {
    if (!this.data.commentContent.trim()) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' });
      return;
    }
    this.runProtected('comment');
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
        return api.createComment({ targetType: 'NEWS', targetId: this.data.id, content: this.data.commentContent.trim() });
      }
    };

    taskMap[action]()
      .then(() => {
        wx.showToast({ title: '操作成功', icon: 'success' });
        this.setData({ commentContent: '' });
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
