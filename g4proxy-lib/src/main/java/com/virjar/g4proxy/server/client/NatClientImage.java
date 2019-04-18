package com.virjar.g4proxy.server.client;

import com.google.common.collect.Maps;
import com.virjar.g4proxy.protocol.Constant;

import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NatClientImage {
    @Getter
    private String clientId;
    private AtomicLong seqGenerator = new AtomicLong(0);
    @Getter
    private int mappingPort;
    @Getter
    private Channel natChannel;

    @Getter
    @Setter
    private Channel userMappingServerChannel;

    // private Map<Long, Channel> userMappingChannels = Maps.newConcurrentMap();

    public NatClientImage(String clientId, int mappingPort, Channel natChannel, Channel userMappingServerChannel) {
        this.clientId = clientId;
        this.mappingPort = mappingPort;
        this.natChannel = natChannel;
        this.userMappingServerChannel = userMappingServerChannel;
        this.natChannel.attr(Constant.userMappingChannelForNatChannel).set(Maps.<Long, Channel>newConcurrentMap());
    }

    public void updateChannel(Channel channel) {
        log.info("update channel for client: {}", clientId);
        this.natChannel = channel;
        this.natChannel.attr(Constant.userMappingChannelForNatChannel).set(Maps.<Long, Channel>newConcurrentMap());
    }

    public Channel queryUserMappingChannel(Long seq) {
        return natChannel.attr(Constant.userMappingChannelForNatChannel).get().get(seq);
    }

    public Long onNewConnection(Channel userMappingChannel) {
        long seq = seqGenerator.incrementAndGet();
        natChannel.attr(Constant.userMappingChannelForNatChannel).get().put(seq, userMappingChannel);

        userMappingChannel.attr(Constant.SERIAL_NUM).set(seq);
        userMappingChannel.attr(Constant.USER_MAPPING_CHANNEL_PORT).set(mappingPort);
        userMappingChannel.attr(Constant.USER_MAPPING_CLIENT).set(clientId);
        return seq;
    }

    public void releaseConnection(Channel channel) {
        Long seq = channel.attr(Constant.SERIAL_NUM).get();
        if (seq == null) {
            log.warn("no seq bound for user mapping channel: {}", channel);
            return;
        }
        channel.attr(Constant.SERIAL_NUM).set(null);
        channel.attr(Constant.USER_MAPPING_CHANNEL_PORT).set(null);
        channel.attr(Constant.USER_MAPPING_CLIENT).set(null);

        natChannel.attr(Constant.userMappingChannelForNatChannel).get().remove(seq);

        if (channel.isActive()) {
            channel.close();
        }
    }

    public void closeAllUserChannel() {
        for (Channel channel : natChannel.attr(Constant.userMappingChannelForNatChannel).get().values()) {
            releaseConnection(channel);
        }

    }
}
