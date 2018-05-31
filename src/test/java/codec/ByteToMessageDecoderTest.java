package codec;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

import buffer.ByteBuf;
import codec.ByteToMessageDecoder;
import channel.ChannelHandlerContext;
import handler.ChannelInboundHandler;

public class ByteToMessageDecoderTest {
  private class Message {
    public int vInt;        // 4
    public double vDouble;  // 8
    public long vLong;      // 8
    public char vChar;      // 2
    public float vFloat;    // 4
  }

  private class MessageDecoder extends ByteToMessageDecoder {
    @Override
    public void decode(ByteBuf buf, List<Object> outs) throws Exception {
      while (buf.readableBytes() >= 26) {
        Message msg = new Message();
        msg.vInt = buf.getInt();  
        msg.vDouble = buf.getDouble();
        msg.vLong = buf.getLong();
        msg.vChar = buf.getChar();
        msg.vFloat = buf.getFloat();
        outs.add(msg);
      }
    }
  }

  private List<Object> receiver = new ArrayList<Object>();

  private class MockChannelHandlerContext extends ChannelHandlerContext {
    public MockChannelHandlerContext() {
      super(null, null);
    }

    @Override
    public ChannelHandlerContext fireChannelRead(Object msg) {
      // Verify the Message object.
      receiver.add(msg);
      return this;
    }
  }

  @Test
  public void testDecodeMessage() {
    ChannelHandlerContext ctx = new MockChannelHandlerContext();
    MessageDecoder decoder = new MessageDecoder();

    ByteBuf buf = ByteBuf.alloc();
    buf.putInt(1);
    buf.putDouble(2.0);
    buf.putLong(3);

    decoder.channelRead(ctx, buf);
    assertTrue(receiver.isEmpty());

    buf = ByteBuf.alloc();
    buf.putChar('R');
    buf.putFloat((float)4.0);
    decoder.channelRead(ctx, buf);
    assertEquals(1, receiver.size());

    Message msg = (Message)receiver.get(0);
    assertEquals(1, msg.vInt);
    assertEquals(2.0, msg.vDouble, 0.0001);
    assertEquals(3, msg.vLong);
    assertEquals('R', msg.vChar);
    assertEquals(4.0, msg.vFloat, 0.0001);
  }

  @Test
  public void testDecodeMultipleMessages() {
    ChannelHandlerContext ctx = new MockChannelHandlerContext();
    MessageDecoder decoder = new MessageDecoder();

    ByteBuf buf = ByteBuf.alloc();
    for (int i = 0; i < 100; i++) {
      buf.putInt(1);
      buf.putDouble(2.0);
      buf.putLong(3);
      buf.putChar('R');
      buf.putFloat((float)4.0);
    }
    buf.putInt(1);
    buf.putDouble(2.0);
    buf.putLong(3);

    receiver.clear();
    decoder.channelRead(ctx, buf);
    assertEquals(100, receiver.size());

    for (int i = 0; i < 100; i++) {
      Message msg = (Message)receiver.get(i);
      assertEquals(1, msg.vInt);
      assertEquals(2.0, msg.vDouble, 0.0001);
      assertEquals(3, msg.vLong);
      assertEquals('R', msg.vChar);
      assertEquals(4.0, msg.vFloat, 0.0001);
    }

    buf = ByteBuf.alloc();
    buf.putChar('R');
    buf.putFloat((float)4.0);
    decoder.channelRead(ctx, buf);
    assertEquals(101, receiver.size());
  }
}
