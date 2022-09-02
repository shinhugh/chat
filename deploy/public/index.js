// Required: /public/api.js

// --------------------------------------------------

// Constants

const userApi = '/api/user';
const loginApi = '/api/login';
const chatApi = '/api/chat';

// --------------------------------------------------

// DOM elements

const userName = document.getElementById('p_user_name');
const logoutSubmit = document.getElementById('button_logout_submit');
const chatHistorySection = document.getElementById('div_chat_history_section');
const chatComposerContent = document.getElementById('textarea_chat_composer_content');
const chatComposerSubmit = document.getElementById('button_chat_composer_submit');
const overlayNotification = document.getElementById('p_overlay_notification');

// --------------------------------------------------

// Sync messages

// var syncStartTime = null;
// var syncFinishTime = new Date();

// apiRead(chatApi, {
//   'finishTime': syncFinishTime.toISOString()
// })
// .then((obj) => {
//   syncStartTime = syncFinishTime;
//   appendToChatHistory(obj);
// })
// .catch(() => {
//   console.error('Unable to fetch messages');
// });

// setInterval(() => {
//   syncFinishTime = new Date();
//   if (syncStartTime) {
//     apiRead(chatApi, {
//       'startTime': syncStartTime.toISOString(),
//       'finishTime': syncFinishTime.toISOString()
//     })
//     .then((obj) => {
//       syncStartTime = syncFinishTime;
//       appendToChatHistory(obj);
//     })
//     .catch(() => {
//       console.error('Unable to fetch messages');
//     });
//     return;
//   }
//   apiRead(chatApi, {
//     'finishTime': syncFinishTime.toISOString()
//   })
//   .then((obj) => {
//     syncStartTime = syncFinishTime;
//     appendToChatHistory(obj);
//   })
//   .catch(() => {
//     console.error('Unable to fetch messages');
//   });
// }, 200);

// --------------------------------------------------

// Display user name

apiRead(userApi, null)
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
  apiDelete(loginApi, null)
  .then(() => {
    location.href = '/login';
  })
  .catch(() => {
    location.href = '/login';
  });
};

// --------------------------------------------------

// Send messages

// chatComposerSubmit.onclick = () => {
//   chatComposerSubmit.disabled = true;
//   let messageContent = chatComposerContent.value;
//   if (messageContent == '') {
//     chatComposerSubmit.disabled = false;
//     return;
//   }
//   let obj = {
//     'content': messageContent
//   };
//   apiCreate(chatApi, null, obj)
//   .then(() => {
//     chatComposerContent.value = '';
//     chatComposerSubmit.disabled = false;
//   })
//   .catch(() => {
//     showOverlayNotification('Unable to send message', 2000);
//     chatComposerSubmit.disabled = false;
//   });
// };

chatComposerContent.addEventListener('keypress', (event) => {
  if (event.key == 'Enter') {
    event.preventDefault();
    chatComposerSubmit.click();
  }
});

chatComposerContent.focus();

// --------------------------------------------------

// Create entry in message history UI

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
  bottomScrolled = chatHistorySection.scrollTop == (chatHistorySection.scrollHeight - chatHistorySection.offsetHeight);
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

// Append messages to the chat history UI

function appendToChatHistory(obj) {
  let indexReceived = 0;
  let indexSent = 0;
  while (indexReceived < obj['received'].length && indexSent < obj['sent'].length) {
    let receivedMessage = obj['received'][indexReceived];
    let sentMessage = obj['sent'][indexSent];
    let receivedMessageTimestamp = Date.parse(receivedMessage.timestamp);
    let sentMessageTimestamp = Date.parse(sentMessage.timestamp);
    if (receivedMessageTimestamp < sentMessageTimestamp) {
      chatHistorySection.append(createIncomingMessage(receivedMessage));
      indexReceived++;
    } else {
      chatHistorySection.append(createOutgoingMessage(sentMessage));
      indexSent++;
    }
  }
  while (indexReceived < obj['received'].length) {
    let receivedMessage = obj['received'][indexReceived];
    chatHistorySection.append(createIncomingMessage(receivedMessage));
    indexReceived++;
  }
  while (indexSent < obj['sent'].length) {
    let sentMessage = obj['sent'][indexSent];
    chatHistorySection.append(createOutgoingMessage(sentMessage));
    indexSent++;
  }
  if ((indexReceived > 0 || indexSent > 0) && bottomScrolled) {
    chatHistorySection.scroll({
      'top': chatHistorySection.scrollHeight,
      'behavior': 'smooth'
    });
  }
}

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