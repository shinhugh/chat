const apiMessage = {
  'private': {
    'errorDescriptionCallbacks': [],
    'newMessagesCallbacks': [],
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

    'invokeNewMessagesCallbacks': (newMessages) => {
      for (const callback of apiMessage.private.newMessagesCallbacks) {
        callback(newMessages);
      }
    },

    'establishSocket': (batchSizeLimit) => {
      if ('WebSocket' in window) {
        apiMessage.private.socket = new WebSocket(apiMessage.private.socketUrl);

        apiMessage.private.socket.onmessage = (messageToClientWrapper) => {
          const messageToClient = JSON.parse(messageToClientWrapper.data);
          if (messageToClient.messagesData && messageToClient.messagesData.messages) {
            for (const message of messageToClient.messagesData.messages) {
              message.timestamp = new Date(message.timestamp);
            }
            apiMessage.private.addMessages(messageToClient.messagesData.messages);
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

    'addMessages': (messages) => {
      messages.sort((messageA, messageB) => {
        if (messageA.timestamp < messageB.timestamp) {
          return -1;
        } else if (messageA.timestamp > messageB.timestamp) {
          return 1;
        } else {
          return 0;
        }
      });
      const newMessages = [];
      let lastAddedNode = null;
      for (const message of messages) {
        const newMessageNode = {
          'message': message,
          'previous': null,
          'next': null
        };
        if (apiMessage.private.messageIds.has(newMessageNode.message.id)) {
          continue;
        }
        if (lastAddedNode) {
          let currMessageNode = lastAddedNode.next;
          while (currMessageNode && newMessageNode.message.timestamp >= currMessageNode.message.timestamp) {
            currMessageNode = currMessageNode.next;
          }
          if (!currMessageNode) {
            newMessageNode.previous = apiMessage.private.latestMessageNode;
            newMessageNode.previous.next = newMessageNode;
            apiMessage.private.latestMessageNode = newMessageNode;
          } else {
            newMessageNode.previous = currMessageNode.previous;
            newMessageNode.next = currMessageNode;
            newMessageNode.previous.next = newMessageNode;
            currMessageNode.previous = newMessageNode;
          }
        }
        else {
          if (apiMessage.private.oldestMessageNode == null) {
            apiMessage.private.oldestMessageNode = newMessageNode;
            apiMessage.private.latestMessageNode = newMessageNode;
          }
          else if (newMessageNode.message.timestamp >= apiMessage.private.latestMessageNode.message.timestamp) {
            newMessageNode.previous = apiMessage.private.latestMessageNode;
            apiMessage.private.latestMessageNode.next = newMessageNode;
            apiMessage.private.latestMessageNode = newMessageNode;
          }
          else if (newMessageNode.message.timestamp < apiMessage.private.oldestMessageNode.message.timestamp) {
            newMessageNode.next = apiMessage.private.oldestMessageNode;
            apiMessage.private.oldestMessageNode.previous = newMessageNode;
            apiMessage.private.oldestMessageNode = newMessageNode;
          }
          else {
            let currMessageNode = apiMessage.private.latestMessageNode;
            while (newMessageNode.message.timestamp < currMessageNode.message.timestamp) {
              currMessageNode = currMessageNode.previous;
            }
            newMessageNode.previous = currMessageNode;
            newMessageNode.next = currMessageNode.next;
            currMessageNode.next = newMessageNode;
            newMessageNode.next.previous = newMessageNode;
          }
        }
        apiMessage.private.messageIds.set(newMessageNode.message.id, true);
        apiMessage.private.messageCount++;
        newMessages.push(newMessageNode.message);
        lastAddedNode = newMessageNode;
      }
      apiMessage.private.invokeNewMessagesCallbacks(newMessages);
    },

    'requestMostRecentMessages': (limit) => {
      if (!apiMessage.private.socket || apiMessage.private.socket.readyState != 1) {
        apiMessage.private.invokeErrorDescriptionCallbacks('Unable to fetch messages');
        return false;
      }
      const messageToServer = {
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

  'registerNewMessagesCallback': (callback) => {
    apiMessage.private.newMessagesCallbacks.push(callback);
  },

  'getMessages': () => {
    const messageArray = [];
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
    const messageToServer = {
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
    const messageToServer = {
      'sendMessageData': {
        'content': messageContent
      }
    };
    apiMessage.private.socket.send(JSON.stringify(messageToServer));
    return true;
  }
};
