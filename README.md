# Chat

A global chat room, comprised of the server-side code and the web client implementation.

The back-end is currently running on an AWS EC2 instance. The web client is available **[here](http://ec2-13-57-232-164.us-west-1.compute.amazonaws.com)**.

*None of the communication is currently encrypted; users should avoid using important credentials.*

![Demo](demo.gif)

---

## Technology

The back-end server is running a Java servlet container, exposing an HTTP API. Authentication and user operations are done over HTTP. Chat functionality is provided over WebSocket.

All data are stored in a SQL relational database.

The server has dependencies on the following external Java libraries:

- Corresponding JDBC driver for the RDBMS used
- Gson

Of course, there are dependencies on the libraries provided by the servlet container as well (e.g. `HTTPServlet`).

The server currently deployed on AWS is using Apache Tomcat and MySQL.

---

## HTTP API

### `/api/login`

**POST**: Log in and generate a session token.

Request:

```
curl -v -X POST http://13.57.232.164/api/login -d "{\"name\":\"Abe\",\"pw\":\"abe\"}"
```

```
POST /api/login
Content-Type: application/json
Body:
{
  "name": "Abe",
  "pw": "abe"
}
```

Response:

```
200
Set-Cookie: session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs; Path=/; SameSite=Strict; Max-Age=86399; Expires=Sun, 18 Sep 2022 18:26:33 GMT
```

**DELETE**: Log out and invalidate a session token.

Request:

```
curl -v -X DELETE http://13.57.232.164/api/login -b "session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs"
```

```
DELETE /api/login
Cookie: session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs
```

Response:

```
200
Set-Cookie: session=; Path=/; SameSite=Strict; Max-Age=0; Expires=Thu, 01 Jan 1970 12:00:00 GMT
```

### `/api/user`

**GET**: Get the user's information, identified by the provided session token.

Request:

```
curl -v -X GET http://13.57.232.164/api/user -b "session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs"
```

```
GET /api/user
Cookie: session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs
```

Response:

```
200
Content-Type: application/json
Body:
{
  "id": "x6sAjV8WDXsBDD1d",
  "name": "Abe"
}
```

**POST**: Create a new user.

Request:

```
curl -v -X POST http://13.57.232.164/api/user -d "{\"name\":\"Abe\",\"pw\":\"abe\"}"
```

```
POST /api/user
Content-Type: application/json
Body:
{
  "name": "Abe",
  "pw": "abe"
}
```

Response:

```
200
```

**PUT**: Update the user's information/credentials, identified by the provided session token.

Request:

```
curl -v -X PUT http://13.57.232.164/api/user -b "session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs" -d "{\"pw\":\"1234\"}"
```

```
PUT /api/user
Cookie: session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs
Content-Type: application/json
Body:
{
  "pw": "1234"
}
```

Response:

```
200
```

**DELETE**: Delete the user, identified by the provided session token.

Request:

```
curl -v -X DELETE http://13.57.232.164/api/user -b "session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs"
```

```
DELETE /api/user
Cookie: session=UEDfVQYxlIfxMk6bgWWdLbJcxbaczIQs
```

Response:

```
200
```

---

## WebSocket API

### `/api/messages`

The initial WebSocket handshake request must contain a valid session token. This enables the server to authenticate and identify the user. Failure to provide a valid token will immediately terminate the connection. Once the socket is established, the client no longer needs to send the session token in any form. The server verifies the cached session token before each time that an incoming message is processed. This prevents sockets from outliving expired session tokens.

Socket communication between the server and client is done through JSON format.

**Message to server**: Fetch the most recent messages, specifying the maximum batch size.

```
{
  'fetchMessagesData': {
    'limit': 15
  }
}
```

**Message to server**: Fetch the most recent messages with timestamps earlier than that of the specified message, specifying the maximum batch size.

```
{
  'fetchMessagesData': {
    'messageId': '4CowKnC463igp5TD',
    'limit': 15
  }
}
```

**Message to server**: Send a new message.

```
{
  'sendMessageData': {
    'content': 'Hello world!'
  }
}
```

**Message to client**: Receive messages.

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

---

## Database design

All of the application's data are stored in relational databases.

### Users

To generate the hash, the user's password is concatenated with a randomly generated salt then passed into the SHA-256 algorithm. The hash and salt are stored within the user's entry so that the server can validate login attempts in the future.

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

The expiration value is given as the number of milliseconds since the Unix epoch. Each time that a session token is verified, its expiration is also checked. Optionally, the server can also run an automated task between certain time intervals where it purges all expired entries (e.g. via a `cron` job).

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

The timestamp value is given as the number of milliseconds since the Unix epoch.

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