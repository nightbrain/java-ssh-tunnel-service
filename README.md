# java-ssh-tunnel-service

Created: Nov 14, 2019 10:47 PM
Created By: dong nv
Last Edited Time: Nov 14, 2019 10:56 PM
Type: README

# 🚧 W.I.P.

- Redis commands:

    PUBLISH SSH_CHANNEL connect|n # ex: PUBLISH SSH_CHANNEL connect|10

    PUBLISH SSH_CHANNEL disconnect|listened_port # ex: PUBLISH SSH_CHANNEL disconnect|1080

    PUBLISH SSH_CHANNEL current|~

    PUBLISH SSH_CHANNEL clear|~

    SUBSCRIBE SSH_CHANNEL_OK
    # Receive: 
    # 1) "message" -> 
    # 2) "SSH_CHANNEL_OK"
    # 3) "57835"