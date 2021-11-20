package com.jackzhong.learn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

import static io.netty.util.internal.StringUtil.NEWLINE;

@Slf4j
public class NettyByteBuf {
    public static void main(String[] args) {

        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();//容量是可以动态扩容的
        buffer.writeBytes(new byte[]{0x01, 0x02, 0x03, 0x04});
        log.debug("buffer class:{}",buffer.getClass().getSimpleName());
        logger(buffer);
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < 300; i++ ){
            sb.append(i+"");
        }
        buffer.writeBytes(sb.toString().getBytes());
        logger(buffer);

        CharSequence charSequence = buffer.readCharSequence(20, Charset.forName("utf-8"));
        log.debug("read:{}",charSequence);
        logger(buffer);
    }

    private static void logger(ByteBuf buffer){
        int length = buffer.readableBytes();
        int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
        StringBuilder buf = new StringBuilder(rows * 80 *2)
                .append("read index:").append(buffer.readerIndex())
                .append(" write index:").append(buffer.writerIndex())
                .append(" capacity:").append(buffer.capacity())
                .append(length)
                .append(NEWLINE);
        ByteBufUtil.appendPrettyHexDump(buf,buffer);
        log.debug("{}",buf.toString());
    }
}
