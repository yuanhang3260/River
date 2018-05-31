package codec;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import buffer.ByteBuf;
import channel.ChannelHandlerContext;
import handler.ChannelInboundHandler;

public abstract class ByteToMessageDecoder extends ChannelInboundHandler {
  private ByteBuf cumulation;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    List<Object> outs = new ArrayList<Object>();
    if (msg instanceof ByteBuf) {
      ByteBuf data = (ByteBuf)msg;
      if (cumulation == null) {
        cumulation = data;
      } else {
        cumulation.put(data);
      }

      try {
        decode(cumulation, outs);
        if (cumulation.readableBytes() == 0) {
          cumulation = null;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      outs.add(msg);
    }

    for (Object obj : outs) {
      ctx.fireChannelRead(obj);
    }
  }

  public abstract void decode(ByteBuf buf, List<Object> outs) throws Exception;
}
