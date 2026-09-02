const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const fileInfo = document.getElementById('fileInfo');
const fileName = document.getElementById('fileName');
const fileSize = document.getElementById('fileSize');
const startBtn = document.getElementById('startBtn');
const abortBtn = document.getElementById('abortBtn');
const taskPanel = document.getElementById('taskPanel');
const statusText = document.getElementById('statusText');
const progressText = document.getElementById('progressText');
const progressBar = document.getElementById('progressBar');
const detailText = document.getElementById('detailText');
const taskMeta = document.getElementById('taskMeta');
const resultPanel = document.getElementById('resultPanel');
const hashValue = document.getElementById('hashValue');
const copyBtn = document.getElementById('copyBtn');
const errorBox = document.getElementById('errorBox');
const brokerBadge = document.getElementById('brokerBadge');
const storageBadge = document.getElementById('storageBadge');

const RESUME_PREFIX = 'sha256-resume:';
const PRESIGN_BATCH_SIZE = 40;
const PART_RETRY_COUNT = 3;

let selectedFile = null;
let currentSession = null;
let pollTimer = null;
let activeXhrs = new Set();
let uploadCancelled = false;

loadSystemInfo();
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
abortBtn.addEventListener('click', abortUpload);
copyBtn.addEventListener('click', async () => {
    await navigator.clipboard.writeText(hashValue.textContent);
    copyBtn.textContent = '已复制';
    setTimeout(() => copyBtn.textContent = '复制', 1200);
});

window.addEventListener('offline', () => {
    if (currentSession && activeXhrs.size > 0) {
        showError('网络已断开。已上传分片保留在对象存储，网络恢复后点击“继续上传”。');
    }
});

async function loadSystemInfo() {
    try {
        const response = await fetch('/api/sha256/system', {cache: 'no-store'});
        if (!response.ok) return;
        const info = await response.json();
        brokerBadge.textContent = (info.broker || 'mq').toUpperCase();
        storageBadge.textContent = (info.storage || 's3').toUpperCase();
    } catch (_) { }
}

function selectFile(file) {
    if (!file) return;
    cancelActiveRequests();
    selectedFile = file;
    currentSession = null;
    fileName.textContent = file.name;
    fileSize.textContent = `${formatBytes(file.size)} · 将使用分片并发直传`;
    fileInfo.classList.remove('hidden');
    taskPanel.classList.add('hidden');
    resultPanel.classList.add('hidden');
    errorBox.classList.add('hidden');
    abortBtn.classList.add('hidden');
    startBtn.disabled = false;
    startBtn.textContent = '开始分片上传';
}

async function startUpload() {
    if (!selectedFile || startBtn.disabled) return;
    clearInterval(pollTimer);
    uploadCancelled = false;
    resultPanel.classList.add('hidden');
    errorBox.classList.add('hidden');
    taskPanel.classList.remove('hidden');
    startBtn.disabled = true;
    abortBtn.classList.remove('hidden');
    taskMeta.textContent = '';

    try {
        updateProgress(0, '正在识别文件', '正在生成断点续传文件指纹…');
        const fingerprint = await computeFingerprint(selectedFile);

        const init = await postJson('/api/sha256/uploads/init', {
            fileName: selectedFile.name,
            fileSize: selectedFile.size,
            lastModified: selectedFile.lastModified,
            contentType: selectedFile.type || 'application/octet-stream',
            fingerprint
        });

        currentSession = init;
        localStorage.setItem(RESUME_PREFIX + fingerprint, JSON.stringify({
            sessionId: init.sessionId,
            fileName: selectedFile.name,
            fileSize: selectedFile.size,
            updatedAt: Date.now()
        }));

        if (init.taskId) {
            abortBtn.classList.add('hidden');
            taskMeta.textContent = `已恢复完成的上传会话 · 任务 ${init.taskId}`;
            updateProgress(0, '上传已完成', '已从断点记录恢复计算任务，继续查询 Worker 状态');
            pollTask(init.taskId);
            return;
        }

        const uploadedSet = new Set((init.uploadedParts || []).map(part => part.partNumber));
        const missing = [];
        for (let part = 1; part <= init.totalParts; part++) {
            if (!uploadedSet.has(part)) missing.push(part);
        }

        const resumeText = init.resumed && uploadedSet.size > 0
            ? `检测到断点：已完成 ${uploadedSet.size}/${init.totalParts} 个分片，从 ${formatBytes(init.uploadedBytes)} 继续`
            : `共 ${init.totalParts} 个分片，每片约 ${formatBytes(init.partSize)}，并发 ${init.recommendedConcurrency}`;
        taskMeta.textContent = `Upload Session ${init.sessionId} · ${resumeText}`;

        if (missing.length > 0) {
            await uploadMissingParts(init, missing, init.uploadedBytes || 0);
        } else {
            updateProgress(100, '分片已齐全', '对象存储中所有分片都已存在，直接执行合并');
        }

        if (uploadCancelled) throw new Error('上传已取消');
        updateProgress(100, '正在合并', '服务端正在校验所有 Part，并调用对象存储 Multipart Complete');
        const complete = await postJson(`/api/sha256/uploads/${init.sessionId}/complete`, {});
        localStorage.removeItem(RESUME_PREFIX + fingerprint);
        abortBtn.classList.add('hidden');
        taskMeta.textContent = `任务 ${complete.taskId} · ${String(complete.broker || '').toUpperCase()}`;
        updateProgress(0, '计算任务已创建', '文件已合并为一个对象，Outbox 将可靠投递到 Worker');
        pollTask(complete.taskId);
    } catch (error) {
        if (uploadCancelled) {
            showError('上传已取消，Multipart 分片已清理。');
        } else {
            showError(`${error.message || '上传中断'}。已完成的分片不会重复上传，点击“继续上传”即可恢复。`);
            startBtn.textContent = '继续上传';
            abortBtn.classList.toggle('hidden', !currentSession);
        }
        startBtn.disabled = false;
    }
}

async function uploadMissingParts(session, missingParts, initialUploadedBytes) {
    let completedBytes = initialUploadedBytes;
    const inflight = new Map();
    const concurrency = Math.max(1, Math.min(8, session.recommendedConcurrency || 4));

    const render = () => {
        let transientBytes = 0;
        inflight.forEach(value => transientBytes += value);
        const loaded = Math.min(selectedFile.size, completedBytes + transientBytes);
        const percent = Math.floor(loaded * 100 / selectedFile.size);
        updateProgress(percent, session.resumed ? '断点续传中' : '分片上传中',
            `${formatBytes(loaded)} / ${formatBytes(selectedFile.size)} · ${concurrency} 路并发`);
    };
    render();

    for (let offset = 0; offset < missingParts.length; offset += PRESIGN_BATCH_SIZE) {
        if (uploadCancelled) throw new Error('上传已取消');
        const partNumbers = missingParts.slice(offset, offset + PRESIGN_BATCH_SIZE);
        const signed = await postJson(`/api/sha256/uploads/${session.sessionId}/presign`, {partNumbers});
        const urlMap = new Map(signed.parts.map(item => [item.partNumber, item.url]));

        await runPool(partNumbers, concurrency, async partNumber => {
            const start = (partNumber - 1) * session.partSize;
            const end = Math.min(selectedFile.size, start + session.partSize);
            const blob = selectedFile.slice(start, end);
            const url = urlMap.get(partNumber);
            if (!url) throw new Error(`分片 ${partNumber} 缺少预签名 URL`);

            await uploadPartWithRetry(partNumber, blob, url, inflight, render);
            completedBytes += blob.size;
            inflight.delete(partNumber);
            render();
        });
    }
}

async function uploadPartWithRetry(partNumber, blob, url, inflight, render) {
    let lastError;
    for (let attempt = 1; attempt <= PART_RETRY_COUNT; attempt++) {
        if (uploadCancelled) throw new Error('上传已取消');
        try {
            await uploadPart(partNumber, blob, url, inflight, render);
            return;
        } catch (error) {
            lastError = error;
            inflight.delete(partNumber);
            render();
            if (attempt < PART_RETRY_COUNT) await sleep(500 * Math.pow(2, attempt - 1));
        }
    }
    throw new Error(`分片 ${partNumber} 上传失败：${lastError?.message || '网络错误'}`);
}

function uploadPart(partNumber, blob, url, inflight, render) {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        activeXhrs.add(xhr);
        xhr.open('PUT', url, true);
        xhr.timeout = 10 * 60 * 1000;
        xhr.upload.onprogress = event => {
            if (event.lengthComputable) {
                inflight.set(partNumber, event.loaded);
                render();
            }
        };
        xhr.onload = () => {
            activeXhrs.delete(xhr);
            if (xhr.status >= 200 && xhr.status < 300) resolve();
            else reject(new Error(`HTTP ${xhr.status}`));
        };
        xhr.onerror = () => {
            activeXhrs.delete(xhr);
            reject(new Error('网络连接失败'));
        };
        xhr.ontimeout = () => {
            activeXhrs.delete(xhr);
            reject(new Error('上传超时'));
        };
        xhr.onabort = () => {
            activeXhrs.delete(xhr);
            reject(new Error('上传已中止'));
        };
        xhr.send(blob);
    });
}

async function abortUpload() {
    if (!currentSession) return;
    uploadCancelled = true;
    cancelActiveRequests();
    startBtn.disabled = true;
    abortBtn.disabled = true;
    try {
        await fetch(`/api/sha256/uploads/${currentSession.sessionId}`, {method: 'DELETE'});
        Object.keys(localStorage)
            .filter(key => key.startsWith(RESUME_PREFIX))
            .forEach(key => {
                try {
                    const value = JSON.parse(localStorage.getItem(key));
                    if (value?.sessionId === currentSession.sessionId) localStorage.removeItem(key);
                } catch (_) { }
            });
        currentSession = null;
        updateProgress(0, '已取消', '对象存储 Multipart Upload 已终止，未合并分片将被释放');
    } finally {
        startBtn.disabled = false;
        startBtn.textContent = '重新上传';
        abortBtn.disabled = false;
        abortBtn.classList.add('hidden');
    }
}

function cancelActiveRequests() {
    activeXhrs.forEach(xhr => {
        try { xhr.abort(); } catch (_) { }
    });
    activeXhrs.clear();
}

async function runPool(items, concurrency, worker) {
    let index = 0;
    async function runner() {
        while (true) {
            const current = index++;
            if (current >= items.length) return;
            await worker(items[current]);
        }
    }
    await Promise.all(Array.from({length: Math.min(concurrency, items.length)}, runner));
}

async function computeFingerprint(file) {
    const sampleSize = Math.min(1024 * 1024, file.size);
    const first = new Uint8Array(await file.slice(0, sampleSize).arrayBuffer());
    const lastStart = Math.max(0, file.size - sampleSize);
    const last = new Uint8Array(await file.slice(lastStart, file.size).arrayBuffer());
    const meta = new TextEncoder().encode(`${file.name}|${file.size}|${file.lastModified}|${file.type}|`);
    const bytes = new Uint8Array(meta.length + first.length + last.length);
    bytes.set(meta, 0);
    bytes.set(first, meta.length);
    bytes.set(last, meta.length + first.length);

    if (globalThis.crypto?.subtle) {
        const digest = await crypto.subtle.digest('SHA-256', bytes);
        return Array.from(new Uint8Array(digest)).map(b => b.toString(16).padStart(2, '0')).join('');
    }
    return fallbackFingerprint(bytes);
}

function fallbackFingerprint(bytes) {
    let h1 = 0x811c9dc5;
    let h2 = 0x9e3779b9;
    for (const b of bytes) {
        h1 ^= b;
        h1 = Math.imul(h1, 0x01000193);
        h2 ^= (b + ((h2 << 6) >>> 0) + (h2 >>> 2));
        h2 >>>= 0;
    }
    return `fallback-${(h1 >>> 0).toString(16).padStart(8, '0')}${(h2 >>> 0).toString(16).padStart(8, '0')}`;
}

async function postJson(url, body) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    });
    let data = {};
    try { data = await response.json(); } catch (_) { }
    if (!response.ok) throw new Error(data.error || `请求失败，HTTP ${response.status}`);
    return data;
}

function pollTask(taskId) {
    clearInterval(pollTimer);
    const poll = async () => {
        try {
            const response = await fetch('/api/sha256/tasks/' + taskId, {cache: 'no-store'});
            const task = await response.json();
            if (!response.ok) throw new Error(task.error || ('任务查询失败，HTTP ' + response.status));

            if (task.status === 'QUEUED') {
                updateProgress(0, '排队中', 'Outbox/MQ 正在等待可用 Worker');
            } else if (task.status === 'RETRYING') {
                updateProgress(task.progress, '等待重试', `已重试 ${task.retryCount || 0} 次 · ${task.error || '稍后重新计算'}`);
            } else if (task.status === 'RUNNING') {
                updateProgress(task.progress, 'Worker 正在计算', `${formatBytes(task.processedBytes)} / ${formatBytes(task.totalBytes)}`);
            } else if (task.status === 'SUCCESS') {
                clearInterval(pollTimer);
                updateProgress(100, '计算完成', `${formatBytes(task.totalBytes)} · ${String(task.broker || '').toUpperCase()}`);
                hashValue.textContent = task.sha256;
                resultPanel.classList.remove('hidden');
                startBtn.disabled = false;
                startBtn.textContent = '再次计算';
            } else if (task.status === 'FAILED') {
                clearInterval(pollTimer);
                showError(task.error || '计算失败');
                startBtn.disabled = false;
            } else if (task.status === 'DEAD_LETTERED') {
                clearInterval(pollTimer);
                showError(`任务已进入死信队列：${task.error || '重试次数已耗尽'}`);
                startBtn.disabled = false;
            }
        } catch (error) {
            clearInterval(pollTimer);
            showError(error.message);
            startBtn.disabled = false;
        }
    };
    poll();
    pollTimer = setInterval(poll, 700);
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

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
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
