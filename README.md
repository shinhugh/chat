# Chat

A global chat room, comprised of the server-side code and the web client
implementation.

![Demo](demo.gif)

Currently running on an AWS EC2 instance. The web client is available
**[here](http://ec2-13-57-232-164.us-west-1.compute.amazonaws.com)**.

Documentation is actively under progress!

---

## Technology

The back-end server is running a Java servlet container, exposing an HTTP API.
Authentication and user operations are done over HTTP. Chat functionality is
provided over WebSocket.

All data are stored in a SQL relational database.

The live app running on AWS EC2 is using Apache Tomcat and MySQL.

---

## Database design

All of the application's data are stored in relational databases.

### Users

| id | name | hash | salt |
| - | - | - | - |
| mLBczvL4...oz | Abe | Y4786o8V...qR | VGFAhwVI...Tv |
| 0p5srKt4...Os | Bob | S8MRb5fO...71 | MNt5IAIg...Ul |

```
CREATE TABLE users (
  id   VARCHAR(16) NOT NULL PRIMARY KEY,
  name VARCHAR(16) NOT NULL UNIQUE,
  hash VARCHAR(64) NOT NULL,
  salt VARCHAR(16) NOT NULL
);
```

### Sessions

| id | userId | expiration |
| - | - | - |
| KtF88V2i...5C | mLBczvL4...oz | 1663403598788 |
| EZQqtUFC...a3 | 0p5srKt4...Os | 1663410570130 |
| MqrjTXlL...m0 | mLBczvL4...oz | 1663412916625 |

```
CREATE TABLE sessions (
  id         VARCHAR(32)     NOT NULL PRIMARY KEY,
  userId     VARCHAR(16)     NOT NULL,
  expiration BIGINT UNSIGNED NOT NULL
);
```

### Messages

| id | userId | timestamp | content |
| - | - | - | - |
| SGZuQFjN...ay | 0p5srKt4...Os | 1663430617592 | Hello world! |
| 5ceRlTvh...rJ | mLBczvL4...oz | 1663430621183 | Hi Bob! |
| NspBi6jv...Bc | 0p5srKt4...Os | 1663430628539 | How are you? |
| n9GtGtmH...bE | mLBczvL4...oz | 1663430630714 | I am well. |

```
CREATE TABLE messages (
  id        VARCHAR(16)     NOT NULL PRIMARY KEY,
  userId    VARCHAR(16)     NOT NULL,
  timestamp BIGINT UNSIGNED NOT NULL,
  content   VARCHAR(256)    NOT NULL
);
```

---

## HTTP API

### `/api/login`

**POST**: Log in and generate a session token

TODO: Example

**DELETE**: Log out and invalidate a session token

TODO: Example

### `/api/user`

**GET**: Get the user's information, identified by the provided session token

TODO: Example

**POST**: Create a new user

TODO: Example

**PUT**: Update the user's information/credentials, identified by the provided session token

TODO: Example

**DELETE**: Delete the user, identified by the provided session token

TODO: Example

---

## WebSocket API

### `/api/messages`

The initial HTTP handshake request must contain a valid session token. This
enables the server to authenticate and identify the user. Failure to provide a
valid token will immediately terminate the connection. Once the socket is
established, the client no longer needs to send the session token in any form,
but the server verifies the cached session token before each time that an
incoming message is processed. This prevents sockets from outliving expired
session tokens.

Socket communication between the server and client is done through JSON format.

**Message to server**: Fetch the most recent messages, specifying the maximum
batch size

```
{
  'fetchMessagesData': {
    'limit': 15
  }
}
```

**Message to server**: Fetch the most recent messages with timestamps earlier
than that of the specified message, specifying the maximum batch size

```
{
  'fetchMessagesData': {
    'messageId': '4CowKnC463igp5TD',
    'limit': 15
  }
}
```

**Message to server**: Send a new message

```
{
  'sendMessageData': {
    'content': 'Hello world!'
  }
}
```

**Message to client**: Receive messages

```
{
  'messagesData': {
    'messages': [
      {
        'id': 'Rz5c8d8WeJJIs8pB',
        'outgoing': true,
        'timestamp': '2022-09-17T02:54:29.317Z',
        'content': 'Hello world!'
      },
      {
        'id': 'dN9ObuLt0S3Shrrz',
        'outgoing': false,
        'userName': 'Bob',
        'timestamp': '2022-09-17T02:54:35.872Z',
        'content': 'Hey there.'
      }
    ]
  }
}
```