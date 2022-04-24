package com.muly.wave.socket.message.controller;

import com.muly.wave.socket.api.SocketOperatorApi;
import com.muly.wave.socket.api.enums.ServerMessageTypeEnum;
import com.muly.wave.socket.api.exception.SocketException;
import com.muly.wave.socket.websocket.pojo.SysMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;


/**
 * @Author: wave-muly
 * @Date: 2021/10/9 11:28
 */
@RequestMapping("/api/v1/sys")
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysMessageController {

    private final SocketOperatorApi socketOperatorApi;

    @PostMapping(value = "/notice/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void notice(@PathVariable(name = "userId") String userId, @RequestBody String messageContent) {
        SysMessage item = new SysMessage();
        item.setReceiveUserId(Long.valueOf(userId));
        item.setMessageContent(messageContent);
        item.setMessageType(ServerMessageTypeEnum.SYS_NOTICE_MSG_TYPE.getCode());
        item.setMessageSendTime(new Date());
        try {
            socketOperatorApi.sendMsgOfUserSession(ServerMessageTypeEnum.SYS_NOTICE_MSG_TYPE.getCode(), item.getReceiveUserId().toString(), item);
        } catch (SocketException socketException) {
            // 该用户不在线
        }
    }

}
