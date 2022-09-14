var apiMessage = {
  'private': {
    'socketUrl': (window.location.protocol == 'http:' ? 'ws://' : 'wss://') + window.location.host + '/api/messages',
    'batchSize': 5,
    'socket': null,
    'count': 0,
    'latestMessageNode': null,
    'oldestMessageNode': null,
    'errorDescriptionCallbacks': [],
    'newMessageCallbacks': [],

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

    'addMessage': (message) => {
      let newMessageNode = {
        'message': message,
        'previous': null,
        'next': null
      };
      let index = 0;
      if (apiMessage.private.oldestMessageNode == null) {
        apiMessage.private.oldestMessageNode = newMessageNode;
        apiMessage.private.latestMessageNode = newMessageNode;
      }
      else if (newMessageNode.message.timestamp >= apiMessage.private.latestMessageNode.message.timestamp) {
        newMessageNode.previous = apiMessage.private.latestMessageNode;
        apiMessage.private.latestMessageNode.next = newMessageNode;
        apiMessage.private.latestMessageNode = newMessageNode;
        index = apiMessage.private.count;
      }
      else if (newMessageNode.message.timestamp < apiMessage.private.oldestMessageNode.message.timestamp) {
        newMessageNode.next = apiMessage.private.oldestMessageNode;
        apiMessage.private.oldestMessageNode.previous = newMessageNode;
        apiMessage.private.oldestMessageNode = newMessageNode;
      }
      else {
        let currMessageNode = apiMessage.private.latestMessageNode;
        index = apiMessage.private.count - 1;
        while (newMessageNode.message.timestamp < currMessageNode.message.timestamp) {
          currMessageNode = currMessageNode.previous;
          index--;
        }
        newMessageNode.previous = currMessageNode.previous;
        newMessageNode.next = currMessageNode;
        newMessageNode.previous.next = newMessageNode;
        currMessageNode.previous = newMessageNode;
      }
      apiMessage.private.count++;
      apiMessage.private.invokeNewMessageCallbacks(newMessageNode.message, index);
    },

    'requestMostRecentMessages': () => {
      if (!apiMessage.private.socket || apiMessage.private.socket.readyState != 1) {
        apiMessage.private.invokeErrorDescriptionCallbacks('Unable to fetch messages');
        return false;
      }
      let messageToServer = {
        'fetchMessagesData': {
          'limit': apiMessage.private.batchSize
        }
      };
      apiMessage.private.socket.send(JSON.stringify(messageToServer));
      return true;
    }
  },

  'initialize': () => {
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
        apiMessage.private.requestMostRecentMessages();
      };

      apiMessage.private.socket.onclose = () => {
        apiMessage.private.invokeErrorDescriptionCallbacks('Unable to connect');
      };
    }

    else {
      apiMessage.private.invokeErrorDescriptionCallbacks('Unable to connect');
    }
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

  'requestPastMessages': () => {
    if (!apiMessage.private.socket || apiMessage.private.socket.readyState != 1) {
      apiMessage.private.invokeErrorDescriptionCallbacks('Unable to fetch messages');
      return false;
    }
    let messageToServer = {
      'fetchMessagesData': {
        'messageId': apiMessage.private.oldestMessageNode ? apiMessage.private.oldestMessageNode.message.id : null,
        'limit': apiMessage.private.batchSize
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
