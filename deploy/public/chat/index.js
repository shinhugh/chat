// Required: /public/apiHttp.js

// TODO: Fetch past messages until scrollable
// TODO: Maintain scroll location upon loading past messages

// ==================================================

// Model

// --------------------------------------------------

// Constants

const userApiUrl = '/api/user';
const loginApiUrl = '/api/login';
const messageSocketUrl = (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/api/messages';
const messageBatchSize = 5;

// --------------------------------------------------

// Error events

var errorCallbacks = [];

const registerErrorCallback = (callback) => {
  errorCallbacks.push(callback);
};

const handleError = (error) => {
  for (const callback of errorCallbacks) {
    callback(error);
  }
};

// --------------------------------------------------

// Establish websocket

var messageSocket;

if ('WebSocket' in window) {
  messageSocket = new WebSocket(messageSocketUrl);

  messageSocket.onmessage = (messageToClientWrapper) => {
    let messageToClient = JSON.parse(messageToClientWrapper.data);
    if (messageToClient.messagesData && messageToClient.messagesData.messages) {
      for (let i = messageToClient.messagesData.messages.length - 1; i >= 0 ; i--) {
        messageToClient.messagesData.messages[i].timestamp = new Date(messageToClient.messagesData.messages[i].timestamp);
        apiMessage.addMessage(messageToClient.messagesData.messages[i]);
      }
    }
  };

  messageSocket.onopen = () => {
    requestMostRecentMessages();
  };

  messageSocket.onclose = () => {
    handleError('Unable to connect');
  };
}

else {
  handleError('Unable to connect');
}

// --------------------------------------------------

// Send requests to server

const requestPastMessages = () => {
  if (!messageSocket || messageSocket.readyState != 1) {
    handleError('Unable to fetch messages');
    return false;
  }
  let messageToServer = {
    'fetchMessagesData': {
      'messageId': apiMessage.getOldestMessageId(),
      'limit': messageBatchSize
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
  return true;
};

const requestMostRecentMessages = () => {
  if (!messageSocket || messageSocket.readyState != 1) {
    handleError('Unable to fetch messages');
    return false;
  }
  let messageToServer = {
    'fetchMessagesData': {
      'limit': messageBatchSize
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
  return true;
};

const sendMessage = (messageContent) => {
  if (!messageSocket || messageSocket.readyState != 1) {
    handleError('Unable to send message');
    return false;
  }
  let messageToServer = {
    'sendMessageData': {
      'content': messageContent
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
  return true;
};

// --------------------------------------------------

// Log out

const logOut = () => {
  apiHttp.delete(loginApiUrl, null)
  .then(() => {
    location.href = '/login';
  })
  .catch(() => {
    location.href = '/login';
  });
};

// --------------------------------------------------

// Fetch user name

var userNameCallbacks = [];

const registerUserNameCallback = (callback) => {
  userNameCallbacks.push(callback);
};

const handleUserName = (userName) => {
  for (const callback of userNameCallbacks) {
    callback(userName);
  }
};

apiHttp.read(userApiUrl, null)
.then((user) => {
  handleUserName(user.name);
})
.catch(() => {
  handleError('Unable to fetch user name');
  handleUserName('');
});

// ==================================================

// View

// --------------------------------------------------

// DOM elements

const userName = document.getElementById('p_user_name');
const logoutSubmit = document.getElementById('button_logout_submit');
const chatHistorySection = document.getElementById('div_chat_history_section');
const chatComposerContent = document.getElementById('textarea_chat_composer_content');
const chatComposerSubmit = document.getElementById('button_chat_composer_submit');
const overlayNotification = document.getElementById('p_overlay_notification');

// --------------------------------------------------

// Overlay notification

var notificationTimeout;

const showOverlayNotification = (message, timeout) => {
  clearTimeout(notificationTimeout);
  overlayNotification.innerHTML = message;
  overlayNotification.hidden = false;
  notificationTimeout = setTimeout(() => {
    overlayNotification.hidden = true;
    overlayNotification.innerHTML = '';
  }, timeout);
};

registerErrorCallback((description) => {
  showOverlayNotification(description, 2000);
});

// --------------------------------------------------

// Message composer

chatComposerSubmit.onclick = () => {
  chatComposerSubmit.disabled = true;
  let messageContent = chatComposerContent.value;
  if (messageContent == '') {
    chatComposerSubmit.disabled = false;
    return;
  }
  if (!sendMessage(messageContent)) {
    chatComposerSubmit.disabled = false;
    return;
  }
  chatComposerContent.value = '';
  chatComposerSubmit.disabled = false;
};

chatComposerContent.addEventListener('keypress', (event) => {
  if (event.key == 'Enter') {
    event.preventDefault();
    chatComposerSubmit.click();
  }
});

chatComposerContent.focus();

// --------------------------------------------------

// Set user name text

registerUserNameCallback((userName) => {
  userName.innerHTML = userName;
});

// --------------------------------------------------

// Logout button

logoutSubmit.onclick = () => {
  logoutSubmit.disabled = true;
  logOut();
};

// --------------------------------------------------

// Create entry for message history UI

const createIncomingMessageView = (message) => {
  let container = document.createElement('div');
  container.className = 'incoming_message_container';
  container.append(document.createElement('div'));
  container.lastChild.className = 'incoming_message_header';
  container.lastChild.append(document.createElement('p'));
  container.lastChild.lastChild.className = 'message_user_name';
  container.lastChild.lastChild.innerHTML = message.userName;
  container.lastChild.append(document.createElement('p'));
  container.lastChild.lastChild.className = 'message_timestamp';
  let timestampDate = new Date(message.timestamp);
  container.lastChild.lastChild.innerHTML = timestampDate.toLocaleDateString() + ' ' + timestampDate.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit' });
  container.append(document.createElement('p'));
  container.lastChild.className = 'message_content';
  container.lastChild.innerHTML = message.content;
  return container;
};

const createOutgoingMessageView = (message) => {
  let container = document.createElement('div');
  container.className = 'outgoing_message_container';
  container.append(document.createElement('div'));
  container.lastChild.className = 'outgoing_message_header';
  container.lastChild.append(document.createElement('p'));
  container.lastChild.lastChild.className = 'message_timestamp';
  let timestampDate = new Date(message.timestamp);
  container.lastChild.lastChild.innerHTML = timestampDate.toLocaleDateString() + ' ' + timestampDate.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit' });
  container.append(document.createElement('p'));
  container.lastChild.className = 'message_content';
  container.lastChild.innerHTML = message.content;
  return container;
};

apiMessage.registerNewMessageCallback((index, message) => {
  let chatEntry;
  if (message.outgoing) {
    chatEntry = createOutgoingMessageView(message);
  } else {
    chatEntry = createIncomingMessageView(message);
  }
  chatHistorySection.insertBefore(chatEntry, chatHistorySection.children[index]);
  scrollIfLocked();
});

// --------------------------------------------------

// Keep chat history section scrolled to the bottom

var scrollBottomLocked = true;

chatHistorySection.onscroll = () => {
  scrollBottomLocked = chatHistorySection.scrollTop + 1 >= (chatHistorySection.scrollHeight - chatHistorySection.offsetHeight);
  if (chatHistorySection.scrollTop == 0) {
    requestPastMessages();
  }
};

const resizeObserver = new ResizeObserver(() => {
  if (scrollBottomLocked) {
    chatHistorySection.scroll({
      'top': chatHistorySection.scrollHeight,
      'behavior': 'auto'
    });
  }
});
resizeObserver.observe(chatHistorySection);

const scrollIfLocked = () => {
  if (scrollBottomLocked) {
    chatHistorySection.scrollTop = chatHistorySection.scrollHeight;
  }
};
