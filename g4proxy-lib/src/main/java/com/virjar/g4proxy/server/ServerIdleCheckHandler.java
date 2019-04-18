package com.virjar.g4proxy.server;

import com.virjar.g4proxy.protocol.Constant;
import com.virjar.g4proxy.protocol.NatMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerIdleCheckHandler extends IdleStateHandler {
    public ServerIdleCheckHandler() {
        super(Constant.READ_IDLE_TIME + 10, Constant.WRITE_IDLE_TIME, 0);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT == evt) {
            log.info("read timeout,close channel");
            ctx.channel().close();
        } else if (IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT == evt) {
            //服务器还是需要手动写入心跳数据，否则通过代理下载大文件的时候，会堵塞通道（客户端下行带宽大于服务器下行带宽。将会导致大量数据包在服务器端累积，服务器接受数据出现延时）
            log.info("write timeout,send heartbeat message to nat client");

            NatMessage proxyMessage = new NatMessage();
            proxyMessage.setType(NatMessage.TYPE_HEARTBEAT);
            ctx.channel().writeAndFlush(proxyMessage);
        }
        super.channelIdle(ctx, evt);
    }
}
