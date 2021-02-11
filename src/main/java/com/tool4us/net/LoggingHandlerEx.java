package com.tool4us.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.*;
import io.netty.util.internal.StringUtil;
import java.net.SocketAddress;

import com.tool4us.common.Logs;



/**
 * 네크워킹 레벨의 상세 로깅을 위한 클래스.
 * Netty에서 제공하는 LoggingHandler2 참고
 * 
 * @author TurboK
 */
public class LoggingHandlerEx extends ChannelDuplexHandler
{
    private static final String NEWLINE;
    private static final String BYTE2HEX[];
    private static final String HEXPADDING[];
    private static final String BYTEPADDING[];
    private static final char BYTE2CHAR[];
    
    static
    {
        NEWLINE = StringUtil.NEWLINE;
        BYTE2HEX = new String[256];
        HEXPADDING = new String[16];
        BYTEPADDING = new String[16];
        BYTE2CHAR = new char[256];
        for(int i = 0; i < BYTE2HEX.length; i++)
            BYTE2HEX[i] = (new StringBuilder()).append(' ').append(StringUtil.byteToHexStringPadded(i)).toString();
        
        for(int i = 0; i < HEXPADDING.length; i++)
        {
            int padding = HEXPADDING.length - i;
            StringBuilder buf = new StringBuilder(padding * 3);
            for(int j = 0; j < padding; j++)
                buf.append("   ");
            
            HEXPADDING[i] = buf.toString();
        }
        
        for(int i = 0; i < BYTEPADDING.length; i++)
        {
            int padding = BYTEPADDING.length - i;
            StringBuilder buf = new StringBuilder(padding);
            for(int j = 0; j < padding; j++)
                buf.append(' ');
            
            BYTEPADDING[i] = buf.toString();
        }
        
        for(int i = 0; i < BYTE2CHAR.length; i++)
            if( i <= 31 || i >= 127 )
                BYTE2CHAR[i] = '.';
            else
                BYTE2CHAR[i] = (char) i;
    }
    
    private int             _level = 0;
    private boolean         _packetDisplay = false;
    
    
    /**
     * @param logLevel DEBUG = 1, NORMAL = 2, WARNING = 3, ERROR = 4, FATAL = 5
     * @param packetDisplay Packet 표시 여부 (로그량이 많이 증가함)
     */
    public LoggingHandlerEx(int logLevel, boolean packetDisplay)
    {
        _level = logLevel;
        _packetDisplay = packetDisplay;
    }

    protected String format(ChannelHandlerContext ctx, String message)
    {
        String chStr = ctx.channel().toString();
        return (new StringBuilder(chStr.length() + message.length() + 1)).append(chStr).append(' ').append(message)
                .toString();
    }
    
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception
    {
        Logs.raw(_level, format(ctx, "REGISTERED"));
        super.channelRegistered(ctx);
    }
    
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
    {
        Logs.raw(_level, format(ctx, "UNREGISTERED"));
        super.channelUnregistered(ctx);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        Logs.raw(_level, format(ctx, "ACTIVE"));
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        Logs.raw(_level, format(ctx, "INACTIVE"));
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        Logs.raw(_level, format(ctx, (new StringBuilder()).append("EXCEPTION: ").append(cause).toString()), cause);
        super.exceptionCaught(ctx, cause);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        Logs.raw(_level, format(ctx, (new StringBuilder()).append("USER_EVENT: ").append(evt).toString()));
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception
    {
        Logs.raw(_level, format(ctx, (new StringBuilder()).append("BIND(").append(localAddress).append(')').toString()));
        super.bind(ctx, localAddress, promise);
    }
    
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception
    {
        Logs.raw(_level,
            format(ctx, (new StringBuilder()).append("CONNECT(").append(remoteAddress).append(", ").append(localAddress).append(')').toString())
        );
        super.connect(ctx, remoteAddress, localAddress, promise);
    }
    
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
        Logs.raw(_level, format(ctx, "DISCONNECT()"));
        super.disconnect(ctx, promise);
    }
    
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
        Logs.raw(_level, format(ctx, "CLOSE()"));
        super.close(ctx, promise);
    }
    
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
        Logs.raw(_level, format(ctx, "DEREGISTER()"));
        super.deregister(ctx, promise);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        logMessage(ctx, "RECEIVED", msg);
        ctx.fireChannelRead(msg);
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        logMessage(ctx, "WRITE", msg);
        ctx.write(msg, promise);
    }
    
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception
    {
        Logs.raw(_level, format(ctx, "FLUSH"));
        ctx.flush();
    }
    
    private void logMessage(ChannelHandlerContext ctx, String eventName, Object msg)
    {
        Logs.raw(_level, format(ctx, formatMessage(eventName, msg)));
    }
    
    protected String formatMessage(String eventName, Object msg)
    {
        if( msg instanceof ByteBuf )
            return formatByteBuf(eventName, (ByteBuf) msg);
        if( msg instanceof ByteBufHolder )
            return formatByteBufHolder(eventName, (ByteBufHolder) msg);
        else
            return formatNonByteBuf(eventName, msg);
    }
    
    protected String formatByteBuf(String eventName, ByteBuf buf)
    {
        int length = buf.readableBytes();
        int rows = length / 16 + (length % 15 != 0 ? 1 : 0) + 4;
        StringBuilder dump = null;
        
        if( _packetDisplay ) 
            dump = new StringBuilder(rows * 80 + eventName.length() + 16);
        else
            dump = new StringBuilder(64);
        
        dump.append(eventName)
            .append('(')
            .append(length)
            .append('B')
            .append(')');
        
        if( !_packetDisplay )
            return dump.toString();
        
        dump.append((new StringBuilder()).append(NEWLINE)
                .append("         +-------------------------------------------------+").append(NEWLINE)
                .append("         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |").append(NEWLINE)
                .append("+--------+-------------------------------------------------+----------------+")
                .toString());
        
        int startIndex = buf.readerIndex();
        int endIndex = buf.writerIndex();
        int i;
        for(i = startIndex; i < endIndex; i++)
        {
            int relIdx = i - startIndex;
            int relIdxMod16 = relIdx & 15;
            if( relIdxMod16 == 0 )
            {
                dump.append(NEWLINE).append(Long.toHexString((long) relIdx & 4294967295L | 4294967296L))
                        .setCharAt(dump.length() - 9, '|');
                dump.append('|');
            }
            dump.append(BYTE2HEX[buf.getUnsignedByte(i)]);
            if( relIdxMod16 != 15 )
                continue;
            dump.append(" |");
            for(int j = i - 15; j <= i; j++)
                dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
            
            dump.append('|');
        }
        
        if( (i - startIndex & 15) != 0 )
        {
            int remainder = length & 15;
            dump.append(HEXPADDING[remainder]).append(" |");
            for(int j = i - remainder; j < i; j++)
                dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
            
            dump.append(BYTEPADDING[remainder]).append('|');
        }
        dump.append((new StringBuilder()).append(NEWLINE)
                .append("+--------+-------------------------------------------------+----------------+").toString());
        
        return dump.toString();
    }
    
    protected String formatNonByteBuf(String eventName, Object msg)
    {
        return (new StringBuilder()).append(eventName).append(": ").append(msg).toString();
    }
    
    protected String formatByteBufHolder(String eventName, ByteBufHolder msg)
    {
        return formatByteBuf(eventName, msg.content());
    }
}
