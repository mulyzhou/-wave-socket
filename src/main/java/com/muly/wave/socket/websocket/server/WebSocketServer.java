package com.muly.wave.socket.websocket.server;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.muly.wave.socket.api.enums.ClientMessageTypeEnum;
import com.muly.wave.socket.api.enums.ServerMessageTypeEnum;
import com.muly.wave.socket.api.enums.SystemMessageTypeEnum;
import com.muly.wave.socket.api.message.SocketMsgCallbackInterface;
import com.muly.wave.socket.api.session.pojo.SocketSession;
import com.muly.wave.socket.websocket.message.SocketMessageCenter;
import com.muly.wave.socket.websocket.operator.channel.WaveSocketOperator;
import com.muly.wave.socket.websocket.pojo.WebSocketMessageDTO;
import com.muly.wave.socket.websocket.session.SessionCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * 消息监听处理器
 *
 * @author wave-muly
 * @date 2021/6/1 下午2:35
 */
@Slf4j
@ServerEndpoint(value = "/webSocket/{token}")
@Component
public class WebSocketServer {

    /**
     * 连接建立调用的方法
     * <p>
     * 暂时无用，需要在建立连接的时候做一些事情的话可以修改这里
     *
     * @param session 会话信息
     * @author wave-muly
     * @date 2021/6/21 下午5:14
     **/
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        String userId = token;
//        try {
//            // 解析用户信息
//            DefaultJwtPayload defaultPayload = JwtContext.me().getDefaultPayload(token);
//            userId = defaultPayload.getUserId().toString();
//        } catch (io.jsonwebtoken.JwtException e) {
//            try {
//                session.close();
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
//            }
//        }

        // 操作api包装
        WaveSocketOperator gunsSocketOperator = new WaveSocketOperator(session);

        // 回复消息
        WebSocketMessageDTO replyMsg = new WebSocketMessageDTO();
        replyMsg.setServerMsgType(ServerMessageTypeEnum.SYS_REPLY_MSG_TYPE.getCode());
        replyMsg.setToUserId(userId);

        // 创建会话对象
        SocketSession<WaveSocketOperator> socketSession = new SocketSession<>();
        try {
            // 设置回复内容
            replyMsg.setData(session.getId());
            socketSession.setSessionId(session.getId());
            socketSession.setUserId(userId);
            socketSession.setSocketOperatorApi(gunsSocketOperator);
            socketSession.setToken(token);
            socketSession.setConnectionTime(System.currentTimeMillis());

            // 维护会话
            SessionCenter.addSocketSession(socketSession);
        } finally {
            // 回复消息
            gunsSocketOperator.writeAndFlush(replyMsg);

            // 触发首次连接回调
            SocketMsgCallbackInterface socketMsgCallbackInterface = SocketMessageCenter.getSocketMsgCallbackInterface(SystemMessageTypeEnum.SYS_LISTENER_ONOPEN.getCode());
            if (ObjectUtil.isNotEmpty(socketMsgCallbackInterface)) {
                // 触发回调
                socketMsgCallbackInterface.callback(SystemMessageTypeEnum.SYS_LISTENER_ONOPEN.getCode(), null, socketSession);
            }
        }

    }

    /**
     * 连接关闭调用的方法
     *
     * @param session 会话信息
     * @author wave-muly
     * @date 2021/6/21 下午5:14
     **/
    @OnClose
    public void onClose(Session session) {
        try {
            SocketSession<WaveSocketOperator> socketSession = SessionCenter.getSessionBySessionId(session.getId());
            // 触发首次连接回调
            SocketMsgCallbackInterface socketMsgCallbackInterface = SocketMessageCenter.getSocketMsgCallbackInterface(SystemMessageTypeEnum.SYS_LISTENER_ONCLOSE.getCode());
            if (ObjectUtil.isNotEmpty(socketMsgCallbackInterface)) {
                // 触发回调
                socketMsgCallbackInterface.callback(SystemMessageTypeEnum.SYS_LISTENER_ONCLOSE.getCode(), null, socketSession);
            }
        } finally {
            SessionCenter.closed(session.getId());
        }
    }

    /**
     * 收到消息调用的方法
     *
     * @param message       　接收到的消息
     * @param socketChannel 会话信息
     * @author wave-muly
     * @date 2021/6/21 下午5:14
     **/
    @OnMessage
    public void onMessage(String message, Session socketChannel) {

        // 转换为Java对象
        WebSocketMessageDTO webSocketMessageDTO = JSON.parseObject(message, WebSocketMessageDTO.class);

        // 维护通道是否已初始化
        SocketSession<WaveSocketOperator> socketSession = SessionCenter.getSessionBySessionId(socketChannel.getId());

        // 心跳包
        if (ObjectUtil.isNotEmpty(socketSession) && ClientMessageTypeEnum.USER_HEART.getCode().equals(webSocketMessageDTO.getClientMsgType())) {
            // 更新会话最后活跃时间
            if (ObjectUtil.isNotEmpty(socketSession)) {
                socketSession.setLastActiveTime(System.currentTimeMillis());
            }
            return;
        }

        // 用户ID为空不处理直接跳过
        if (ObjectUtil.isEmpty(webSocketMessageDTO.getFormUserId())) {
            return;
        }

        // 会话建立成功执行业务逻辑
        if (ObjectUtil.isNotEmpty(socketSession)) {

            // 更新最后会话时间
            socketSession.setLastActiveTime(System.currentTimeMillis());

            // 找到该消息的处理器
            SocketMsgCallbackInterface socketMsgCallbackInterface = SocketMessageCenter.getSocketMsgCallbackInterface(webSocketMessageDTO.getClientMsgType());
            if (ObjectUtil.isNotEmpty(socketMsgCallbackInterface)) {
                // 触发回调
                socketMsgCallbackInterface.callback(webSocketMessageDTO.getClientMsgType(), webSocketMessageDTO, socketSession);
            } else {
                socketChannel.getAsyncRemote().sendText("{\"serverMsgType\":\"404\"}");
            }
        }
    }

    /**
     * 会话发送异常调用的方法
     *
     * @param session 会话信息
     * @param error   　错误信息
     * @author wave-muly
     * @date 2021/6/21 下午5:14
     **/
    @OnError
    public void onError(Session session, Throwable error) {
        SocketSession<WaveSocketOperator> socketSession = SessionCenter.getSessionBySessionId(session.getId());
        // 触发首次连接回调
        SocketMsgCallbackInterface socketMsgCallbackInterface = SocketMessageCenter.getSocketMsgCallbackInterface(SystemMessageTypeEnum.SYS_LISTENER_ONERROR.getCode());
        if (ObjectUtil.isNotEmpty(socketMsgCallbackInterface)) {
            // 触发回调
            socketMsgCallbackInterface.callback(SystemMessageTypeEnum.SYS_LISTENER_ONERROR.getCode(), error, socketSession);
        }
        log.error("session 发生错误:" + session.getId());
    }
}
