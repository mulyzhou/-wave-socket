package com.muly.wave.socket.websocket.operator;



import cn.hutool.core.util.ObjectUtil;
import com.muly.wave.socket.api.SocketOperatorApi;
import com.muly.wave.socket.api.exception.SocketException;
import com.muly.wave.socket.api.exception.enums.SocketExceptionEnum;
import com.muly.wave.socket.api.message.SocketMsgCallbackInterface;
import com.muly.wave.socket.api.session.pojo.SocketSession;
import com.muly.wave.socket.websocket.message.SocketMessageCenter;
import com.muly.wave.socket.websocket.operator.channel.WaveSocketOperator;
import com.muly.wave.socket.websocket.pojo.WebSocketMessageDTO;
import com.muly.wave.socket.websocket.session.SessionCenter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * WebSocket操作实现类
 * <p>
 * 如果是Spring boot项目，通过注入SocketOperatorApi接口操作socket，需将本来交给Spring管理
 *
 * @author wave-muly
 * @date 2021/6/2 上午10:41
 */
@Component
public class WebSocketOperator implements SocketOperatorApi {

    @Override
    public void sendMsgOfUserSessionBySessionId(String msgType, String sessionId, Object msg) throws SocketException {
        SocketSession<WaveSocketOperator> session = SessionCenter.getSessionBySessionId(sessionId);
        if (ObjectUtil.isEmpty(session)) {
            throw new SocketException(SocketExceptionEnum.SESSION_NOT_EXIST);
        }
        WebSocketMessageDTO webSocketMessageDTO = new WebSocketMessageDTO();
        webSocketMessageDTO.setData(msg);
        webSocketMessageDTO.setServerMsgType(msgType);
        session.getSocketOperatorApi().writeAndFlush(webSocketMessageDTO);
    }

    @Override
    public void sendMsgOfUserSession(String msgType, String userId, Object msg) throws SocketException {
        // 根据用户ID获取会话
        List<SocketSession<WaveSocketOperator>> socketSessionList = SessionCenter.getSessionByUserIdAndMsgType(userId);
        if (ObjectUtil.isEmpty(socketSessionList)) {
            throw new SocketException(SocketExceptionEnum.SESSION_NOT_EXIST);
        }
        WebSocketMessageDTO webSocketMessageDTO = new WebSocketMessageDTO();
        webSocketMessageDTO.setData(msg);
        webSocketMessageDTO.setServerMsgType(msgType);
        for (SocketSession<WaveSocketOperator> session : socketSessionList) {
            // 发送内容
            session.getSocketOperatorApi().writeAndFlush(webSocketMessageDTO);
        }
    }

    @Override
    public void sendMsgOfAllUserSession(String msgType, Object msg) {
        Collection<List<SocketSession<WaveSocketOperator>>> values = SessionCenter.getSocketSessionMap().values();
        WebSocketMessageDTO webSocketMessageDTO = new WebSocketMessageDTO();
        webSocketMessageDTO.setData(msg);
        webSocketMessageDTO.setServerMsgType(msgType);
        for (List<SocketSession<WaveSocketOperator>> sessions : values) {
            for (SocketSession<WaveSocketOperator> session : sessions) {
                // 找到该类型的通道
                if (session.getMessageType().equals(msgType)) {
                    session.getSocketOperatorApi().writeAndFlush(webSocketMessageDTO);
                }
            }
        }
    }

    @Override
    public void closeSocketBySocketId(String socketId) {
        SessionCenter.closed(socketId);
    }

    @Override
    public void msgTypeCallback(String msgType, SocketMsgCallbackInterface callbackInterface) {
        SocketMessageCenter.setMessageListener(msgType, callbackInterface);
    }
}
