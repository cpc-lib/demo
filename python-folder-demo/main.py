#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
import queue
import shutil
import sys
import threading
import time
from pathlib import Path
from typing import Callable, List, Optional, Tuple


class CustomThreadPool:
    """
    一个简化版自定义线程池，模拟 Java ThreadPoolExecutor 的核心能力：
    - core_workers: 核心线程数
    - max_workers: 最大线程数
    - queue_size: 任务队列长度
    """

    def __init__(self, core_workers: int, max_workers: int, queue_size: int, keep_alive: float = 10.0):
        if core_workers <= 0:
            raise ValueError("core_workers 必须大于 0")
        if max_workers <= 0:
            raise ValueError("max_workers 必须大于 0")
        if queue_size <= 0:
            raise ValueError("queue_size 必须大于 0")
        if core_workers > max_workers:
            raise ValueError("core_workers 不能大于 max_workers")

        self.core_workers = core_workers
        self.max_workers = max_workers
        self.keep_alive = keep_alive
        self.task_queue: queue.Queue = queue.Queue(maxsize=queue_size)

        self.workers: List[threading.Thread] = []
        self.worker_count = 0
        self.lock = threading.Lock()
        self.shutdown_flag = False

        self.active_tasks = 0
        self.active_lock = threading.Lock()
        self.all_done = threading.Condition(self.active_lock)

        # 先启动核心线程
        for _ in range(self.core_workers):
            self._start_worker(core=True)

    def _start_worker(self, core: bool) -> None:
        with self.lock:
            worker_name = f"custom-pool-{'core' if core else 'extra'}-{self.worker_count + 1}"
            t = threading.Thread(target=self._worker_run, args=(core,), name=worker_name, daemon=True)
            self.workers.append(t)
            self.worker_count += 1
            t.start()

    def _worker_run(self, core: bool) -> None:
        while True:
            if self.shutdown_flag and self.task_queue.empty():
                return

            try:
                timeout = None if core else self.keep_alive
                task = self.task_queue.get(timeout=timeout)
            except queue.Empty:
                # 非核心线程空闲超时后退出
                if not core:
                    with self.lock:
                        current = threading.current_thread()
                        if current in self.workers:
                            self.workers.remove(current)
                    return
                continue

            if task is None:
                self.task_queue.task_done()
                return

            fn, args, kwargs = task

            with self.active_lock:
                self.active_tasks += 1

            try:
                fn(*args, **kwargs)
            except Exception as exc:
                print(f"[线程池任务异常] {exc}", file=sys.stderr)
            finally:
                with self.active_lock:
                    self.active_tasks -= 1
                    if self.active_tasks == 0 and self.task_queue.empty():
                        self.all_done.notify_all()

                self.task_queue.task_done()

    def submit(self, fn: Callable, *args, **kwargs) -> None:
        if self.shutdown_flag:
            raise RuntimeError("线程池已关闭，不能再提交任务")

        # 先尝试直接放入队列
        try:
            self.task_queue.put((fn, args, kwargs), block=False)
        except queue.Full:
            # 队列满了，如果还能扩线程，就扩到 max_workers
            with self.lock:
                current_workers = len(self.workers)
                if current_workers < self.max_workers:
                    self._start_worker(core=False)

            # 再阻塞放入队列，模拟有界队列回压
            self.task_queue.put((fn, args, kwargs), block=True)

    def wait_completion(self) -> None:
        self.task_queue.join()
        with self.active_lock:
            while self.active_tasks > 0 or not self.task_queue.empty():
                self.all_done.wait(timeout=0.5)

    def shutdown(self, wait: bool = True) -> None:
        self.shutdown_flag = True

        if wait:
            self.wait_completion()

        # 给所有线程发退出信号
        with self.lock:
            workers_snapshot = list(self.workers)

        for _ in workers_snapshot:
            try:
                self.task_queue.put(None, timeout=1)
            except queue.Full:
                pass

        if wait:
            for worker in workers_snapshot:
                worker.join(timeout=2)


class FolderOperationService:
    def __init__(self, core_workers: int, max_workers: int, queue_size: int):
        self.pool = CustomThreadPool(
            core_workers=core_workers,
            max_workers=max_workers,
            queue_size=queue_size
        )
        self.success_count = 0
        self.fail_count = 0
        self.counter_lock = threading.Lock()

    def copy_directory(self, source_dir: Path, target_dir: Path) -> None:
        self._process_directory(source_dir, target_dir, operation="copy")

    def move_directory(self, source_dir: Path, target_dir: Path) -> None:
        self._process_directory(source_dir, target_dir, operation="move")

    def _process_directory(self, source_dir: Path, target_dir: Path, operation: str) -> None:
        self._validate_path(source_dir, target_dir)

        self.success_count = 0
        self.fail_count = 0

        target_dir.mkdir(parents=True, exist_ok=True)

        directories, files = self._scan_directory(source_dir)

        # 预创建目标目录结构
        for directory in directories:
            relative = directory.relative_to(source_dir)
            (target_dir / relative).mkdir(parents=True, exist_ok=True)

        start_time = time.time()

        # 提交文件任务
        for file_path in files:
            self.pool.submit(self._process_single_file, source_dir, target_dir, file_path, operation)

        # 等待全部执行完成
        self.pool.wait_completion()

        # move 后清理空目录
        if operation == "move":
            self._delete_empty_directories(source_dir)

        cost_ms = int((time.time() - start_time) * 1000)
        print("\n========== 执行结果 ==========")
        print(f"操作类型: {operation}")
        print(f"成功处理文件数: {self.success_count}")
        print(f"失败文件数: {self.fail_count}")
        print(f"总耗时: {cost_ms} ms")

    def _scan_directory(self, source_dir: Path) -> Tuple[List[Path], List[Path]]:
        directories: List[Path] = []
        files: List[Path] = []

        for root, dir_names, file_names in os.walk(source_dir):
            root_path = Path(root)
            directories.append(root_path)
            for file_name in file_names:
                files.append(root_path / file_name)

        return directories, files

    def _process_single_file(self, source_dir: Path, target_dir: Path, source_file: Path, operation: str) -> None:
        relative = source_file.relative_to(source_dir)
        target_file = target_dir / relative

        try:
            target_file.parent.mkdir(parents=True, exist_ok=True)

            if operation == "copy":
                shutil.copy2(source_file, target_file)
            elif operation == "move":
                shutil.move(str(source_file), str(target_file))
            else:
                raise ValueError(f"不支持的操作类型: {operation}")

            with self.counter_lock:
                self.success_count += 1
                current = self.success_count

            print(f"[成功][{operation}] {source_file} -> {target_file}，累计成功: {current}")

        except Exception as exc:
            with self.counter_lock:
                self.fail_count += 1
            print(f"[失败][{operation}] {source_file} -> {target_file}，原因: {exc}", file=sys.stderr)

    def _delete_empty_directories(self, source_dir: Path) -> None:
        for root, dir_names, file_names in os.walk(source_dir, topdown=False):
            root_path = Path(root)
            try:
                if not dir_names and not file_names:
                    root_path.rmdir()
            except OSError:
                pass

        if source_dir.exists():
            try:
                source_dir.rmdir()
                print(f"已删除空源目录: {source_dir}")
            except OSError:
                print(f"源目录仍有未处理内容，保留目录: {source_dir}")

    @staticmethod
    def _validate_path(source_dir: Path, target_dir: Path) -> None:
        if not source_dir.exists():
            raise ValueError(f"源目录不存在: {source_dir}")
        if not source_dir.is_dir():
            raise ValueError(f"源路径不是目录: {source_dir}")
        if source_dir.resolve() == target_dir.resolve():
            raise ValueError("源目录和目标目录不能相同")

        try:
            target_dir.resolve().relative_to(source_dir.resolve())
            raise ValueError("目标目录不能是源目录的子目录，否则会导致递归处理问题")
        except ValueError:
            pass

    def shutdown(self) -> None:
        self.pool.shutdown(wait=True)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="使用自定义线程池并发复制或移动目录内容")
    parser.add_argument("operation", choices=["copy", "move"], help="操作类型：copy 或 move")
    parser.add_argument("source_dir", help="源目录")
    parser.add_argument("target_dir", help="目标目录")
    parser.add_argument("--core-workers", type=int, default=4, help="核心线程数，默认 4")
    parser.add_argument("--max-workers", type=int, default=8, help="最大线程数，默认 8")
    parser.add_argument("--queue-size", type=int, default=1000, help="队列长度，默认 1000")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    source_dir = Path(args.source_dir).expanduser().resolve()
    target_dir = Path(args.target_dir).expanduser().resolve()

    service = FolderOperationService(
        core_workers=args.core_workers,
        max_workers=args.max_workers,
        queue_size=args.queue_size
    )

    try:
        if args.operation == "copy":
            service.copy_directory(source_dir, target_dir)
        else:
            service.move_directory(source_dir, target_dir)
    finally:
        service.shutdown()


if __name__ == "__main__":
    main()