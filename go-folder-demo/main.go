package main

import (
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

type Task func()

type WorkerPool struct {
	coreWorkers int
	maxWorkers  int
	queueSize   int

	taskQueue chan Task
	wg        sync.WaitGroup

	mu          sync.Mutex
	workerCount int
	closed      bool
}

func NewWorkerPool(coreWorkers, maxWorkers, queueSize int) (*WorkerPool, error) {
	if coreWorkers <= 0 {
		return nil, errors.New("coreWorkers 必须大于 0")
	}
	if maxWorkers <= 0 {
		return nil, errors.New("maxWorkers 必须大于 0")
	}
	if queueSize <= 0 {
		return nil, errors.New("queueSize 必须大于 0")
	}
	if coreWorkers > maxWorkers {
		return nil, errors.New("coreWorkers 不能大于 maxWorkers")
	}

	pool := &WorkerPool{
		coreWorkers: coreWorkers,
		maxWorkers:  maxWorkers,
		queueSize:   queueSize,
		taskQueue:   make(chan Task, queueSize),
	}

	for i := 0; i < coreWorkers; i++ {
		pool.startWorker()
	}

	return pool, nil
}

func (p *WorkerPool) startWorker() {
	p.mu.Lock()
	defer p.mu.Unlock()

	if p.workerCount >= p.maxWorkers {
		return
	}

	p.workerCount++
	workerID := p.workerCount

	go func() {
		for task := range p.taskQueue {
			if task != nil {
				task()
			}
		}
		fmt.Printf("[线程池] worker-%d 已退出\n", workerID)
	}()
}

func (p *WorkerPool) Submit(task Task) error {
	p.mu.Lock()
	if p.closed {
		p.mu.Unlock()
		return errors.New("线程池已关闭")
	}
	p.mu.Unlock()

	p.wg.Add(1)

	// 先尝试直接放入队列
	select {
	case p.taskQueue <- func() {
		defer p.wg.Done()
		task()
	}:
		return nil
	default:
		// 队列满了，尝试扩容 worker
		p.mu.Lock()
		if p.workerCount < p.maxWorkers {
			p.mu.Unlock()
			p.startWorker()
		} else {
			p.mu.Unlock()
		}

		// 阻塞提交，模拟有界队列 + 回压
		p.taskQueue <- func() {
			defer p.wg.Done()
			task()
		}
		return nil
	}
}

func (p *WorkerPool) Wait() {
	p.wg.Wait()
}

func (p *WorkerPool) Shutdown() {
	p.mu.Lock()
	if p.closed {
		p.mu.Unlock()
		return
	}
	p.closed = true
	p.mu.Unlock()

	p.Wait()
	close(p.taskQueue)
}

type FolderOperationService struct {
	pool         *WorkerPool
	successCount int64
	failCount    int64
}

func NewFolderOperationService(coreWorkers, maxWorkers, queueSize int) (*FolderOperationService, error) {
	pool, err := NewWorkerPool(coreWorkers, maxWorkers, queueSize)
	if err != nil {
		return nil, err
	}
	return &FolderOperationService{pool: pool}, nil
}

func (s *FolderOperationService) CopyDirectory(sourceDir, targetDir string) error {
	return s.processDirectory(sourceDir, targetDir, "copy")
}

func (s *FolderOperationService) MoveDirectory(sourceDir, targetDir string) error {
	return s.processDirectory(sourceDir, targetDir, "move")
}

func (s *FolderOperationService) processDirectory(sourceDir, targetDir, operation string) error {
	if err := validatePath(sourceDir, targetDir); err != nil {
		return err
	}

	atomic.StoreInt64(&s.successCount, 0)
	atomic.StoreInt64(&s.failCount, 0)

	if err := os.MkdirAll(targetDir, os.ModePerm); err != nil {
		return fmt.Errorf("创建目标目录失败: %w", err)
	}

	var directories []string
	var files []string

	err := filepath.Walk(sourceDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		rel, err := filepath.Rel(sourceDir, path)
		if err != nil {
			return err
		}

		if info.IsDir() {
			directories = append(directories, path)
			targetSubDir := filepath.Join(targetDir, rel)
			return os.MkdirAll(targetSubDir, os.ModePerm)
		}

		files = append(files, path)
		return nil
	})
	if err != nil {
		return fmt.Errorf("遍历目录失败: %w", err)
	}

	start := time.Now()

	for _, filePath := range files {
		src := filePath
		err := s.pool.Submit(func() {
			s.processSingleFile(sourceDir, targetDir, src, operation)
		})
		if err != nil {
			return fmt.Errorf("提交任务失败: %w", err)
		}
	}

	s.pool.Wait()

	if operation == "move" {
		if err := deleteEmptyDirectories(sourceDir, directories); err != nil {
			fmt.Printf("删除空目录时出现部分异常: %v\n", err)
		}
	}

	cost := time.Since(start).Milliseconds()
	fmt.Println("\n========== 执行结果 ==========")
	fmt.Printf("操作类型: %s\n", operation)
	fmt.Printf("成功处理文件数: %d\n", atomic.LoadInt64(&s.successCount))
	fmt.Printf("失败文件数: %d\n", atomic.LoadInt64(&s.failCount))
	fmt.Printf("总耗时: %d ms\n", cost)

	return nil
}

func (s *FolderOperationService) processSingleFile(sourceDir, targetDir, sourceFile, operation string) {
	rel, err := filepath.Rel(sourceDir, sourceFile)
	if err != nil {
		atomic.AddInt64(&s.failCount, 1)
		fmt.Printf("[失败][%s] 计算相对路径失败 %s，原因: %v\n", operation, sourceFile, err)
		return
	}

	targetFile := filepath.Join(targetDir, rel)

	if err := os.MkdirAll(filepath.Dir(targetFile), os.ModePerm); err != nil {
		atomic.AddInt64(&s.failCount, 1)
		fmt.Printf("[失败][%s] 创建目标目录失败 %s -> %s，原因: %v\n", operation, sourceFile, targetFile, err)
		return
	}

	switch operation {
	case "copy":
		err = copyFile(sourceFile, targetFile)
	case "move":
		err = moveFile(sourceFile, targetFile)
	default:
		err = fmt.Errorf("不支持的操作类型: %s", operation)
	}

	if err != nil {
		atomic.AddInt64(&s.failCount, 1)
		fmt.Printf("[失败][%s] %s -> %s，原因: %v\n", operation, sourceFile, targetFile, err)
		return
	}

	current := atomic.AddInt64(&s.successCount, 1)
	fmt.Printf("[成功][%s] %s -> %s，累计成功: %d\n", operation, sourceFile, targetFile, current)
}

func (s *FolderOperationService) Shutdown() {
	s.pool.Shutdown()
}

func validatePath(sourceDir, targetDir string) error {
	sourceAbs, err := filepath.Abs(sourceDir)
	if err != nil {
		return err
	}
	targetAbs, err := filepath.Abs(targetDir)
	if err != nil {
		return err
	}

	sourceAbs = filepath.Clean(sourceAbs)
	targetAbs = filepath.Clean(targetAbs)

	info, err := os.Stat(sourceAbs)
	if err != nil {
		if os.IsNotExist(err) {
			return fmt.Errorf("源目录不存在: %s", sourceAbs)
		}
		return err
	}
	if !info.IsDir() {
		return fmt.Errorf("源路径不是目录: %s", sourceAbs)
	}
	if sourceAbs == targetAbs {
		return errors.New("源目录和目标目录不能相同")
	}

	// 目标目录不能是源目录子目录
	sourceWithSep := sourceAbs + string(os.PathSeparator)
	targetWithSep := targetAbs + string(os.PathSeparator)
	if strings.HasPrefix(targetWithSep, sourceWithSep) {
		return errors.New("目标目录不能是源目录的子目录，否则会导致递归处理问题")
	}

	return nil
}

func copyFile(src, dst string) error {
	srcFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer srcFile.Close()

	srcInfo, err := srcFile.Stat()
	if err != nil {
		return err
	}

	dstFile, err := os.Create(dst)
	if err != nil {
		return err
	}

	_, err = io.Copy(dstFile, srcFile)
	closeErr := dstFile.Close()
	if err != nil {
		return err
	}
	if closeErr != nil {
		return closeErr
	}

	// 保留权限
	if err := os.Chmod(dst, srcInfo.Mode()); err != nil {
		return err
	}

	return nil
}

func moveFile(src, dst string) error {
	// 先尝试直接 rename
	err := os.Rename(src, dst)
	if err == nil {
		return nil
	}

	// 跨磁盘时 fallback: copy + delete
	if err := copyFile(src, dst); err != nil {
		return err
	}
	return os.Remove(src)
}

func deleteEmptyDirectories(sourceDir string, directories []string) error {
	// 逆序删除，先删深层子目录
	for i := len(directories) - 1; i >= 0; i-- {
		dir := directories[i]
		if dir == sourceDir {
			continue
		}
		_ = os.Remove(dir)
	}

	err := os.Remove(sourceDir)
	if err == nil {
		fmt.Printf("已删除空源目录: %s\n", sourceDir)
		return nil
	}

	if os.IsExist(err) || strings.Contains(err.Error(), "directory not empty") {
		fmt.Printf("源目录仍有未处理内容，保留目录: %s\n", sourceDir)
		return nil
	}

	return err
}

func main() {
	operation := flag.String("op", "", "操作类型：copy 或 move")
	sourceDir := flag.String("src", "", "源目录")
	targetDir := flag.String("dst", "", "目标目录")
	coreWorkers := flag.Int("core-workers", 4, "核心 worker 数")
	maxWorkers := flag.Int("max-workers", 8, "最大 worker 数")
	queueSize := flag.Int("queue-size", 1000, "任务队列长度")

	flag.Parse()

	if *operation == "" || *sourceDir == "" || *targetDir == "" {
		fmt.Println("用法:")
		fmt.Println("  go run main.go -op copy -src D:/a -dst D:/b -core-workers 4 -max-workers 8 -queue-size 1000")
		fmt.Println("  go run main.go -op move -src D:/a -dst D:/b -core-workers 4 -max-workers 8 -queue-size 1000")
		os.Exit(1)
	}

	service, err := NewFolderOperationService(*coreWorkers, *maxWorkers, *queueSize)
	if err != nil {
		fmt.Printf("初始化失败: %v\n", err)
		os.Exit(1)
	}
	defer service.Shutdown()

	switch *operation {
	case "copy":
		err = service.CopyDirectory(*sourceDir, *targetDir)
	case "move":
		err = service.MoveDirectory(*sourceDir, *targetDir)
	default:
		fmt.Printf("不支持的操作类型: %s，仅支持 copy 或 move\n", *operation)
		os.Exit(1)
	}

	if err != nil {
		fmt.Printf("执行失败: %v\n", err)
		os.Exit(1)
	}
}
