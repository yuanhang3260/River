package codec;

import buffer.ByteBuf;
import channel.ChannelHandlerContext;
import channel.ChannelPromise;
import handler.ChannelOutboundHandler;

public abstract class AbstractEncoder extends ChannelOutboundHandler {
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    try {
      ByteBuf encoded = ByteBuf.alloc();
      encode(msg, encoded);
      ctx.write(encoded, promise);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public abstract void encode(Object msg, ByteBuf buf) throws Exception;
}