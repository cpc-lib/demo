$server = "192.168.220.200"
7700, 10010, 10086 | ForEach-Object {
    Test-NetConnection $server -Port $_
}
