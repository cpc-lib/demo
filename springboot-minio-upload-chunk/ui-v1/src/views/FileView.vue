<template>
    <div class="container">
        <div style="display:none;">
            <video width="500" height="240" controls id="upvideo">
            </video>
        </div>
        <h2>上传示例</h2>
        <el-upload class="upload-demo" ref="upload" action="https://jsonplaceholder.typicode.com/posts/"
                   :on-remove="handleRemove" :on-change="handleFileChange" :file-list="uploadFileList"
                   :show-file-list="false"
                   :auto-upload="false" multiple>
            <el-button slot="trigger" type="primary" plain>选择文件</el-button>
            <el-button style="margin-left: 5px;" type="success" @click="handler" plain>上传</el-button>
            <el-button type="danger" @click="clearFileHandler" plain>清空</el-button>
        </el-upload>
        <table style="margin-top: 20px">
            <th style="display:inline-block;font-size: 10px;color: #909399;margin-left: 100px">
                文件名
            </th>
            <th style="display:inline-block;;font-size: 12px;color: #909399;margin-left: 191px;">
                文件大小
            </th>
            <th style="display:inline-block;;font-size: 12px;color: #909399;margin-left: 100px">
                上传进度
            </th>
            <th style="display:inline-block;;font-size: 12px;color: #909399;margin-left: 155px">
                状态
            </th>
        </table>
        <!-- 文件列表 -->
        <div class="file-list-wrapper">
            <el-collapse>
                <el-collapse-item v-for="(item, index) in uploadFileList" :key="index">
                    <template slot="title">
                        <div class="upload-file-item">
                            <div class="file-info-item file-name" :title="item.name">{{ item.name }}</div>
                            <div class="file-info-item file-size">{{ item.size | transformByte }}</div>
                            <div class="file-info-item file-progress">
                                <span class="file-progress-label"></span>
                                <el-progress :percentage="item.uploadProgress" class="file-progress-value"/>
                            </div>
                            <div class="file-info-item file-size"><span></span>
                                <el-tag v-if="item.status === '等待上传'" size="medium" type="info">等待上传</el-tag>
                                <el-tag v-else-if="item.status === '校验MD5'" size="medium" type="warning">校验MD5</el-tag>
                                <el-tag v-else-if="item.status === '正在上传'" size="medium">正在上传</el-tag>
                                <el-tag v-else-if="item.status === '上传成功'" size="medium" type="success">上传完成</el-tag>
                                <el-tag v-else size="medium">正在上传</el-tag>
<!--                                <el-tag v-else size="medium" type="danger">上传错误</el-tag>-->
                            </div>
                        </div>
                    </template>
                    <div class="file-chunk-list-wrapper">
                        <!-- 分片列表 -->
                        <el-table :data="item.chunkList" max-height="400" style="width: 100%">
                            <el-table-column prop="chunkNumber" label="分片序号" width="180">
                            </el-table-column>
                            <el-table-column prop="progress" label="上传进度">
                                <template v-slot="{ row }">
                                    <el-progress v-if="!row.status || row.progressStatus === 'normal'"
                                                 :percentage="row.progress"/>
                                    <el-progress v-else :percentage="row.progress" :status="row.progressStatus"
                                                 :text-inside="true" :stroke-width="16"/>
                                </template>
                            </el-table-column>
                            <el-table-column prop="status" label="状态" width="180">
                            </el-table-column>
                        </el-table>
                    </div>
                </el-collapse-item>
            </el-collapse>
        </div>
    </div>
</template>
<script>

    import {checkUpload, initUpload, mergeUpload, uploadFileInfo} from "@/api/upload";

    import {fileSuffixTypeUtil} from "@/utils/FileUtil"

    import axios from 'axios'

    import SparkMD5 from 'spark-md5'

    const FILE_UPLOAD_ID_KEY = 'file_upload_id'

    const chunkSize = 10 * 1024 * 1024 //10mb

    let currentFileIndex = 0


    const FileStatus = {
        wait: '等待上传',
        getMd5: '校验MD5',
        chip: '正在创建序列',
        uploading: '正在上传',
        success: '上传成功',
        error: '上传错误'
    }

    export default {
        data() {
            return {
                // 上传并发数
                simultaneousUploads: 3,
                uploadIdInfo: null,
                uploadFileList: [],
            }
        },


        methods: {
            /**
             * 开始上传文件
             */
            handler() {
                const self = this
                //判断文件列表是否为空
                if (this.uploadFileList.length === 0) {
                    this.$message.error('请先选择文件')
                    return
                }
                if (currentFileIndex >= this.uploadFileList.length) {
                    this.$message.success("文件上传成功")
                    return;
                }
                //当前操作文件
                const currentFile = this.uploadFileList[currentFileIndex]
                console.log("当前操作文件：", currentFile)
                //debugger
                //更新上传标签
                currentFile.status = FileStatus.getMd5
                currentFile.chunkUploadedList = []

                //截取封面图片
                //this.ScreenshotVideo(currentFile.raw);

                // 1. 计算文件MD5
                this.getFileMd5(currentFile.raw, async (md5, totalChunks) => {
                    console.log("md5值", md5)
                    // 2. 检查是否已上传
                    const checkResult = await self.checkFileUploadedByMd5(md5)
                    console.log("检查是否已上传-->",checkResult)
                    // debugger
                    if (checkResult.code === 1) {
                        //self.$message.success(`上传成功，文件地址：${checkResult.data.url}`)
                        console.log('上传成功文件访问地址：' + checkResult.data.url)
                        currentFile.status = FileStatus.success
                        currentFile.uploadProgress = 100
                        //如果此文件上传过，就跳到下一个文件
                        currentFileIndex++;
                        this.handler()
                        return
                    } else if (checkResult.code === 2) {  // "上传中" 状态
                        // 获取已上传分片列表
                        console.log("上传中：", checkResult)
                        currentFile.status = FileStatus.uploading
                        let chunkUploadedList = checkResult.data.chunkUploadedList
                        console.log("chunkUploadedList",chunkUploadedList)
                        currentFile.chunkUploadedList = chunkUploadedList
                        console.log("成功上传的分片信息", chunkUploadedList)
                    } else {   // 未上传
                        console.log('未上传')
                    }
                    // 3. 正在创建分片
                    currentFile.status = FileStatus.chip;

                    //创建分片
                    let fileChunks = self.createFileChunk(currentFile.raw, chunkSize);

                    //重命名文件
                    //let fileName = this.getNewFileName(currentFile)

                    // 获取文件类型
                    //let type = currentFile.name.substring(currentFile.name.lastIndexOf(".") + 1)
                    let type = fileSuffixTypeUtil(currentFile.name)

                    let param = {
                        fileName: currentFile.name,
                        fileSize: currentFile.size,
                        chunkSize: chunkSize,
                        chunkNum: totalChunks,
                        fileMd5: md5,
                        contentType: 'application/octet-stream',
                        fileType: type,
                        //uploadId:localStorage.getItem("file_upload_id"),
                        chunkUploadedList: currentFile.chunkUploadedList//已上传的分片索引+1
                    }
                   // debugger
                    // 4. 获取上传url
                    let uploadIdInfoResult = await self.getFileUploadUrls(param)
                    //debugger

                    let uploadIdInfo = uploadIdInfoResult.data
                    console.log("获取上传url-->", uploadIdInfo)

                    let uploadUrls = uploadIdInfo.urlList

                    self.$set(currentFile, 'chunkList', [])

                    if (uploadUrls !== undefined) {
                        if (fileChunks.length !== uploadUrls.length) {
                            self.$message.error('文件分片上传地址获取错误')
                            return
                        }
                    }
                    fileChunks.map((chunkItem, index) => {
                        if(currentFile.chunkUploadedList.indexOf(index+1)!==-1){
                            currentFile.chunkList.push({
                                chunkNumber: index + 1,
                                chunk: chunkItem,
                                uploadUrl: uploadUrls[index],
                                progress: 100,
                                progressStatus:'success',
                                status: '上传成功'
                            })
                        }else{
                            currentFile.chunkList.push({
                                chunkNumber: index + 1,
                                chunk: chunkItem,
                                uploadUrl: uploadUrls[index],
                                progress: 0,
                                status: '—'
                            })
                        }
                    })
                    console.log("所有分片信息：",currentFile.chunkList)
                    let tempFileChunks = []

                    currentFile.chunkList.forEach((item) => {
                        tempFileChunks.push(item)
                    })

                    //更新状态
                    currentFile.status = FileStatus.uploading

                    // 处理分片列表，删除已上传的分片
                    tempFileChunks = self.processUploadChunkList(tempFileChunks)
                    console.log("删除已上传的分片-->", tempFileChunks)
                    // 5. 上传
                    await self.uploadChunkBase(tempFileChunks)


                    console.log('---上传完成---')

                    //判断是否单文件上传或者分片上传
                    if (uploadIdInfo.uploadId === "SingleFileUpload") {
                        console.log("单文件上传");
                        //更新状态
                        currentFile.status = FileStatus.success
                        //文件下标偏移
                        currentFileIndex++;
                        //递归上传下一个文件
                        this.handler()
                        return
                    } else {
                        // 6. 合并文件
                        console.log("合并文件-->",currentFile)
                        const mergeResult =await self.mergeFile({
                            uploadId: uploadIdInfo.uploadId,
                            fileName: currentFile.name,
                            fileMd5: md5,
                            fileType: type,
                            chunkNum:uploadIdInfo.urlList.length,
                            chunkSize:chunkSize,
                            fileSize:currentFile.size
                        })

                        //合并文件状态
                        if (!mergeResult.data) {
                            currentFile.status = FileStatus.error
                            self.$message.error(mergeResult.error)
                        } else {
                            localStorage.removeItem(FILE_UPLOAD_ID_KEY)
                            currentFile.status = FileStatus.success
                            console.log('文件访问地址：' + mergeResult.data)
                            //文件下标偏移
                            currentFileIndex++;
                            //递归上传下一个文件
                            this.handler()
                        }
                    }
                })
            },

            /**
             * 保存文件信息到数据库
             * @param {*} imgInfoUrl  上传图片封面
             * @param {*} currentFile 上传文件
             * @param {*} fileName 文件名
             * @param {*} url 文件url地址
             * @param {*} md5 md5校验
             */
            saveFileInfoToDB(currentFile, fileName, url, md5) {

                let userInfoCache = JSON.parse(localStorage.getItem('userInfo'))

                let VideoFileInfo = {
                    userId: userInfoCache.id,
                    fileRealName: currentFile.name,
                    fileName: fileName,
                    fileSize: currentFile.size,
                    fileMd5: md5,
                    fileAddress: url,
                    //   imgAddress: imgInfoUrl,
                    bucketName: 'video',
                    fileType: 'video',
                }

                console.log(VideoFileInfo);
                uploadFileInfo(VideoFileInfo).then(res => {
                    console.log(res.data);
                    if (res.status == 200) {
                        this.$message.success("文件信息存储成功");

                        //递归上传文件
                        if (this.uploadFileList.length > currentFileIndex) {
                            this.handleUpload()
                        }

                    } else {
                        this.$message.error("文件信息存储失败");
                    }
                })
            },


            /**
             * 清空列表
             */
            clearFileHandler() {
                this.uploadFileList = []
                this.uploadIdInfo = null
                this.fileIndex = 0
                currentFileIndex = 0
            },

            /**
             * 上传文件列表
             * @param {*} file
             * @param {*} fileList
             */
            handleFileChange(file, fileList) {
                this.initFileProperties(file)
                this.uploadFileList = fileList

            },

            //初始化文件属性
            initFileProperties(file) {
                file.chunkList = []
                file.status = FileStatus.wait
                file.progressStatus = 'warning'
                file.uploadProgress = 0
            },
            /**
             * 移除文件列表
             * @param {*} file
             * @param {*} fileList
             */
            handleRemove(file, fileList) {
                this.uploadFileList = fileList
            },
            /**
             * 检查上传文件格式
             * @param {*} file
             */
            beforeUploadVideo(file) {
                let type = file.name.substring(file.name.lastIndexOf(".") + 1);
                if (
                    [
                        "mp4",
                        "ogg",
                        "flv",
                        "avi",
                        "wmv",
                        "rmvb"
                    ].indexOf(type) == -1
                ) {
                    this.$message.error("请上传正确的视频格式");
                    return false;
                }
            },

            getNewFileName(file, md5) {
                return new Date().getTime() + file.name
                //return md5+"-"+ file.name
            },


            /**
             * 分片读取文件 获取文件的MD5
             * @param file
             * @param callback
             */
            getFileMd5(file, callback) {
                const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice
                const fileReader = new FileReader()
                // 计算分片数
                const totalChunks = Math.ceil(file.size / chunkSize)
                console.log('总分片数：' + totalChunks)
                let currentChunk = 0
                const spark = new SparkMD5.ArrayBuffer()
                loadNext()
                fileReader.onload = function (e) {
                    try {
                        spark.append(e.target.result)
                    } catch (error) {
                        console.log('获取Md5错误：' + currentChunk)
                    }
                    if (currentChunk < totalChunks) {
                        currentChunk++
                        loadNext()
                    } else {
                        callback(spark.end(), totalChunks)
                    }
                }
                fileReader.onerror = function () {
                    console.warn('读取Md5失败，文件读取错误')
                }

                function loadNext() {
                    const start = currentChunk * chunkSize
                    const end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize
                    // 注意这里的 fileRaw
                    fileReader.readAsArrayBuffer(blobSlice.call(file, start, end))
                }
            },
            /**
             * 文件分片
             */
            createFileChunk(file, size = chunkSize) {
                const fileChunkList = []
                let count = 0
                while (count < file.size) {
                    fileChunkList.push({
                        file: file.slice(count, count + size),
                    })
                    count += size
                }
                return fileChunkList
            },
            /**
             * 处理即将上传的分片列表，判断是否有已上传的分片，有则从列表中删除
             */
            processUploadChunkList(chunkList) {
                const currentFile = this.uploadFileList[currentFileIndex]
                let chunkUploadedList = currentFile.chunkUploadedList
                if (chunkUploadedList === undefined || chunkUploadedList === null || chunkUploadedList.length === 0) {
                    return chunkList
                }
                //
                for (let i = chunkList.length - 1; i >= 0; i--) {
                    const chunkItem = chunkList[i]
                    for (let j = 0; j < chunkUploadedList.length; j++) {
                        if (chunkItem.chunkNumber === chunkUploadedList[j]) {
                            chunkList.splice(i, 1)
                            break
                        }
                    }
                }
                return chunkList
            },

            /**
             * 上传分片
             * @param chunkList
             * @returns {Promise<unknown>}
             */
            uploadChunkBase(chunkList) {
                const self = this
                let successCount = 0
                let totalChunks = chunkList.length
                return new Promise((resolve, reject) => {
                    const handler = () => {
                        if (chunkList.length) {

                            const chunkItem = chunkList.shift()
                            // 直接上传二进制，不需要构造 FormData，否则上传后文件损坏
                            axios.put(chunkItem.uploadUrl, chunkItem.chunk.file, {
                                // 上传进度处理
                                onUploadProgress: self.checkChunkUploadProgress(chunkItem),
                                headers: {
                                    'Content-Type': 'application/octet-stream'
                                }
                            }).then(response => {
                                if (response.status === 200) {
                                    console.log('分片：' + chunkItem.chunkNumber + ' 上传成功')
                                    successCount++
                                    // 继续上传下一个分片
                                  //  debugger
                                    handler()
                                } else {
                                    console.log('上传失败：' + response.status + '，' + response.statusText)
                                }
                            })
                                .catch(error => {
                                    // 更新状态
                                    console.log('分片：' + chunkItem.chunkNumber + ' 上传失败，' + error)
                                    // 重新添加到队列中
                                    chunkList.push(chunkItem)
                                    handler()
                                })
                        }
                        if (successCount >= totalChunks) {
                            resolve()
                        }
                    }
                    // 并发
                    for (let i = 0; i < this.simultaneousUploads; i++) {
                        handler()
                    }
                })
            },

            /**
             * 获取直接上传的uri链接
             * @param fileParam
             * @returns {Promise<AxiosResponse<any>>}
             */
            getFileUploadUrls(fileParam) {
                return initUpload(fileParam)
            },

            /**
             * 根据MD5查看文件是否上传过
             * @param md5
             * @returns {Promise<unknown>}
             */
            checkFileUploadedByMd5(md5) {
                return new Promise((resolve, reject) => {
                    checkUpload(md5).then(response => {
                        console.log("md5-->:", response);
                        resolve(response)
                    }).catch(error => {
                        reject(error)
                    })
                })
            },
            /**
             * 合并文件
             *   uploadId: self.uploadIdInfo.uploadId,
             fileName: currentFile.name,
             //fileMd5: fileMd5,
             bucketName: 'bucket'
             */
            mergeFile(fileParam) {
                const self = this;
                return new Promise((resolve, reject) => {
                    mergeUpload(fileParam).then(response => {
                        console.log(response);
                        let data = response
                        console.log("@@@", data)
                        if (!data) {
                            data.msg = FileStatus.error
                            resolve(data)
                        } else {
                            data.msg = FileStatus.success
                            resolve(data)
                        }
                    })
                    // .catch(error => {
                    //     self.$message.error('合并文件失败：' + error)
                    //     file.status = FileStatus.error
                    //     reject()
                    // })
                })
            },
            /**
             * 检查分片上传进度
             */
            checkChunkUploadProgress(item) {
                return p => {
                    item.progress = parseInt(String((p.loaded / p.total) * 100))
                    console.log("进度：", this.uploadFileList[currentFileIndex].uploadProgress)
                    this.updateChunkUploadStatus(item)
                }
            },
            updateChunkUploadStatus(item) {
                let status = FileStatus.uploading
                let progressStatus = 'normal'
                if (item.progress >= 100) {
                    status = FileStatus.success
                    progressStatus = 'success'
                }
                let chunkIndex = item.chunkNumber - 1
                let currentChunk = this.uploadFileList[currentFileIndex].chunkList[chunkIndex]
                // 修改状态
                currentChunk.status = status
                currentChunk.progressStatus = progressStatus
                // 更新状态
                this.$set(this.uploadFileList[currentFileIndex].chunkList, chunkIndex, currentChunk)
                // 获取文件上传进度
                this.getCurrentFileProgress()
            },


            getCurrentFileProgress() {
                const currentFile = this.uploadFileList[currentFileIndex]
                if (!currentFile || !currentFile.chunkList) {
                    return
                }
                const chunkList = currentFile.chunkList
                const uploadedSize = chunkList.map((item) => item.chunk.file.size * item.progress).reduce((acc, cur) => acc + cur)
                // 计算方式：已上传大小 / 文件总大小
                let progress = parseInt((uploadedSize / currentFile.size).toFixed(2))
               // debugger
                currentFile.uploadProgress = progress
                this.$set(this.uploadFileList,currentFileIndex, currentFile)
            },
        },
        filters: {
            transformByte(size) {
                if (!size) {
                    return '0B'
                }
                const unitSize = 1024
                if (size < unitSize) {
                    return size + ' B'
                }
                // KB
                if (size < Math.pow(unitSize, 2)) {
                    return (size / unitSize).toFixed(2) + ' K';
                }
                // MB
                if (size < Math.pow(unitSize, 3)) {
                    return (size / Math.pow(unitSize, 2)).toFixed(2) + ' MB'
                }
                // GB
                if (size < Math.pow(unitSize, 4)) {
                    return (size / Math.pow(unitSize, 3)).toFixed(2) + ' GB';
                }
                // TB
                return (size / Math.pow(unitSize, 4)).toFixed(2) + ' TB';
            }
        }
    }
</script>
<style scoped>
    .container {
        width: 750px;
        margin: 0 auto;
    }

    .file-list-wrapper {
        margin-top: 20px;
    }

    h2 {
        text-align: center;
    }

    .file-info-item {

        margin: 0 10px;
    }

    .upload-file-item {
        display: flex;
    }

    .file-progress {
        display: flex;
        align-items: center;
    }

    .file-progress-value {
        width: 150px;
    }

    .file-name {
        width: 250px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .file-size {
        width: 100px;
        margin-left: 60px;
    }

    .uploader-example {
        width: 880px;
        padding: 15px;
        margin: 40px auto 0;
        font-size: 12px;
        box-shadow: 0 0 10px rgba(0, 0, 0, .4);
    }

    .uploader-example .uploader-btn {
        margin-right: 4px;
    }

    .uploader-example .uploader-list {
        max-height: 440px;
        overflow: auto;
        overflow-x: hidden;
        overflow-y: auto;
    }
</style>
