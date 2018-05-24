package channel;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import buffer.ByteBuf;
import channel.AbstractChannel;
import channel.ChannelHeaderHandler;
import channel.ChannelTailHandler;

public class ClientChannel extends AbstractChannel {
  // Lower-level network IO.
  private SocketChannel javaChannel;
  private ByteBuf inboundBuf;
  private ByteBuf outboundBuf;

  // ------------------------------ Lower level IO functions ------------------------------------ //
  @Override
  public void bind(SocketAddress local, ChannelPromise promise) {
    
  }

  @Override
  public void connect(SocketAddress local, SocketAddress remote, ChannelPromise promise) {

  }

  @Override
  public void read(ByteBuf buf) {

  }

  @Override
  public void write(ByteBuf data, ChannelPromise promise) {

  }

  @Override
  public void flush(ChannelPromise promise) {

  }

  @Override
  public void close(ChannelPromise promise) {

  }
}
