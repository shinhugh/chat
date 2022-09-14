// Required: /public/apiHttp.js
// Required: /public/apiMessage.js

// TODO: Fetch past messages until scrollable
// TODO: Maintain scroll location upon loading past messages

// --------------------------------------------------

// Constants

const userApiUrl = '/api/user';
const loginApiUrl = '/api/login';
const batchSizeLimit = 15;

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

apiMessage.registerErrorDescriptionCallback((description) => {
  showOverlayNotification(description, 2000);
});

// --------------------------------------------------

// Log out

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

// Fetch user name

apiHttp.read(userApiUrl, null)
.then((user) => {
  userName.innerHTML = user.name;
})
.catch(() => {
  showOverlayNotification('Unable to fetch user name', 2000);
  userName.innerHTML = '';
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
  if (!apiMessage.sendMessage(messageContent)) {
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

// Add new messages to UI

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

apiMessage.registerNewMessageCallback((message, index) => {
  let oldHeight = chatHistorySection.scrollHeight;
  let chatEntry;
  if (message.outgoing) {
    chatEntry = createOutgoingMessageView(message);
  } else {
    chatEntry = createIncomingMessageView(message);
  }
  if (index < chatHistorySection.children.length) {
    chatHistorySection.insertBefore(chatEntry, chatHistorySection.children[index]);
  } else {
    chatHistorySection.append(chatEntry);
  }
  let scrollDistance = chatHistorySection.scrollHeight - oldHeight;
  if (scrollBottomLocked) {
    chatHistorySection.scrollTop = chatHistorySection.scrollHeight;
  } else {
    chatHistorySection.scrollTop += scrollDistance;
  }
});

// --------------------------------------------------

// Manage chat history scrolling

var scrollBottomLocked = true;

chatHistorySection.onscroll = () => {
  scrollBottomLocked = chatHistorySection.scrollTop + 1 >= (chatHistorySection.scrollHeight - chatHistorySection.offsetHeight);
  if (chatHistorySection.scrollTop == 0) {
    apiMessage.requestPastMessages(batchSizeLimit);
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

// --------------------------------------------------

// Initialize message API

apiMessage.initialize(batchSizeLimit);
