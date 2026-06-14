# 羽球在线 App

这是从现有原生微信小程序延伸出的 uni-app App 端，不修改 `app/` 小程序代码。

## 运行

1. 用 HBuilderX 打开 `mobile-app/`。
2. 手机和电脑连接同一个热点。
3. 启动后端：

```powershell
cd ../backend
mvn spring-boot:run
```

4. App 调试接口默认指向 `http://10.27.248.95:8080`。

正式发布前，把 `utils/config.js` 中的 `API_BASE_URL` 改成 `https://badminton-celeste.top`，并确保域名能从公网访问且配置 HTTPS。

## 权限

已在 `manifest.json` 中声明相机、相册和网络权限，用于头像、封面和帖子图片上传。
