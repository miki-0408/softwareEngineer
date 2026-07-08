UPDATE user SET avatar = REPLACE(avatar, CHAR(92), '/');
SELECT userId, name, avatar FROM user LIMIT 5;
