# java-ssh-tunnel-service

# :construction: W.I.P.

 - Redis commands:
```shell script
PUBLISH SSH_CHANNEL connect|n # ex: PUBLISH SSH_CHANNEL connect|10
```
```shell script
PUBLISH SSH_CHANNEL disconnect|listened_port # ex: PUBLISH SSH_CHANNEL disconnect|1080
```
```shell script
PUBLISH SSH_CHANNEL current|~
```
```shell script
PUBLISH SSH_CHANNEL clear|~
```
```shell script
SUBSCRIBE SSH_CHANNEL_OK
# Receive: 
# 1) "message"
# 2) "SSH_CHANNEL_OK"
# 3) "57835"
```