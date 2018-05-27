package bootstrap;

import java.net.SocketAddress;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelFuture;
import net.EventLoopGroup;

public abstract class NioBootStrap {
  protected EventLoopGroup eventLoopGroup;
  protected BaseChannel channel;

  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  // User APIs, which actually delegate the call to channel APIs.
  public ChannelFuture bind(SocketAddress local) {
    return channel.bind(local);
  }

  public ChannelFuture connect(SocketAddress remote) {
    return channel.bind(remote);
  }

  public ChannelFuture write(ByteBuf buf) {
    return channel.write(buf);
  }

  public ChannelFuture flush() {
    return channel.flush();
  }

  public ChannelFuture close() {
    return channel.close();
  }
}
