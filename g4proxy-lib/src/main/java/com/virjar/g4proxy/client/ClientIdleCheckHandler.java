package com.virjar.g4proxy.client;

import com.virjar.g4proxy.protocol.Constant;
import com.virjar.g4proxy.protocol.NatMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientIdleCheckHandler extends IdleStateHandler {
    private G4ProxyClient g4ProxyClient;

    public ClientIdleCheckHandler(G4ProxyClient g4ProxyClient) {
        super(Constant.READ_IDLE_TIME, Constant.WRITE_IDLE_TIME, 0);
        this.g4ProxyClient = g4ProxyClient;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT == evt) {
            log.info("write idle, write a heartbeat message to server");
            NatMessage proxyMessage = new NatMessage();
            proxyMessage.setType(NatMessage.TYPE_HEARTBEAT);
            ctx.channel().writeAndFlush(proxyMessage);
        } else if (IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT == evt) {
            //不能在readTimeout的时候，就判定超时。比如在下载大文件的时候，只有数据写。没有写idle发生。也就没有heartbeat的ack。不会产生heartbeat的响应包
            log.info("first read  idle, write a heartbeat message to server");
            NatMessage proxyMessage = new NatMessage();
            proxyMessage.setType(NatMessage.TYPE_HEARTBEAT);
            ctx.channel().writeAndFlush(proxyMessage);
        } else if (IdleStateEvent.READER_IDLE_STATE_EVENT == evt) {
            log.info("read timeout,close channel");
            ctx.channel().close();
            log.info("the cmd channel lost,restart client");
            g4ProxyClient.forceReconnect();
        }

        super.channelIdle(ctx, evt);

    }
}
