import request from '@/utils/request'

//上传信息
export function uploadScreenshot(data){
    return request({
        url:'upload/multipart/uploadScreenshot',
        method:'post',
        data
    })
}

//上传信息
export function uploadFileInfo(data){
    return request({
        url:'upload/multipart/uploadFileInfo',
        method:'post',
        data
    })
}

// 上传校验
export function checkUpload(MD5) {
    return request({
        url: `upload/multipart/check?md5=${MD5}`,
        method: 'get',
    })
};


// 初始化上传
export function initUpload(data) {
    return request({
        url: `upload/multipart/init`,
        method: 'post',
        data
    })
};


// 初始化上传
export function mergeUpload(data) {
    return request({
        url: `upload/multipart/merge`,
        method: 'post',
        data
    })
};


