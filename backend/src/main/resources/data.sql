INSERT INTO users (id, openid, nickname, avatar_url, phone, age, play_years, gender, role, status)
VALUES
(1, 'demo-openid-user', 'why', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/avatar/demo-user.png', '177xxxxxxx', 18, 5, '男', 'USER', 'ACTIVE'),
(2, 'demo-openid-admin', '熊羽', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/avatar/admin.png', '188xxxxxxx', 28, 12, '女', 'ADMIN', 'ACTIVE')
ON DUPLICATE KEY UPDATE
nickname = VALUES(nickname), avatar_url = VALUES(avatar_url), phone = VALUES(phone), age = VALUES(age),
play_years = VALUES(play_years), gender = VALUES(gender), role = VALUES(role), status = VALUES(status);

INSERT INTO categories (id, name, sort_no, status)
VALUES
(1, '赛事讨论', 1, 'ACTIVE'),
(2, '社区交流', 2, 'ACTIVE'),
(3, '羽球装备', 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_no = VALUES(sort_no), status = VALUES(status);

INSERT INTO topics (id, name, description, sort_no, status)
VALUES
(1, '赛事讨论', '围绕比赛、赛程和球员表现的投稿', 1, 'ACTIVE'),
(2, '社区交流', '球友心得、训练记录和约球交流', 2, 'ACTIVE'),
(3, '羽球装备', '球拍、球线、球鞋和穿搭建议', 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), sort_no = VALUES(sort_no), status = VALUES(status);

INSERT INTO banners (id, title, image_url, link_type, link_target, sort_no, status)
VALUES
(1, '羽球在线新闻更新', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/312f8966-5d01-4db8-9618-fd653bcbd731.jpg', 'PAGE', '/pages/news/list/index', 1, 'ACTIVE'),
(2, '羽毛球训练与社区', 'https://mobile-web-design.oss-cn-beijing.aliyuncs.com/uploads/fe50236b-e9e8-4f1b-868f-73af881d0af0.jpg', 'PAGE', '/pages/home/index', 2, 'ACTIVE')
ON DUPLICATE KEY UPDATE
title = VALUES(title), image_url = VALUES(image_url), link_type = VALUES(link_type),
link_target = VALUES(link_target), sort_no = VALUES(sort_no), status = VALUES(status);

INSERT INTO posts (id, topic_id, user_id, title, cover_url, content, images, like_count, favorite_count, comment_count, status)
VALUES
(1, 2, 1, '反手高远球总是不到位，怎么练更有效？', '', '最近反手区被压得比较被动，想听听大家的训练方法。', '', 2, 0, 0, 'PUBLISHED'),
(2, 1, 2, '周末奥体双打缺两位球友', '', '周六下午三点，水平中等偏上，欢迎一起打分级对抗。', '', 5, 1, 0, 'PUBLISHED')
ON DUPLICATE KEY UPDATE
topic_id = VALUES(topic_id), user_id = VALUES(user_id), title = VALUES(title), cover_url = VALUES(cover_url), content = VALUES(content),
images = VALUES(images), like_count = VALUES(like_count), favorite_count = VALUES(favorite_count),
comment_count = VALUES(comment_count), status = VALUES(status);
