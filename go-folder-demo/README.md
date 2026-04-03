使用方式
1）直接运行

复制：

go run main.go -op copy -src D:/Telegram/1 -dst D:/Telegram/2 -core-workers 32 -max-workers 64 -queue-size 1000

移动：

go run main.go -op move -src D:/Telegram/1 -dst D:/Telegram/2 -core-workers 32 -max-workers 64 -queue-size 1000

2）编译后运行

linux
go build -o go-folder-demo main.go

windows
go build -o go-folder-demo.exe main.go

然后：

./go-folder-demo -op copy -src /data/a -dst /data/b -core-workers 32 -max-workers 64 -queue-size 1000

Windows 下：

go-folder-demo.exe -op move -src D:\Telegram\1 -dst D:\Telegram\2 -core-workers 32 -max-workers 64 -queue-size 1000