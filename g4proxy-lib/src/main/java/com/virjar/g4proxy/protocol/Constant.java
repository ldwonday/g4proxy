package com.virjar.g4proxy.protocol;

import java.util.Map;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class Constant {
    //单个报文，最大1M，超过1M需要分段
    public static final int MAX_FRAME_LENGTH = 1024 * 1024;

    public static final int LENGTH_FIELD_OFFSET = 0;

    public static final int LENGTH_FIELD_LENGTH = 4;

    public static final int INITIAL_BYTES_TO_STRIP = 0;

    public static final int LENGTH_ADJUSTMENT = 0;


    public static int READ_IDLE_TIME = 30;

    public static int WRITE_IDLE_TIME = 20;


    public static AttributeKey<Integer> USER_MAPPING_CHANNEL_PORT = AttributeKey.newInstance("user_mapping_channel_port");

    public static AttributeKey<Long> SERIAL_NUM = AttributeKey.newInstance("user_mapping_seq");

    public static AttributeKey<String> USER_MAPPING_CLIENT = AttributeKey.newInstance("user_mapping_client_id");

    public static AttributeKey<String> NAT_CHANNEL_CLIENT_KEY = AttributeKey.newInstance("nat_channel_client_id");

    public static AttributeKey<Channel> NEXT_CHANNEL = AttributeKey.newInstance("next_channel");

    public static AttributeKey<Map<Long, Channel>> userMappingChannelForNatChannel = AttributeKey.newInstance("userMappingChannelForNatChannel");

}
