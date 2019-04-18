package com.virjar.g4proxy.server;

import com.virjar.g4proxy.protocol.Constant;
import com.virjar.g4proxy.protocol.NatMessage;
import com.virjar.g4proxy.server.client.ClientManager;
import com.virjar.g4proxy.server.client.NatClientImage;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class UserMappingChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private ClientManager clientManager;

    public UserMappingChannelHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        int port = sa.getPort();
        log.info("a new connection connect to port:{}", port);
        NatClientImage natClientImage = clientManager.getClient(port);
        if (natClientImage == null) {
            log.error("nat channel not ready! reject connect");
            ctx.close();
            return;
        }

        Long seq = natClientImage.onNewConnection(ctx.channel());
        log.info("create a new connect from user endpoint, with port:{} ,client:{} ,seq:{}", port, natClientImage.getClientId(), seq);
        userChannel.config().setOption(ChannelOption.AUTO_READ, false);

        NatMessage natMessage = new NatMessage();
        natMessage.setType(NatMessage.TYPE_CONNECT);
        natMessage.setSerialNumber(seq);
        natClientImage.getNatChannel().writeAndFlush(natMessage);
        super.channelActive(ctx);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel channel = ctx.channel();
        Long seq = channel.attr(Constant.SERIAL_NUM).get();
        String clientId = channel.attr(Constant.USER_MAPPING_CLIENT).get();

        if (clientId == null) {
            log.error("no client bound for this channel:{}", channel);
            ctx.close();
            return;
        }
        if (seq == null) {
            log.error("no seq generated for this channel:{}", channel);
            ctx.close();
            return;
        }


        NatClientImage client = clientManager.getClient(clientId);
        if (client == null) {
            log.error("now client bound for clientId:{}", clientId);
            ctx.close();
            return;
        }


        int maxPackSize = Constant.MAX_FRAME_LENGTH - 128;
        while (msg.readableBytes() > maxPackSize) {
            byte[] bytes = new byte[maxPackSize];
            msg.readBytes(bytes);

            NatMessage natMessage = new NatMessage();
            natMessage.setData(bytes);
            natMessage.setSerialNumber(seq);
            natMessage.setType(NatMessage.P_TYPE_TRANSFER);
            //write,但是不需要 flush
            client.getNatChannel().write(natMessage);
            log.info("receive data from user endpoint with big packet, forward to natChannel");
        }
        if (msg.readableBytes() > 0) {
            NatMessage natMessage = new NatMessage();
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            natMessage.setData(bytes);
            natMessage.setSerialNumber(seq);
            natMessage.setType(NatMessage.P_TYPE_TRANSFER);

            client.getNatChannel().writeAndFlush(natMessage);
            log.info("receive data from user endpoint, forward to natChannel");
        } else {
            client.getNatChannel().flush();
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // user mapping client disconnect initiative
        Channel channel = ctx.channel();
        log.info("user mapping client disconnect initiative");
        closeHttpProxyEndPoint(ctx);
        if (channel.isActive()) {
            channel.close();
        }
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        //TODO
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("error occur for user mapping :", cause);
        Channel channel = ctx.channel();
        closeHttpProxyEndPoint(ctx);
        if (channel.isActive()) {
            channel.close();
        }
    }


    private void closeHttpProxyEndPoint(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Long seq = channel.attr(Constant.SERIAL_NUM).get();
        String clientId = channel.attr(Constant.USER_MAPPING_CLIENT).get();

        if (clientId == null) {
            log.error("no client bound for this channel:{}", channel);
            return;
        }
        if (seq == null) {
            log.error("no seq generated for this channel:{}", channel);
            return;
        }


        NatClientImage client = clientManager.getClient(clientId);
        if (client == null) {
            log.error("now client bound for clientId:{} for request:", clientId, seq);
            return;
        }


        NatMessage natMessage = new NatMessage();
        natMessage.setType(NatMessage.TYPE_DISCONNECT);
        natMessage.setSerialNumber(seq);
        client.getNatChannel().writeAndFlush(natMessage);
        log.info("disconnect  for request:{} in the client:{}", seq, clientId);

        client.releaseConnection(channel);
    }
}
