// Required: /public/apiUtilities.js

// --------------------------------------------------

var apiMessage = {
  'count': 0,
  'latestMessageNode': null,
  'oldestMessageNode': null,
  'newMessageCallbacks': [],

  'getMessages': () => {
    let messageArray = [];
    let currMessageNode = apiMessage.oldestMessageNode;
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
    if (apiMessage.oldestMessageNode == null) {
      apiMessage.oldestMessageNode = newMessageNode;
      apiMessage.latestMessageNode = newMessageNode;
    }
    else if (newMessageNode.message.timestamp >= apiMessage.latestMessageNode.message.timestamp) {
      newMessageNode.previous = apiMessage.latestMessageNode;
      apiMessage.latestMessageNode.next = newMessageNode;
      apiMessage.latestMessageNode = newMessageNode;
      index = apiMessage.count - 1;
    }
    else if (newMessageNode.message.timestamp < apiMessage.oldestMessageNode.message.timestamp) {
      newMessageNode.next = apiMessage.oldestMessageNode;
      apiMessage.oldestMessageNode.previous = newMessageNode;
      apiMessage.oldestMessageNode = newMessageNode;
    }
    else {
      let currMessageNode = apiMessage.latestMessageNode;
      index = apiMessage.count - 1;
      while (newMessageNode.message.timestamp < currMessageNode.message.timestamp) {
        currMessageNode = currMessageNode.previous;
        index--;
      }
      newMessageNode.previous = currMessageNode.previous;
      newMessageNode.next = currMessageNode;
      newMessageNode.previous.next = newMessageNode;
      currMessageNode.previous = newMessageNode;
    }
    apiMessage.count++;
    for (const callback of apiMessage.newMessageCallbacks) {
      callback(index, message);
    }
  },

  'getOldestMessageId': () => {
    if (apiMessage.oldestMessageNode) {
      return apiMessage.oldestMessageNode.message.id;
    }
    return null;
  },

  'getOldestMessageTimestamp': () => {
    if (apiMessage.oldestMessageNode) {
      return apiMessage.oldestMessageNode.message.timestamp;
    }
    return null;
  },

  'getLatestMessageTimestamp': () => {
    if (apiMessage.latestMessageNode) {
      return apiMessage.latestMessageNode.message.timestamp;
    }
    return null;
  },

  'registerNewMessageCallback': (callback) => {
    apiMessage.newMessageCallbacks.push(callback);
  }
};
