package com.virjar.g4proxy.client;

import com.virjar.g4proxy.protocol.Constant;
import com.virjar.g4proxy.protocol.NatMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LittleProxyChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    protected G4ProxyClient g4ProxyClient;

    public LittleProxyChannelHandler(G4ProxyClient g4ProxyClient) {
        this.g4ProxyClient = g4ProxyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel channel = ctx.channel();
        Long seq = channel.attr(Constant.SERIAL_NUM).get();
        if (seq == null) {
            log.warn("now seq bound for channel:{}", channel);
            channel.close();
            return;
        }

        Channel natChannel = g4ProxyClient.getNatChannel();
        if (natChannel == null || !natChannel.isActive()) {
            log.error("nat connection lost!!");
            channel.close();
            return;
        }


        // 这里需要处理分包的问题，文件下载场景，这个包可能非常大。会导致 io.netty.handler.codec.TooLongFrameException: Adjusted frame length exceeds 1048576: 103335954 - discarded

        int maxPackSize = Constant.MAX_FRAME_LENGTH - 128;
        while (msg.readableBytes() > maxPackSize) {
            byte[] bytes = new byte[maxPackSize];
            msg.readBytes(bytes);

            NatMessage natMessage = new NatMessage();
            natMessage.setData(bytes);
            natMessage.setSerialNumber(seq);
            natMessage.setType(NatMessage.P_TYPE_TRANSFER);
            //write,但是不需要 flush
            natChannel.write(natMessage);
            log.info("receive data from littel proxy");
        }
        if (msg.readableBytes() > 0) {
            NatMessage natMessage = new NatMessage();
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            natMessage.setData(bytes);
            natMessage.setSerialNumber(seq);
            natMessage.setType(NatMessage.P_TYPE_TRANSFER);

            natChannel.writeAndFlush(natMessage);
            log.info("receive data from littel proxy");
        } else {
            natChannel.flush();
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();
        Long seq = channel.attr(Constant.SERIAL_NUM).get();
        if (seq == null) {
            log.warn("now seq bound for channel:{}", channel);
            channel.close();
            return;
        }

        Channel natChannel = g4ProxyClient.getNatChannel();
        if (natChannel == null || !natChannel.isActive()) {
            log.error("nat connection lost!!");
            channel.close();
            return;
        }
        NatMessage natMessage = new NatMessage();
        natMessage.setType(NatMessage.TYPE_DISCONNECT);
        natMessage.setSerialNumber(seq);
        natChannel.writeAndFlush(natChannel);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error occur ", cause);
        super.exceptionCaught(ctx, cause);
    }
}
