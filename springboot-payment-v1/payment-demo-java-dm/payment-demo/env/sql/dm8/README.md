# DM8 SQL 初始化脚本

- `001_payment_demo_dm8.sql`：达梦 DM8 完整初始化脚本，替代原 MySQL 8 初始化脚本。
- 默认执行用户：`SYSDBA`
- 默认应用 schema：`SYSDBA`
- 执行方式：

```bash
bash env/scripts/dm8/init-dm8-sql.sh
```

脚本会通过容器内 `/opt/dmdbms/bin/disql` 按文件名顺序执行 `env/sql/dm8/*.sql`。

说明：

- `001_payment_demo_dm8.sql` 是完整初始化脚本，会重建运行时表并写入初始数据。
- 后端启动不会自动创建运行时表。若启动或查询时报 `无效的表或视图名[t_product]` 或其他运行时表缺失，请先确认应用连接的 `DM_HOST`、`DM_PORT`、`DM_SCHEMA`、`DM_USERNAME` 与执行初始化的库一致，并重新执行完整初始化 SQL。
