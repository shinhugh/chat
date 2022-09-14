var apiMessage = {
  'private': {
    'count': 0,
    'latestMessageNode': null,
    'oldestMessageNode': null,
    'newMessageCallbacks': []
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
      index = apiMessage.private.count - 1;
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
    for (const callback of apiMessage.private.newMessageCallbacks) {
      callback(index, message);
    }
  },

  'getOldestMessageId': () => {
    if (apiMessage.private.oldestMessageNode) {
      return apiMessage.private.oldestMessageNode.message.id;
    }
    return null;
  },

  'getOldestMessageTimestamp': () => {
    if (apiMessage.private.oldestMessageNode) {
      return apiMessage.private.oldestMessageNode.message.timestamp;
    }
    return null;
  },

  'getLatestMessageTimestamp': () => {
    if (apiMessage.private.latestMessageNode) {
      return apiMessage.private.latestMessageNode.message.timestamp;
    }
    return null;
  },

  'registerNewMessageCallback': (callback) => {
    apiMessage.private.newMessageCallbacks.push(callback);
  }
};
