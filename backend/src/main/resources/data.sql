MERGE INTO users (id, openid, nickname, avatar_url, phone, age, play_years, gender, role, status)
KEY(id) VALUES
(1, 'demo-openid-user', 'why', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/avatar/demo-user.png', '177xxxxxxx', 18, 5, '男', 'USER', 'ACTIVE'),
(2, 'demo-openid-admin', '熊羽', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/avatar/admin.png', '188xxxxxxx', 28, 12, '女', 'ADMIN', 'ACTIVE');

MERGE INTO categories (id, name, sort_no, status) KEY(id) VALUES
(1, '赛事资讯', 1, 'ACTIVE'),
(2, '训练技巧', 2, 'ACTIVE'),
(3, '球友社区', 3, 'ACTIVE');

MERGE INTO banners (id, title, image_url, link_type, link_target, sort_no, status) KEY(id) VALUES
(1, '全英公开赛焦点战', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/312f8966-5d01-4db8-9618-fd653bcbd731.jpg', 'NEWS', '1', 1, 'ACTIVE'),
(2, '中杆弹性与控球体验', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/fe50236b-e9e8-4f1b-868f-73af881d0af0.jpg', 'NEWS', '2', 2, 'ACTIVE');

MERGE INTO news (id, category_id, user_id, title, cover_url, summary, author, content, media_url, media_type, view_count, like_count, favorite_count, comment_count, status)
KEY(id) VALUES
(1, 1, 2, '全英公开赛国羽双面回弹，攻防节奏明显提速', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/312f8966-5d01-4db8-9618-fd653bcbd731.jpg', '全英公开赛进入关键轮次，国羽男双在发接发和后二拍衔接上明显提升。', '熊羽球', '<p>全英公开赛进入关键轮次，国羽男双在发接发和后二拍衔接上明显提升。</p><p>教练组在赛后复盘中提到，轮转速度和连续进攻质量是本场胜负重点。</p><img src="https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/3785e162-8327-4f67-b8a8-fa56e9c449d7.jpg" />', NULL, 'IMAGE', 201, 222, 18, 2, 'PUBLISHED'),
(2, 2, 2, '三款高端进攻拍横向体验：中杆弹性与连贯性成最大差异', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/fe50236b-e9e8-4f1b-868f-73af881d0af0.jpg', '从连续点杀、平抽挡和后场突击三个场景比较高端进攻拍。', '装备实验室', '<p>本次体验选择三款高端进攻拍，从连续点杀、平抽挡和后场突击三个场景比较。</p><p>中杆弹性和恢复速度直接影响多拍连贯质量。</p>', NULL, 'IMAGE', 1000, 1000, 31, 1, 'PUBLISHED'),
(3, 3, 1, '周末双打对抗赛报名开启，社区俱乐部将尝试分级编排', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/2025d148-afc4-48fa-83bc-48c6ae0c1289.jpg', '社区球友俱乐部将按水平分组，降低新手参与门槛。', '俱乐部运营组', '<p>本周末双打对抗赛开放报名，社区俱乐部将按水平分组，降低新手参与门槛。</p><p>报名成功后可在我的发布中查看状态。</p>', NULL, 'IMAGE', 2017, 2222, 52, 0, 'PUBLISHED');

MERGE INTO comments (id, target_type, target_id, user_id, parent_id, content, status) KEY(id) VALUES
(1, 'NEWS', 1, 1, NULL, '这一场男双第二拍的处理明显更坚决，尤其是接发后直接压后场那几拍。', 'PUBLISHED'),
(2, 'NEWS', 1, 2, 1, '同感，后半段网前抢点很关键。', 'PUBLISHED'),
(3, 'NEWS', 2, 1, NULL, '中杆反馈差异确实会影响连续进攻，想看更多实测数据。', 'PUBLISHED');

MERGE INTO favorites (id, user_id, target_type, target_id) KEY(id) VALUES
(1, 1, 'NEWS', 1),
(2, 1, 'NEWS', 2);