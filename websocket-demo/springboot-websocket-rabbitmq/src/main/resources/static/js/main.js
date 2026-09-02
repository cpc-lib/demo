'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');
var targetUserSelect = document.querySelector('#targetUser');
var onlineCountElement = document.querySelector('#onlineCount');
var onlineUsersTextElement = document.querySelector('#onlineUsersText');
var onlineUsersListElement = document.querySelector('#onlineUsersList');
var currentUserElement = document.querySelector('#currentUser');
var currentUserAvatarElement = document.querySelector('#currentUserAvatar');
var refreshUsersBtn = document.querySelector('#refreshUsersBtn');
var conversationTitleElement = document.querySelector('#conversationTitle');
var conversationSubtitleElement = document.querySelector('#conversationSubtitle');
var composerTargetElement = document.querySelector('#composerTarget');

var stompClient = null;
var username = null;
var onlineUsers = [];

var colors = ['#5B5BD6', '#32A77B', '#2498C7', '#D96662', '#D69B2D', '#B86AA4', '#D77D32', '#348E8B'];

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if (username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');
        currentUserElement.textContent = username;
        currentUserAvatarElement.textContent = username.charAt(0) || '?';
        currentUserAvatarElement.style.background = getAvatarColor(username);

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({ userid: username }, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    stompClient.subscribe('/topic/public', onMessageReceived);
    stompClient.subscribe('/user/topic/msg', onMessageReceived);

    stompClient.send('/app/chat.addUser', {}, JSON.stringify({
        sender: username,
        type: 'JOIN',
        to: 'all'
    }));

    refreshOnlineUsers();
    connectingElement.classList.add('hidden');
    messageInput.focus();
}

function onError(error) {
    connectingElement.classList.remove('hidden');
    connectingElement.classList.add('error');
    connectingElement.innerHTML = '<span>连接失败，请确认 WebSocket / RabbitMQ STOMP 服务已启动后刷新页面重试。</span>';
    console.error(error);
}

function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    var targetUser = targetUserSelect.value || 'all';

    if (messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT',
            to: targetUser
        };

        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
        messageInput.value = '';
        messageInput.focus();
    }
    event.preventDefault();
}

function refreshOnlineUsers() {
    if (refreshUsersBtn) {
        refreshUsersBtn.disabled = true;
    }

    fetch('/getOnlineUsers', { cache: 'no-store' })
        .then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(function (users) {
            users = Array.isArray(users) ? users : [];
            renderOnlineUsers(users);
        })
        .catch(function (error) {
            console.error('Failed to load online users:', error);
        })
        .finally(function () {
            if (refreshUsersBtn) {
                refreshUsersBtn.disabled = false;
            }
        });
}

function renderOnlineUsers(users) {
    onlineUsers = users;
    var oldTarget = targetUserSelect.value || 'all';

    while (targetUserSelect.options.length > 1) {
        targetUserSelect.remove(1);
    }

    users.forEach(function (user) {
        if (!user || user === username) {
            return;
        }
        var option = document.createElement('option');
        option.value = user;
        option.textContent = user + '（私聊）';
        targetUserSelect.appendChild(option);
    });

    var targetStillExists = Array.prototype.some.call(targetUserSelect.options, function (option) {
        return option.value === oldTarget;
    });
    targetUserSelect.value = targetStillExists ? oldTarget : 'all';

    onlineCountElement.textContent = users.length + ' 在线';
    onlineUsersTextElement.textContent = '在线用户：' + (users.length ? users.join('、') : '-');
    renderMemberList(users);
    updateConversationTarget();
}

function renderMemberList(users) {
    if (!onlineUsersListElement) {
        return;
    }

    onlineUsersListElement.innerHTML = '';

    if (!users.length) {
        var empty = document.createElement('div');
        empty.className = 'members-empty';
        empty.textContent = '暂无在线成员';
        onlineUsersListElement.appendChild(empty);
        return;
    }

    users.forEach(function (user) {
        if (!user) {
            return;
        }

        var isSelf = user === username;
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'member-item';
        button.dataset.user = user;

        if (!isSelf && targetUserSelect.value === user) {
            button.classList.add('active');
        }

        var avatar = document.createElement('span');
        avatar.className = 'member-avatar';
        avatar.textContent = user.charAt(0) || '?';
        avatar.style.backgroundColor = getAvatarColor(user);

        var info = document.createElement('span');
        info.className = 'member-info';

        var name = document.createElement('span');
        name.className = 'member-name';
        name.textContent = user;

        var status = document.createElement('span');
        status.className = 'member-status';
        status.textContent = isSelf ? '当前账号' : '在线 · 点击私聊';

        info.appendChild(name);
        info.appendChild(status);
        button.appendChild(avatar);
        button.appendChild(info);

        if (isSelf) {
            var you = document.createElement('span');
            you.className = 'member-you';
            you.textContent = '我';
            button.appendChild(you);
            button.disabled = true;
        } else {
            button.addEventListener('click', function () {
                targetUserSelect.value = user;
                updateConversationTarget();
                messageInput.focus();
            });
        }

        onlineUsersListElement.appendChild(button);
    });
}

function updateConversationTarget() {
    var target = targetUserSelect.value || 'all';
    var isGroup = target === 'all';

    if (isGroup) {
        conversationTitleElement.textContent = '所有在线用户';
        conversationSubtitleElement.textContent = '当前为群聊模式，消息会发送给所有在线成员';
        composerTargetElement.innerHTML = '<span class="target-dot"></span><span>发送给：<strong>所有在线用户</strong></span>';
    } else {
        conversationTitleElement.textContent = target;
        conversationSubtitleElement.textContent = '当前为私聊模式，消息仅发送给 ' + target;
        composerTargetElement.innerHTML = '<span class="target-dot"></span><span>私聊：<strong>' + escapeHtml(target) + '</strong></span>';
    }

    if (onlineUsersListElement) {
        Array.prototype.forEach.call(onlineUsersListElement.querySelectorAll('.member-item'), function (item) {
            item.classList.toggle('active', !isGroup && item.dataset.user === target);
        });
    }
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    removeEmptyState();

    if (message.type === 'JOIN' || message.type === 'LEAVE') {
        appendEventMessage(message);
        refreshOnlineUsers();
        return;
    }

    appendChatMessage(message);
}

function appendEventMessage(message) {
    var messageElement = document.createElement('li');
    messageElement.className = 'event-message';

    var text = document.createElement('span');
    text.textContent = message.sender + (message.type === 'JOIN' ? ' 上线了' : ' 离线了');
    messageElement.appendChild(text);

    messageArea.appendChild(messageElement);
    scrollMessagesToBottom();
}

function appendChatMessage(message) {
    var messageElement = document.createElement('li');
    messageElement.className = 'chat-message';

    var isOwn = message.sender === username;
    var isPrivate = message.to && message.to !== 'all';

    if (isOwn) {
        messageElement.classList.add('own-message');
    }
    if (isPrivate) {
        messageElement.classList.add('private-message');
    }

    var avatarElement = document.createElement('i');
    avatarElement.className = 'message-avatar';
    avatarElement.textContent = message.sender ? message.sender.charAt(0) : '?';
    avatarElement.style.backgroundColor = getAvatarColor(message.sender || '?');

    var body = document.createElement('div');
    body.className = 'message-body';

    var meta = document.createElement('div');
    meta.className = 'message-meta';

    var sender = document.createElement('span');
    sender.className = 'message-sender';
    sender.textContent = isOwn ? '我' : (message.sender || 'unknown');

    var type = document.createElement('span');
    type.className = 'message-type';
    type.textContent = isPrivate ? '私聊' : '群聊';

    var time = document.createElement('span');
    time.className = 'message-time';
    time.textContent = formatCurrentTime();

    meta.appendChild(sender);
    meta.appendChild(type);
    meta.appendChild(time);

    var bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.textContent = message.content || '';

    body.appendChild(meta);
    body.appendChild(bubble);
    messageElement.appendChild(avatarElement);
    messageElement.appendChild(body);
    messageArea.appendChild(messageElement);

    scrollMessagesToBottom();
}

function removeEmptyState() {
    var emptyState = document.querySelector('#emptyMessageState');
    if (emptyState) {
        emptyState.remove();
    }
}

function scrollMessagesToBottom() {
    messageArea.scrollTop = messageArea.scrollHeight;
}

function formatCurrentTime() {
    var now = new Date();
    var hours = String(now.getHours()).padStart(2, '0');
    var minutes = String(now.getMinutes()).padStart(2, '0');
    return hours + ':' + minutes;
}

function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }
    return colors[Math.abs(hash % colors.length)];
}

function escapeHtml(value) {
    var div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
}

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
targetUserSelect.addEventListener('change', updateConversationTarget);
if (refreshUsersBtn) {
    refreshUsersBtn.addEventListener('click', refreshOnlineUsers);
}
