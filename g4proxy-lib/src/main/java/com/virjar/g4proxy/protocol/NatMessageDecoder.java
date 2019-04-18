package com.virjar.g4proxy.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NatMessageDecoder extends LengthFieldBasedFrameDecoder {

    private static final byte HEADER_SIZE = 4;

    private static final int TYPE_SIZE = 1;

    private static final int SERIAL_NUMBER_SIZE = 8;

    private static final int URI_LENGTH_SIZE = 1;

    public NatMessageDecoder() {
        super(Constant.MAX_FRAME_LENGTH, Constant.LENGTH_FIELD_OFFSET, Constant.LENGTH_FIELD_LENGTH);
    }

    @Override
    protected NatMessage decode(ChannelHandlerContext ctx, ByteBuf in2) throws Exception {
        ByteBuf in = (ByteBuf) super.decode(ctx, in2);
        if (in == null) {
            return null;
        }

        if (in.readableBytes() < HEADER_SIZE) {
            return null;
        }

        int frameLength = in.readInt();
        if (in.readableBytes() < frameLength) {
            return null;
        }
        NatMessage natMessage = new NatMessage();
        byte type = in.readByte();
        long sn = in.readLong();

        natMessage.setSerialNumber(sn);

        natMessage.setType(type);

        byte uriLength = in.readByte();
        byte[] uriBytes = new byte[uriLength];
        in.readBytes(uriBytes);
        natMessage.setExtra(new String(uriBytes));

        byte[] data = new byte[frameLength - TYPE_SIZE - SERIAL_NUMBER_SIZE - URI_LENGTH_SIZE - uriLength];
        in.readBytes(data);
        natMessage.setData(data);

        in.release();
        return natMessage;
    }
}
