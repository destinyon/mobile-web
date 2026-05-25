const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

const EMPTY_FORM = {
  topicId: '',
  title: '',
  coverUrl: '',
  coverKey: '',
  content: '',
  images: []
};

Page({
  data: {
    mode: 'draft',
    postId: '',
    form: { ...EMPTY_FORM },
    topics: [],
    autosaveTimer: null,
    savingDraft: false,
    submitting: false,
    uploadingCover: false,
    uploadingImage: false,
    showAuth: false,
    pendingSubmit: false,
    dragIndex: -1,
    dragTargetIndex: -1,
    dragStyle: ''
  },

  imageDragRects: [],
  imageTouchStart: null,

  onLoad(options) {
    this.setData({
      mode: options.id ? 'edit' : 'draft',
      postId: options.id || ''
    });
    this.loadTopics().then(() => {
      if (options.id) {
        return this.loadPost(options.id);
      }
      return this.loadDraft();
    });
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
    const editingPostId = wx.getStorageSync('editingPostId');
    if (editingPostId && this.data.postId !== editingPostId) {
      wx.removeStorageSync('editingPostId');
      this.setData({ mode: 'edit', postId: editingPostId });
      this.loadTopics().then(() => this.loadPost(editingPostId));
      return;
    }
    if (editingPostId) {
      wx.removeStorageSync('editingPostId');
    }
    if (!this.data.postId && !editingPostId) {
      this.setData({ mode: 'draft' });
      this.loadDraft();
    }
  },

  onUnload() {
    if (this.data.autosaveTimer) {
      clearTimeout(this.data.autosaveTimer);
    }
    if (this.data.mode === 'draft') {
      this.saveDraft();
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

  loadPost(id) {
    return api.getPostDetail(id)
      .then((post) => this.setData({
        form: {
          topicId: post.topicId,
          title: post.title || '',
          coverUrl: post.coverUrl || '',
          coverKey: '',
          content: post.content || '',
          images: (post.images || []).map((url) => ({ url, objectKey: '' }))
        }
      }))
      .catch((error) => wx.showToast({ title: error.msg || '帖子加载失败', icon: 'none' }));
  },

  loadDraft() {
    return api.getPostDraft()
      .then((draft) => {
        if (!draft) return;
        this.setData({
          form: {
            topicId: draft.topicId || this.data.form.topicId,
            title: draft.title === '未命名草稿' ? '' : (draft.title || ''),
            coverUrl: draft.coverUrl || '',
            coverKey: '',
            content: draft.content || '',
            images: (draft.images || []).map((url) => ({ url, objectKey: '' }))
          }
        });
      })
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true });
          return;
        }
        wx.showToast({ title: error.msg || '草稿加载失败', icon: 'none' });
      });
  },

  onField(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [`form.${key}`]: event.detail.value });
    this.scheduleDraftSave();
  },

  selectTopic(event) {
    this.setData({ 'form.topicId': event.currentTarget.dataset.id });
    this.scheduleDraftSave();
  },

  chooseCover() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file) return;
        this.setData({ uploadingCover: true });
        api.uploadFile(file.tempFilePath)
          .then((result) => {
            this.setData({
              'form.coverUrl': result.url,
              'form.coverKey': result.objectKey || ''
            });
            this.scheduleDraftSave();
          })
          .catch((error) => wx.showToast({ title: error.msg || '封面上传失败', icon: 'none' }))
          .finally(() => this.setData({ uploadingCover: false }));
      }
    });
  },

  chooseImages() {
    const remaining = 9 - this.data.form.images.length;
    if (remaining <= 0) {
      wx.showToast({ title: '最多上传 9 张配图', icon: 'none' });
      return;
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const files = res.tempFiles || [];
        this.uploadImages(files.map((file) => file.tempFilePath));
      }
    });
  },

  uploadImages(paths) {
    if (!paths.length) return;
    const slots = 9 - this.data.form.images.length;
    if (slots <= 0) {
      wx.showToast({ title: '最多上传 9 张配图', icon: 'none' });
      return;
    }
    this.setData({ uploadingImage: true });
    const uploads = paths.slice(0, slots).map((path) => api.uploadFile(path));
    Promise.all(uploads)
      .then((results) => {
        const next = this.data.form.images.concat(results.map((item) => ({
          url: item.url,
          objectKey: item.objectKey || ''
        })));
        this.setData({ 'form.images': next });
        this.scheduleDraftSave();
      })
      .catch((error) => wx.showToast({ title: error.msg || '配图上传失败', icon: 'none' }))
      .finally(() => this.setData({ uploadingImage: false }));
  },

  removeCover() {
    const key = this.data.form.coverKey;
    if (key) {
      api.deleteUpload(key).catch(() => {});
    }
    this.setData({ 'form.coverUrl': '', 'form.coverKey': '' });
    this.scheduleDraftSave();
  },

  removeImage(event) {
    const index = Number(event.currentTarget.dataset.index);
    const images = this.data.form.images.slice();
    const item = images[index];
    images.splice(index, 1);
    this.setData({ 'form.images': images });
    if (item && item.objectKey) {
      api.deleteUpload(item.objectKey).catch(() => {});
    }
    this.scheduleDraftSave();
  },

  prepareImageDrag(event) {
    const touch = event.touches && event.touches[0];
    if (!touch) return;
    this.imageTouchStart = {
      clientX: touch.clientX,
      clientY: touch.clientY
    };
  },

  startImageDrag(event) {
    const index = Number(event.currentTarget.dataset.index);
    if (Number.isNaN(index)) return;
    const touch = (event.touches && event.touches[0])
      || (event.changedTouches && event.changedTouches[0])
      || this.imageTouchStart;
    this.setData({
      dragIndex: index,
      dragTargetIndex: index,
      dragStyle: 'transform: translate(0px, 0px) scale(1.04);'
    });
    wx.createSelectorQuery()
      .in(this)
      .selectAll('.image-item')
      .boundingClientRect((rects) => {
        this.imageDragRects = rects || [];
        const rect = this.imageDragRects[index];
        if (rect && touch) {
          this.imageTouchStart = {
            clientX: touch.clientX,
            clientY: touch.clientY,
            originLeft: rect.left,
            originTop: rect.top
          };
        }
      })
      .exec();
  },

  moveImageDrag(event) {
    if (this.data.dragIndex < 0 || !this.imageDragRects.length) return;
    const touch = event.touches && event.touches[0];
    if (!touch) return;
    const start = this.imageTouchStart || touch;
    const offsetX = touch.clientX - start.clientX;
    const offsetY = touch.clientY - start.clientY;
    let nextIndex = this.data.dragTargetIndex;
    let nextDistance = Number.MAX_VALUE;
    this.imageDragRects.forEach((rect, index) => {
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const distance = Math.abs(touch.clientX - centerX) + Math.abs(touch.clientY - centerY);
      if (distance < nextDistance) {
        nextDistance = distance;
        nextIndex = index;
      }
    });
    const patch = {
      dragStyle: `transform: translate(${offsetX}px, ${offsetY}px) scale(1.04);`
    };
    if (nextIndex !== this.data.dragTargetIndex) {
      patch.dragTargetIndex = nextIndex;
    }
    this.setData(patch);
  },

  endImageDrag() {
    const { dragIndex, dragTargetIndex } = this.data;
    if (dragIndex < 0 || dragTargetIndex < 0 || dragIndex === dragTargetIndex) {
      this.setData({ dragIndex: -1, dragTargetIndex: -1, dragStyle: '' });
      this.imageDragRects = [];
      this.imageTouchStart = null;
      return;
    }
    const images = this.data.form.images.slice();
    const current = images.splice(dragIndex, 1)[0];
    images.splice(dragTargetIndex, 0, current);
    this.setData({
      'form.images': images,
      dragIndex: -1,
      dragTargetIndex: -1,
      dragStyle: ''
    });
    this.imageDragRects = [];
    this.imageTouchStart = null;
    this.scheduleDraftSave();
  },

  scheduleDraftSave() {
    if (this.data.mode !== 'draft') return;
    if (this.data.autosaveTimer) {
      clearTimeout(this.data.autosaveTimer);
    }
    const timer = setTimeout(() => this.saveDraft(), 800);
    this.setData({ autosaveTimer: timer });
  },

  saveDraft() {
    if (this.data.mode !== 'draft' || this.data.savingDraft) {
      return Promise.resolve();
    }
    const payload = this.payload(false);
    if (!payload.topicId) {
      return Promise.resolve();
    }
    this.setData({ savingDraft: true });
    return api.savePostDraft(payload)
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true });
          return;
        }
        wx.showToast({ title: error.msg || '草稿保存失败', icon: 'none' });
      })
      .finally(() => this.setData({ savingDraft: false }));
  },

  submit() {
    const payload = this.payload(true);
    if (!payload.topicId) {
      wx.showToast({ title: '请选择话题', icon: 'none' });
      return;
    }
    if (!payload.title || !payload.content) {
      wx.showToast({ title: '请填写标题和正文', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    const action = this.data.mode === 'edit'
      ? api.updatePost(this.data.postId, payload)
      : api.savePostDraft(payload).then(() => api.publishPostDraft());
    action.then((post) => {
      wx.showToast({ title: this.data.mode === 'edit' ? '已更新' : '发布成功', icon: 'success' });
      if (this.data.mode === 'draft') {
        this.setData({ form: { ...EMPTY_FORM, topicId: this.data.form.topicId } });
      }
      this.setData({ mode: 'draft', postId: '' });
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

  payload(strict) {
    const form = this.data.form;
    const title = (form.title || '').trim();
    const content = (form.content || '').trim();
    return {
      topicId: form.topicId ? Number(form.topicId) : undefined,
      title: strict ? title : (title || '未命名草稿'),
      coverUrl: (form.coverUrl || '').trim(),
      content: strict ? content : content,
      images: (form.images || []).map((item) => item.url || item).filter(Boolean)
    };
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
