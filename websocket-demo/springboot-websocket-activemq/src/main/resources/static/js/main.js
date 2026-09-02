'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');
var onlineUsersElement = document.querySelector('#onlineUsers');
var onlineCountElement = document.querySelector('#onlineCount');
var currentUserElement = document.querySelector('#currentUser');
var selectedRecipientLabel = document.querySelector('#selectedRecipientLabel');
var chatTargetElement = document.querySelector('#chatTarget');
var allUsersButton = document.querySelector('#allUsersButton');

var stompClient = null;
var username = null;
var selectedRecipient = 'all';
var onlineUsers = [];

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if (username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');
        currentUserElement.textContent = '当前用户：' + username;

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.connect({ userid: username }, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    // 群聊消息。
    stompClient.subscribe('/topic/public', onMessageReceived);

    // 单聊消息必须订阅 /user 前缀；服务端使用 convertAndSendToUser 路由。
    stompClient.subscribe('/user/queue/private', onMessageReceived);

    // 服务端推送完整在线用户列表。
    stompClient.subscribe('/topic/online-users', onOnlineUsersReceived);

    connectingElement.classList.add('hidden');
    loadOnlineUsers();
    messageInput.focus();
}

function onError() {
    connectingElement.textContent = 'WebSocket 连接失败，请刷新页面后重试。';
    connectingElement.style.color = 'red';
}

function loadOnlineUsers() {
    fetch('/getOnlineUsers')
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Failed to load online users');
            }
            return response.json();
        })
        .then(function (users) {
            renderOnlineUsers(users || []);
        })
        .catch(function (error) {
            console.error(error);
        });
}

function sendMessage(event) {
    var messageContent = messageInput.value.trim();

    if (messageContent && stompClient && stompClient.connected) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT',
            to: selectedRecipient
        };

        // 群聊、单聊统一使用同一个后端入口。
        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
        messageInput.value = '';
        messageInput.focus();
    }
    event.preventDefault();
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    var messageElement = document.createElement('li');

    if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        appendText(messageElement, message.sender + ' 进入了消息室');
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        appendText(messageElement, message.sender + ' 离开了消息室');
    } else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        avatarElement.appendChild(document.createTextNode(initialOf(message.sender)));
        avatarElement.style.backgroundColor = getAvatarColor(message.sender || 'system');
        messageElement.appendChild(avatarElement);

        var headerElement = document.createElement('div');
        headerElement.classList.add('message-header');

        var usernameElement = document.createElement('span');
        usernameElement.appendChild(document.createTextNode(message.sender || '未知用户'));
        headerElement.appendChild(usernameElement);

        var scopeElement = document.createElement('em');
        if (message.to === 'all') {
            scopeElement.textContent = '群聊';
            scopeElement.classList.add('group-tag');
        } else {
            scopeElement.textContent = '私聊 → ' + message.to;
            scopeElement.classList.add('private-tag');
        }
        headerElement.appendChild(scopeElement);
        messageElement.appendChild(headerElement);

        appendText(messageElement, message.content || '');
    }

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onOnlineUsersReceived(payload) {
    var users = JSON.parse(payload.body);
    renderOnlineUsers(users || []);
}

function renderOnlineUsers(users) {
    onlineUsers = Array.from(new Set(users)).sort(function (a, b) {
        return a.localeCompare(b);
    });

    onlineCountElement.textContent = onlineUsers.length + ' 人在线';
    onlineUsersElement.innerHTML = '';

    onlineUsers.forEach(function (user) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'recipient-item';
        button.dataset.recipient = user;

        if (selectedRecipient === user) {
            button.classList.add('active');
        }
        if (user === username) {
            button.classList.add('self-user');
            button.title = '这是你自己';
        }

        var avatar = document.createElement('span');
        avatar.className = 'recipient-avatar';
        avatar.textContent = initialOf(user);
        avatar.style.backgroundColor = getAvatarColor(user);

        var info = document.createElement('span');
        info.className = 'recipient-info';

        var name = document.createElement('strong');
        name.textContent = user === username ? user + '（我）' : user;

        var hint = document.createElement('small');
        hint.textContent = user === username ? '当前登录用户' : '点击进行单聊';

        info.appendChild(name);
        info.appendChild(hint);
        button.appendChild(avatar);
        button.appendChild(info);

        button.addEventListener('click', function () {
            if (user !== username) {
                selectRecipient(user);
            }
        });

        onlineUsersElement.appendChild(button);
    });

    if (selectedRecipient !== 'all' && onlineUsers.indexOf(selectedRecipient) === -1) {
        selectRecipient('all');
    } else {
        refreshRecipientSelection();
    }
}

function selectRecipient(recipient) {
    selectedRecipient = recipient;
    selectedRecipientLabel.textContent = recipient === 'all' ? '所有人' : recipient;
    chatTargetElement.textContent = recipient === 'all'
        ? '当前发送给：所有人（群聊）'
        : '当前发送给：' + recipient + '（私聊）';
    messageInput.placeholder = recipient === 'all'
        ? '输入群发消息...'
        : '输入发给 ' + recipient + ' 的私聊消息...';
    refreshRecipientSelection();
    messageInput.focus();
}

function refreshRecipientSelection() {
    allUsersButton.classList.toggle('active', selectedRecipient === 'all');
    var recipientButtons = onlineUsersElement.querySelectorAll('.recipient-item');
    recipientButtons.forEach(function (button) {
        button.classList.toggle('active', button.dataset.recipient === selectedRecipient);
    });
}

function appendText(container, text) {
    var textElement = document.createElement('p');
    textElement.appendChild(document.createTextNode(text));
    container.appendChild(textElement);
}

function initialOf(value) {
    return value && value.length > 0 ? value.charAt(0).toUpperCase() : '?';
}

function getAvatarColor(messageSender) {
    var value = messageSender || '?';
    var hash = 0;
    for (var i = 0; i < value.length; i++) {
        hash = 31 * hash + value.charCodeAt(i);
    }
    return colors[Math.abs(hash % colors.length)];
}

allUsersButton.addEventListener('click', function () {
    selectRecipient('all');
});
usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
