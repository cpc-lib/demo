const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const fileInfo = document.getElementById('fileInfo');
const fileName = document.getElementById('fileName');
const fileSize = document.getElementById('fileSize');
const startBtn = document.getElementById('startBtn');
const taskPanel = document.getElementById('taskPanel');
const statusText = document.getElementById('statusText');
const progressText = document.getElementById('progressText');
const progressBar = document.getElementById('progressBar');
const detailText = document.getElementById('detailText');
const resultPanel = document.getElementById('resultPanel');
const hashValue = document.getElementById('hashValue');
const copyBtn = document.getElementById('copyBtn');
const errorBox = document.getElementById('errorBox');

let selectedFile = null;
let pollTimer = null;

dropZone.addEventListener('click', () => fileInput.click());
dropZone.addEventListener('keydown', e => {
    if (e.key === 'Enter' || e.key === ' ') fileInput.click();
});
fileInput.addEventListener('change', () => selectFile(fileInput.files[0]));

['dragenter', 'dragover'].forEach(name => dropZone.addEventListener(name, e => {
    e.preventDefault();
    dropZone.classList.add('dragging');
}));
['dragleave', 'drop'].forEach(name => dropZone.addEventListener(name, e => {
    e.preventDefault();
    dropZone.classList.remove('dragging');
}));
dropZone.addEventListener('drop', e => selectFile(e.dataTransfer.files[0]));

startBtn.addEventListener('click', startUpload);
copyBtn.addEventListener('click', async () => {
    await navigator.clipboard.writeText(hashValue.textContent);
    copyBtn.textContent = '已复制';
    setTimeout(() => copyBtn.textContent = '复制', 1200);
});

function selectFile(file) {
    if (!file) return;
    selectedFile = file;
    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    fileInfo.classList.remove('hidden');
    taskPanel.classList.add('hidden');
    resultPanel.classList.add('hidden');
    errorBox.classList.add('hidden');
}

function startUpload() {
    if (!selectedFile) return;
    clearInterval(pollTimer);
    resultPanel.classList.add('hidden');
    errorBox.classList.add('hidden');
    taskPanel.classList.remove('hidden');
    startBtn.disabled = true;
    updateProgress(0, '正在上传', `正在上传 ${selectedFile.name}`);

    const formData = new FormData();
    formData.append('file', selectedFile);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/sha256/tasks');
    xhr.responseType = 'json';

    xhr.upload.onprogress = event => {
        if (event.lengthComputable) {
            const percent = Math.round(event.loaded * 100 / event.total);
            updateProgress(percent, '正在上传', `上传进度 ${percent}%`);
        }
    };

    xhr.onload = () => {
        if (xhr.status !== 202) {
            showError('创建计算任务失败，HTTP ' + xhr.status);
            startBtn.disabled = false;
            return;
        }
        const task = xhr.response;
        updateProgress(0, '已进入计算队列', '任务 ID：' + task.taskId);
        pollTask(task.taskId);
    };

    xhr.onerror = () => {
        showError('网络错误，文件上传失败。');
        startBtn.disabled = false;
    };

    xhr.send(formData);
}

function pollTask(taskId) {
    pollTimer = setInterval(async () => {
        try {
            const response = await fetch('/api/sha256/tasks/' + taskId, {cache: 'no-store'});
            if (!response.ok) throw new Error('任务查询失败，HTTP ' + response.status);
            const task = await response.json();

            if (task.status === 'QUEUED') {
                updateProgress(0, '排队中', '等待 SHA-256 工作线程');
            } else if (task.status === 'RUNNING') {
                updateProgress(task.progress, '正在计算', `${formatBytes(task.processedBytes)} / ${formatBytes(task.totalBytes)}`);
            } else if (task.status === 'SUCCESS') {
                clearInterval(pollTimer);
                updateProgress(100, '计算完成', formatBytes(task.totalBytes));
                hashValue.textContent = task.sha256;
                resultPanel.classList.remove('hidden');
                startBtn.disabled = false;
            } else if (task.status === 'FAILED') {
                clearInterval(pollTimer);
                showError(task.error || '计算失败');
                startBtn.disabled = false;
            }
        } catch (error) {
            clearInterval(pollTimer);
            showError(error.message);
            startBtn.disabled = false;
        }
    }, 700);
}

function updateProgress(percent, status, detail) {
    const normalized = Math.max(0, Math.min(100, percent || 0));
    progressBar.style.width = normalized + '%';
    progressText.textContent = normalized + '%';
    statusText.textContent = status;
    detailText.textContent = detail || '';
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.classList.remove('hidden');
}

function formatBytes(bytes) {
    if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let value = bytes;
    let index = 0;
    while (value >= 1024 && index < units.length - 1) {
        value /= 1024;
        index++;
    }
    return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`;
}
