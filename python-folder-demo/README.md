# Python Folder Tool

使用 Python 线程池对目录执行并发复制或移动，保留原目录结构。

## 功能
- 支持 `copy` 和 `move`
- 使用 `ThreadPoolExecutor` 自定义线程数
- 保留源目录层级结构
- `move` 完成后自动尝试删除空目录

## 运行环境
- Python 3.8+

## 用法
```bash
复制
python main.py copy D:\Telegram\1 D:\Telegram\2 --core-workers 32 --max-workers 64 --queue-size 1000
移动
python main.py move D:\Telegram\1 D:\Telegram\2 --core-workers 32 --max-workers 64 --queue-size 1000
```

## 参数说明
- `operation`：`copy` 或 `move`
- `source_dir`：源目录
- `target_dir`：目标目录
- `--workers`：线程数，默认 `max(4, CPU*2)`
