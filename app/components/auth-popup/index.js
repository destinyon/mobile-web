const api = require('../../services/api');
const { saveLogin } = require('../../utils/auth');

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    }
  },

  data: {
    loading: false
  },

  methods: {
    onCancel() {
      this.triggerEvent('cancel');
    },

    onLogin() {
      if (this.data.loading) return;
      this.setData({ loading: true });
      wx.login({
        success: ({ code }) => {
          if (!code) {
            this.finishLogin('微信登录失败，请稍后重试');
            return;
          }
          api.wxLogin({ code })
            .then((data) => {
              const token = data.token || data.accessToken || '';
              const userInfo = data.user || data.userInfo || data.profile || null;
              if (!token) {
                this.finishLogin('登录响应缺少 token');
                return;
              }
              saveLogin(token, userInfo);
              this.setData({ loading: false });
              this.triggerEvent('success', { token, userInfo });
            })
            .catch((error) => this.finishLogin(error.msg || '登录失败，请稍后重试'));
        },
        fail: () => this.finishLogin('无法唤起微信登录')
      });
    },

    finishLogin(message) {
      this.setData({ loading: false });
      wx.showToast({ title: message, icon: 'none' });
    }
  }
});
