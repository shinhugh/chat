var apiMessage = {
  'private': {
    'errorDescriptionCallbacks': [],
    'newMessageCallbacks': [],
    'socket': null,
    'socketUrl': (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/api/messages',
    'socketCloseCount': 0,
    'maxSocketCloseCount': 3,
    'messageCount': 0,
    'latestMessageNode': null,
    'oldestMessageNode': null,
    'messageIds': new Map(),

    'invokeErrorDescriptionCallbacks': (errorDescription) => {
      for (const callback of apiMessage.private.errorDescriptionCallbacks) {
        callback(errorDescription);
      }
    },

    'invokeNewMessageCallbacks': (newMessage, index) => {
      for (const callback of apiMessage.private.newMessageCallbacks) {
        callback(newMessage, index);
      }
    },

    'establishSocket': (batchSizeLimit) => {
      if ('WebSocket' in window) {
        apiMessage.private.socket = new WebSocket(apiMessage.private.socketUrl);

        apiMessage.private.socket.onmessage = (messageToClientWrapper) => {
          let messageToClient = JSON.parse(messageToClientWrapper.data);
          if (messageToClient.messagesData && messageToClient.messagesData.messages) {
            for (let i = messageToClient.messagesData.messages.length - 1; i >= 0 ; i--) {
              let message = messageToClient.messagesData.messages[i];
              message.timestamp = new Date(message.timestamp);
              apiMessage.private.addMessage(message);
            }
          }
        };

        apiMessage.private.socket.onopen = () => {
          apiMessage.private.requestMostRecentMessages(batchSizeLimit);
        };

        apiMessage.private.socket.onclose = () => {
          apiMessage.private.socketCloseCount++;
          if (apiMessage.private.socketCloseCount == apiMessage.private.maxSocketCloseCount) {
            apiMessage.private.invokeErrorDescriptionCallbacks('Unable to connect');
          } else {
            apiMessage.private.establishSocket(batchSizeLimit);
          }
        };
      }

      else {
        apiMessage.private.invokeErrorDescriptionCallbacks('Unable to connect');
      }
    },

    'addMessage': (message) => {
      let newMessageNode = {
        'message': message,
        'previous': null,
        'next': null
      };
      if (apiMessage.private.messageIds.has(newMessageNode.message.id)) {
        return;
      }
      let index = 0;
      if (apiMessage.private.oldestMessageNode == null) {
        apiMessage.private.oldestMessageNode = newMessageNode;
        apiMessage.private.latestMessageNode = newMessageNode;
      }
      else if (newMessageNode.message.timestamp >= apiMessage.private.latestMessageNode.message.timestamp) {
        newMessageNode.previous = apiMessage.private.latestMessageNode;
        apiMessage.private.latestMessageNode.next = newMessageNode;
        apiMessage.private.latestMessageNode = newMessageNode;
        index = apiMessage.private.messageCount;
      }
      else if (newMessageNode.message.timestamp < apiMessage.private.oldestMessageNode.message.timestamp) {
        newMessageNode.next = apiMessage.private.oldestMessageNode;
        apiMessage.private.oldestMessageNode.previous = newMessageNode;
        apiMessage.private.oldestMessageNode = newMessageNode;
      }
      else {
        let currMessageNode = apiMessage.private.latestMessageNode;
        index = apiMessage.private.messageCount - 1;
        while (newMessageNode.message.timestamp < currMessageNode.message.timestamp) {
          currMessageNode = currMessageNode.previous;
          index--;
        }
        newMessageNode.previous = currMessageNode.previous;
        newMessageNode.next = currMessageNode;
        newMessageNode.previous.next = newMessageNode;
        currMessageNode.previous = newMessageNode;
      }
      apiMessage.private.messageIds.set(newMessageNode.message.id, true);
      apiMessage.private.messageCount++;
      apiMessage.private.invokeNewMessageCallbacks(newMessageNode.message, index);
    },

    'requestMostRecentMessages': (limit) => {
      if (!apiMessage.private.socket || apiMessage.private.socket.readyState != 1) {
        apiMessage.private.invokeErrorDescriptionCallbacks('Unable to fetch messages');
        return false;
      }
      let messageToServer = {
        'fetchMessagesData': {
          'limit': limit
        }
      };
      apiMessage.private.socket.send(JSON.stringify(messageToServer));
      return true;
    }
  },

  'initialize': (batchSizeLimit) => {
    apiMessage.private.establishSocket(batchSizeLimit);
  },

  'registerErrorDescriptionCallback': (callback) => {
    apiMessage.private.errorDescriptionCallbacks.push(callback);
  },

  'registerNewMessageCallback': (callback) => {
    apiMessage.private.newMessageCallbacks.push(callback);
  },

  'getMessages': () => {
    let messageArray = [];
    let currMessageNode = apiMessage.private.oldestMessageNode;
    while (currMessageNode) {
      messageArray.push(currMessageNode.message);
      currMessageNode = currMessageNode.next;
    }
    return messageArray;
  },

  'requestPastMessages': (limit) => {
    if (!apiMessage.private.socket || apiMessage.private.socket.readyState != 1) {
      apiMessage.private.invokeErrorDescriptionCallbacks('Unable to fetch messages');
      return false;
    }
    let messageToServer = {
      'fetchMessagesData': {
        'messageId': apiMessage.private.oldestMessageNode ? apiMessage.private.oldestMessageNode.message.id : null,
        'limit': limit
      }
    };
    apiMessage.private.socket.send(JSON.stringify(messageToServer));
    return true;
  },

  'sendMessage': (messageContent) => {
    if (!apiMessage.private.socket || apiMessage.private.socket.readyState != 1) {
      apiMessage.private.invokeErrorDescriptionCallbacks('Unable to send message');
      return false;
    }
    let messageToServer = {
      'sendMessageData': {
        'content': messageContent
      }
    };
    apiMessage.private.socket.send(JSON.stringify(messageToServer));
    return true;
  }
};
