// Required: /public/apiHttp.js

// TODO: Chat history UI should reflect apiMessage state
// TODO: Fetch past messages until scrollable

// ==================================================

// Model

// --------------------------------------------------

// Constants

const userApiUrl = '/api/user';
const loginApiUrl = '/api/login';
const messageSocketUrl = (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/api/messages';
const messageBatchSize = 5;

// --------------------------------------------------

// Establish websocket

var messageSocket;

if ('WebSocket' in window) {
  messageSocket = new WebSocket(messageSocketUrl);

  messageSocket.onmessage = (messageToClientWrapper) => {
    let messageToClient = JSON.parse(messageToClientWrapper.data);
    if (messageToClient.messagesData && messageToClient.messagesData.messages) {
      for (const message of messageToClient.messagesData.messages) {
        message.timestamp = new Date(message.timestamp);
        apiMessage.addMessage(message);
      }
    }
  };

  messageSocket.onopen = () => {
    requestMostRecentMessages();
  };

  messageSocket.onclose = () => {
    showOverlayNotification('Unable to connect', 2000);
  };
}

else {
  showOverlayNotification('Unable to connect', 2000);
}

// --------------------------------------------------

// Send requests to server

const requestPastMessages = () => {
  if (!messageSocket) {
    return;
  }
  let messageToServer = {
    'fetchMessagesData': {
      'messageId': apiMessage.getOldestMessageId(),
      'limit': messageBatchSize
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
};

const requestMostRecentMessages = () => {
  if (!messageSocket) {
    return;
  }
  let messageToServer = {
    'fetchMessagesData': {
      'limit': messageBatchSize
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
};

const sendMessage = (messageContent) => {
  let messageToServer = {
    'sendMessageData': {
      'content': messageContent
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
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

apiHttp.read(userApiUrl, null)
.then((user) => {
  setUserNameText(user.name);
})
.catch(() => {
  showOverlayNotification('Unable to fetch user name', 2000);
  setUserNameText('');
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


// --------------------------------------------------

// Message composer

chatComposerSubmit.onclick = () => {
  chatComposerSubmit.disabled = true;
  let messageContent = chatComposerContent.value;
  if (messageContent == '') {
    chatComposerSubmit.disabled = false;
    return;
  }
  if (!messageSocket || messageSocket.readyState != 1) {
    showOverlayNotification('Unable to send message', 2000);
    chatComposerSubmit.disabled = false;
    return;
  }
  sendMessage(messageContent);
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

const setUserNameText = (userName) => {
  userName.innerHTML = userName;
};

// --------------------------------------------------

// Logout button

logoutSubmit.onclick = () => {
  logoutSubmit.disabled = true;
  logOut();
};

// --------------------------------------------------

// Create entry for message history UI

const addMessageView = (message) => {
  let chatEntry;
  if (message.outgoing) {
    chatEntry = createOutgoingMessageView(message);
  } else {
    chatEntry = createIncomingMessageView(message);
  }
  chatHistorySection.append(chatEntry); // TODO
  scrollIfLocked();
};

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
