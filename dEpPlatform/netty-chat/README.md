# netty-chat

### Protocol:

| PacketLen  | Header  | Actual Content |
| :----: |:-------:| :-------------:|
| 4byte  | 12byte   |   data  |

###  Packet:
- PacketLen 包长度
- HeaderLen 头长度
- Version   版本号
- Operation 业务操作号
- SeqId     包递增号
- Body      业务数据

### Architecture:

<img src="https://raw.githubusercontent.com/im-qq/netty-chat/master/docs/architecture.png" width="500">
<img src="https://raw.githubusercontent.com/im-qq/netty-chat/master/docs/proto.png" width="800">

### Operation:

    AuthOperation -> decode<AuthToken> -> AuthService
    MessageOperation -> decode<Message> -> MessageService

### MessageQueue

    MQMessage
    MQProducer
    MQConsumer
    MQMessageListener

### Test

    Tcp: comet -> test -> ChatClient
    WebSocket: comet -> test -> websocket -> demo.html
    
### License
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.