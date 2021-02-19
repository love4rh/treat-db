package lib.turbok.hermes;

import static lib.turbok.hermes.HermesClientManager.CM;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;



public class HermesClientHandler extends SimpleChannelInboundHandler<WebSocketFrame>
{
    private String      _guid = null;
    
    private Channel     _channel = null;

    
    public String getRmoteAddress()
    {
        return _channel.remoteAddress().toString();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {   
        Logs.writeInfo("ACTIVE", ctx.channel().remoteAddress().toString());
        
        _channel = ctx.channel();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        Logs.writeInfo("INACTIVE", ctx.channel().remoteAddress().toString());
        
        ctx.fireChannelInactive();
        
        if( _guid != null )
        {
            CM.pop(_guid);
        }

        _channel = null;
        _guid = null;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception
    {
        // ping and pong frames already handled
        if( frame instanceof TextWebSocketFrame )
        {
            long sTick = System.currentTimeMillis();

            String msgText = ((TextWebSocketFrame) frame).text();
            JsonObject jsonObject = new JsonParser().parse(msgText).getAsJsonObject();
            String type = jsonObject.get("type").getAsString();
            StringBuilder sb = new StringBuilder();
            
            if( "hello".equals(type) )
            {
                _guid = CM.push(this);
                
                Logs.writeInfo("HELLO", ctx.channel(), _guid);

                sb.append("{\"type\":\"hello\"")
                  .append(",\"id\":\"").append(_guid).append("\"}");
            }
            else if( "text".equals(type) )
            {
                String value = jsonObject.get("value").getAsString();
                
                sb.append("{\"type\":\"text\"")
                  .append(",\"text\":\"").append(value.toUpperCase()).append("\"}");
            }
            else if( "send".equals(type) )
            {
                int rCode = 0;
                String rguid = jsonObject.get("receiver").getAsString();
                String message = jsonObject.get("data").getAsString();
                
                HermesClientHandler rHandler = CM.get(rguid);
                
                if( rHandler == null )
                {
                    rCode = 9;
                }
                else
                {
                    StringBuilder sb2 = new StringBuilder(); 
                    sb2.append("{\"type\":\"data\"")
                      .append(",\"value\":\"").append(message.replace("\"", "\\\"")).append("\"}");
                    
                    rHandler.send(sb2.toString());
                }
                
                sb.append("{\"type\":\"send\"")
                  .append(",\"code\":").append(rCode).append("}");
                
                Logs.writeInfo("SEND", ctx.channel(), rguid, rCode);
            }

            send( sb.toString() );
            
            Logs.writeInfo("PROCTIME", (System.currentTimeMillis() - sTick));
        }
        else
        {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
    
    public boolean send(String message)
    {
        if( _channel == null ) { return false; }
        
        _channel.writeAndFlush( new TextWebSocketFrame(message) );
        
        return true;
    }
}
