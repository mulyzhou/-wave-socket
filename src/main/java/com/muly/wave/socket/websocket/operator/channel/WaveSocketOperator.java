package com.muly.wave.socket.websocket.operator.channel;


import com.alibaba.fastjson.JSON;

import javax.websocket.Session;
import java.io.IOException;

/**
 * Socket操作类实现
 * <p>
 * 简单封装Spring Boot的默认WebSocket
 *
 * @author wave-muly
 * @date 2021/6/1 下午3:41
 */
public class WaveSocketOperator implements SocketChannelExpandInterFace {

    /**
     * 实际操作的通道
     */
    private Session socketChannel;

    public WaveSocketOperator(Session socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void writeAndFlush(Object obj) {
        try {
            if (socketChannel.isOpen()) {
                socketChannel.getBasicRemote().sendText(JSON.toJSONString(obj));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToChannel(Object obj) {
        if (socketChannel.isOpen()) {
            socketChannel.getAsyncRemote().sendText(JSON.toJSONString(obj));
        }
    }

    @Override
    public void close() {
        try {
            if (socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isInvalid() {
        return socketChannel.isOpen();
    }
}
