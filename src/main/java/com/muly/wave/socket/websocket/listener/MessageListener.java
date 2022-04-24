package com.muly.wave.socket.websocket.listener;

import com.muly.wave.socket.api.SocketOperatorApi;
import com.muly.wave.socket.api.enums.SystemMessageTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Author: bozhou
 * @Date: 2021/10/9 14:09
 */
@Slf4j
@Component
@Order(99)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MessageListener implements CommandLineRunner {
    private final SocketOperatorApi socketOperatorApi;
    @Override
    public void run(String... args) throws Exception {
        // 项目启动默认注册了 监听首次连接的监听器 在客户端连接时会调用该监听器
        socketOperatorApi.msgTypeCallback(SystemMessageTypeEnum.SYS_LISTENER_ONOPEN.getCode(), (msgType, msg, socketSession)->{
            log.info("connection success");
            socketSession.getSocketOperatorApi().writeAndFlush("connection success");
        });
    }
}
