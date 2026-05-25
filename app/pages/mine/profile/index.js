const api = require('../../../services/api');
const { isAuthError } = require('../../../utils/auth');

const ageOptions = Array.from({ length: 63 }, (_, index) => String(index + 8));
const playYearOptions = Array.from({ length: 41 }, (_, index) => String(index));
const genderOptions = ['男', '女', '不透露'];

Page({
  data: {
    form: {
      nickname: '',
      avatarUrl: '',
      phone: '',
      age: '',
      playYears: '',
      gender: ''
    },
    ageOptions,
    playYearOptions,
    genderOptions,
    avatarKey: '',
    loading: true,
    saving: false,
    uploading: false,
    showAuth: false
  },

  onLoad() {
    this.load();
  },

  load() {
    this.setData({ loading: true });
    return api.getProfile()
      .then((profile) => this.setData({
        form: {
          nickname: profile.nickname || '',
          avatarUrl: profile.avatarUrl || '',
          phone: profile.phone || '',
          age: profile.age || '',
          playYears: profile.playYears || '',
          gender: profile.gender || ''
        }
      }))
      .catch((error) => {
        if (isAuthError(error)) {
          this.setData({ showAuth: true });
          return;
        }
        wx.showToast({ title: error.msg || '资料加载失败', icon: 'none' });
      })
      .finally(() => this.setData({ loading: false }));
  },

  onField(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [`form.${key}`]: event.detail.value });
  },

  onPickerChange(event) {
    const key = event.currentTarget.dataset.key;
    const options = this.data[event.currentTarget.dataset.options] || [];
    const value = options[Number(event.detail.value)] || '';
    this.setData({ [`form.${key}`]: value });
  },

  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file) return;
        this.setData({ uploading: true });
        api.uploadFile(file.tempFilePath)
          .then((result) => this.setData({
            'form.avatarUrl': result.url,
            avatarKey: result.objectKey || ''
          }))
          .catch((error) => wx.showToast({ title: error.msg || '头像上传失败', icon: 'none' }))
          .finally(() => this.setData({ uploading: false }));
      }
    });
  },

  save() {
    const form = this.data.form;
    if (!form.nickname.trim()) {
      wx.showToast({ title: '请填写昵称', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    api.updateProfile({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl,
      phone: form.phone,
      age: form.age === '' ? undefined : Number(form.age),
      playYears: form.playYears === '' ? undefined : Number(form.playYears),
      gender: form.gender
    }).then((profile) => {
      const app = getApp();
      app.setAuth(wx.getStorageSync('token'), profile);
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 600);
    }).catch((error) => {
      if (isAuthError(error)) {
        this.setData({ showAuth: true });
        return;
      }
      wx.showToast({ title: error.msg || '保存失败', icon: 'none' });
    }).finally(() => this.setData({ saving: false }));
  },

  goBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/mine/index/index' });
    }
  },

  hideAuth() {
    this.setData({ showAuth: false });
  },

  afterLogin() {
    this.setData({ showAuth: false });
    this.load();
  }
});
