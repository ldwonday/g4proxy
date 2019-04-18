package com.virjar.g4proxy.server.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.virjar.g4proxy.protocol.Constant;
import com.virjar.g4proxy.server.AvailablePortResManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientManager {
    private Map<String, NatClientImage> natClientImageMap = new ConcurrentHashMap<>();
    @Getter
    private BiMap<String, Integer> clientPortBiMap = HashBiMap.create();
    private Map<Integer, Channel> userMappingServerChannels = new ConcurrentHashMap<>();
    @Setter
    private ServerBootstrap userMappingBootstrap;

    public ClientManager() {
        // this.userMappingBootstrap = userMappingBootstrap;
    }

    public boolean registerNewClient(String client, Channel natChannel) {
        NatClientImage natClientImage = natClientImageMap.get(client);
        Integer mappingPort = null;
        if (natClientImage != null) {
            log.warn("update nat channel");
            natClientImage.getNatChannel().close();
            natClientImage.closeAllUserChannel();
            mappingPort = natClientImage.getMappingPort();

            //需要关闭所有的连接，但是计数器不能清零。计数器清零可能导致紊乱
            natClientImage.updateChannel(natChannel);

            Channel userMappingServerChannel = userMappingServerChannels.get(mappingPort);
            log.info("userMappingServerChannel:{}", userMappingServerChannel);
            if (userMappingServerChannel == null || !userMappingServerChannel.isOpen() || !userMappingServerChannel.isActive()) {
                log.info("re open mapping port: {}", mappingPort);
                ChannelFuture channelFuture = userMappingBootstrap.bind(mappingPort);
                try {
                    channelFuture.get();
                } catch (Exception e) {
                    log.error("wait for port binding error", e);
                    return false;
                }
                if (!channelFuture.isSuccess()) {
                    log.warn("bind mapping port:{} failed", mappingPort, channelFuture.cause());
                    return false;
                }
                userMappingServerChannels.put(mappingPort, channelFuture.channel());
                natClientImage.setUserMappingServerChannel(channelFuture.channel());
            }

        } else {

            // retry with useful port
            for (int i = 0; i < 5; i++) {
                mappingPort = AvailablePortResManager.allocate(client);
                if (mappingPort == null) {
                    log.error("failed to allocate port");
                    return false;
                }
                ChannelFuture channelFuture = userMappingBootstrap.bind(mappingPort);
                try {
                    channelFuture.get();
                } catch (Exception e) {
                    log.error("wait for port binding error", e);
                    continue;
                }
                if (!channelFuture.isSuccess()) {
                    log.warn("bind mapping port:{} failed", mappingPort, channelFuture.cause());
                    continue;
                }
                userMappingServerChannels.put(mappingPort, channelFuture.channel());
                natClientImage = new NatClientImage(client, mappingPort, natChannel, channelFuture.channel());
                break;
            }

            if (natClientImage == null) {
                log.error("open user mapping port failed finally");
                return false;
            }
        }


        natChannel.attr(Constant.NAT_CHANNEL_CLIENT_KEY).set(client);
        clientPortBiMap.put(client, mappingPort);
        natClientImageMap.put(client, natClientImage);
        log.info("client :{} register success,with port:{}", client, mappingPort);
        return true;
    }

    public NatClientImage getClient(String clientId) {
        return natClientImageMap.get(clientId);
    }

    public NatClientImage getClient(Integer mappingPort) {
        String clientId = clientPortBiMap.inverse().get(mappingPort);
        if (clientId == null) {
            return null;
        }
        return natClientImageMap.get(clientId);
    }


//    public void closeClient(String clientId) {
//        log.info("close client :{}", clientId);
//        NatClientImage client = natClientImageMap.remove(clientId);
//        if (client == null) {
//            log.error("no client registered for clientId:{}", clientId);
//            return;
//        }
//        client.closeAllUserChannel();
//        client.getNatChannel().close();
//        //这样，停止监听对应的代理端口，防止代理请求再次打进来
//        client.getUserMappingServerChannel().close();
//    }
}
