package codec;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import buffer.ByteBuf;
import channel.ChannelHandlerContext;
import codec.ByteToMessageDecoder;
import handler.ChannelInboundHandler;

public abstract class ReplayingDecoder extends ByteToMessageDecoder {
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
        cumulation.markReadIndex();
        decode(cumulation, outs);
        if (cumulation.readableBytes() == 0) {
          cumulation = null;
        }
      } catch (BufferUnderflowException e) {
        // In case of BufferUnderflowException thrown from decode(), recover the read position of
        // cumulation buffer and discard all decoded objects.
        cumulation.resetReadIndex();
        return;
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
}
