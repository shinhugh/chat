// Required: /public/apiHttp.js

// TODO: Implement ability to insert messages anywhere in history by timestamp
// TODO: Implement message fetching on scroll-up / pull-down

// --------------------------------------------------

// Constants

const userApiUrl = '/api/user';
const loginApiUrl = '/api/login';
const messageSocketUrl = (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/api/messages';

// --------------------------------------------------

// DOM elements

const userName = document.getElementById('p_user_name');
const logoutSubmit = document.getElementById('button_logout_submit');
const chatHistorySection = document.getElementById('div_chat_history_section');
const chatComposerContent = document.getElementById('textarea_chat_composer_content');
const chatComposerSubmit = document.getElementById('button_chat_composer_submit');
const overlayNotification = document.getElementById('p_overlay_notification');

// --------------------------------------------------

// Open websocket for messages

var messageSocket = null;
if ('WebSocket' in window) {
  messageSocket = new WebSocket(messageSocketUrl);

  messageSocket.onmessage = (messageToClientWrapper) => {
    let messageToClient = JSON.parse(messageToClientWrapper.data);
    if (messageToClient.messagesData && messageToClient.messagesData.messages) {
      for (const message of messageToClient.messagesData.messages) {
        addMessageToUI(message);
      }
    }
  };

  messageSocket.onopen = () => {
    let messageToServer = {
      'fetchMessagesData': {
        'timestamp': (new Date()).toISOString(),
        'limit': 10
      }
    };
    messageSocket.send(JSON.stringify(messageToServer));
  };

  messageSocket.onclose = () => {
    showOverlayNotification('Unable to connect', 2000);
  };
} else {
  showOverlayNotification('Unable to connect', 2000);
}

// --------------------------------------------------

// Send messages

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
  let messageToServer = {
    'sendMessageData': {
      'content': messageContent
    }
  };
  messageSocket.send(JSON.stringify(messageToServer));
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

// Display user name

apiHttp.read(userApiUrl, null)
.then((obj) => {
  userName.innerHTML = obj['name'];
})
.catch(() => {
  showOverlayNotification('Unable to fetch user name', 2000);
  userName.innerHTML = '';
});

// --------------------------------------------------

// Logout

logoutSubmit.onclick = () => {
  logoutSubmit.disabled = true;
  apiHttp.delete(loginApiUrl, null)
  .then(() => {
    location.href = '/login';
  })
  .catch(() => {
    location.href = '/login';
  });
};

// --------------------------------------------------

// Create entry for message history UI

function addMessageToUI(message) {
  let chatEntry;
  if (message.outgoing) {
    chatEntry = createOutgoingMessage(message);
  } else {
    chatEntry = createIncomingMessage(message);
  }
  chatHistorySection.append(chatEntry);
  if (bottomScrolled) {
    chatHistorySection.scrollTop = chatHistorySection.scrollHeight;
  }
}

function createIncomingMessage(obj) {
  let container = document.createElement('div');
  container.className = 'incoming_message_container';
  container.append(document.createElement('div'));
  container.lastChild.className = 'incoming_message_header';
  container.lastChild.append(document.createElement('p'));
  container.lastChild.lastChild.className = 'message_user_name';
  container.lastChild.lastChild.innerHTML = obj.userName;
  container.lastChild.append(document.createElement('p'));
  container.lastChild.lastChild.className = 'message_timestamp';
  let timestampDate = new Date(obj.timestamp);
  container.lastChild.lastChild.innerHTML = timestampDate.toLocaleDateString() + ' ' + timestampDate.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit' });
  container.append(document.createElement('p'));
  container.lastChild.className = 'message_content';
  container.lastChild.innerHTML = obj.content;
  return container;
}

function createOutgoingMessage(obj) {
  let container = document.createElement('div');
  container.className = 'outgoing_message_container';
  container.append(document.createElement('div'));
  container.lastChild.className = 'outgoing_message_header';
  container.lastChild.append(document.createElement('p'));
  container.lastChild.lastChild.className = 'message_timestamp';
  let timestampDate = new Date(obj.timestamp);
  container.lastChild.lastChild.innerHTML = timestampDate.toLocaleDateString() + ' ' + timestampDate.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit' });
  container.append(document.createElement('p'));
  container.lastChild.className = 'message_content';
  container.lastChild.innerHTML = obj.content;
  return container;
}

// --------------------------------------------------

// Keep chat history section scrolled to the bottom

var bottomScrolled = true;
chatHistorySection.onscroll = () => {
  bottomScrolled = chatHistorySection.scrollTop + 1 >= (chatHistorySection.scrollHeight - chatHistorySection.offsetHeight);
};

const resizeObserver = new ResizeObserver(() => {
  if (bottomScrolled) {
    chatHistorySection.scroll({
      'top': chatHistorySection.scrollHeight,
      'behavior': 'auto'
    });
  }
});
resizeObserver.observe(chatHistorySection);

// --------------------------------------------------

// Display an overlay notification

var notificationTimeout;

function showOverlayNotification(message, timeout) {
  clearTimeout(notificationTimeout);
  overlayNotification.innerHTML = message;
  overlayNotification.hidden = false;
  notificationTimeout = setTimeout(() => {
    overlayNotification.hidden = true;
    overlayNotification.innerHTML = '';
  }, timeout);
}
