// Required: /public/apiUtilities.js

// --------------------------------------------------

var apiMessage = {
  'latestMessageNode': null,
  'oldestMessageNode': null,

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
    if (!message.outgoing && !message.userName) {
      return;
    }
    let newMessageNode = {
      'message': message,
      'previous': null,
      'next': null
    };
    if (apiMessage.oldestMessageNode == null) {
      apiMessage.oldestMessageNode = newMessageNode;
      apiMessage.latestMessageNode = newMessageNode;
      return;
    }
    if (newMessageNode.message.timestamp >= apiMessage.latestMessageNode.message.timestamp) {
      newMessageNode.previous = apiMessage.latestMessageNode;
      apiMessage.latestMessageNode.next = newMessageNode;
      apiMessage.latestMessageNode = newMessageNode;
      return;
    }
    if (newMessageNode.message.timestamp < apiMessage.oldestMessageNode.message.timestamp) {
      newMessageNode.next = apiMessage.oldestMessageNode;
      apiMessage.oldestMessageNode.previous = newMessageNode;
      apiMessage.oldestMessageNode = newMessageNode;
      return;
    }
    let currMessageNode = apiMessage.oldestMessageNode;
    while (newMessageNode.message.timestamp >= currMessageNode.message.timestamp) {
      currMessageNode = currMessageNode.next;
    }
    newMessageNode.previous = currMessageNode.previous;
    newMessageNode.next = currMessageNode;
    newMessageNode.previous.next = newMessageNode;
    currMessageNode.previous = newMessageNode;
  },

  'getOldestMessageId': () => {
    if (apiMessage.oldestMessageNode) {
      return apiMessage.oldestMessageNode.message.id;
    }
    return null;
  }
};
