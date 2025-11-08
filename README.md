# Socket-Based Chat Server & Client

A simple **multi-user chat system** implemented in Java using **socket programming**.  
It supports real-time messaging between multiple clients connected to a single server.

---

## Features

✅ Multi-client support (multiple users can chat simultaneously)  
✅ Commands for login, private chat (DM), broadcast, and logout  
✅ Auto heartbeats (`PING`/`PONG`) to maintain connection stability  
✅ Server-side user list tracking  
✅ Automatic timeout and cleanup for inactive users  
✅ Works with any standard terminal — no special dependencies required  

---

## How It Works

- The **server** listens on a port (default: `4000`) for client connections.  
- Each new client connection is handled in a separate thread.  
- The **client** sends commands to the server (LOGIN, MSG, DM, WHO, LOGOUT).  
- The server broadcasts messages or sends direct messages accordingly.

---

## Commands Available

| Command | Description | Example |
|----------|--------------|----------|
| `LOGIN <username>` | Log into the server with a username | `LOGIN Pravalika` |
| `MSG <message>` | Send a broadcast message to all users | `MSG Hello everyone!` |
| `DM <username> <message>` | Send a private message to a specific user | `DM Ishaan Hey there!` |
| `WHO` | See the list of connected users | `WHO` |
| `PING` | Check server responsiveness | `PING` |
| `LOGOUT` | Disconnect safely from the chat | `LOGOUT` |

---


## 🖥️ How to Compile and Run

### 🛠️ Step 1: Compile both Java files
Open a terminal in the project directory and run:

javac Chatserver.java Chatclient.java

### Step 2: Start the Server

Run this command in Terminal 1:

java Chatserver 4000


Expected Output:

Chat Server running on port 4000
Waiting for clients...

### Step 3: Start Client 1

Open Terminal 2 and run:

java Chatclient localhost 4000


Expected Output:

Welcome to Chat Server
Available commands: LOGIN <username>, MSG <message>, DM <username> <message>, WHO, PING, LOGOUT
>>

### Step 4: Start Client 2

Open Terminal 3 and run:

java Chatclient localhost 4000

Now both clients can communicate in real time via the chat server.

### Demo Video :

![demo_video](./demo_socket_server(algokart_assignment).mp4)

### Author
Pravalika Batchu
