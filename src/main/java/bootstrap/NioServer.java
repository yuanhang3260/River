package bootstrap;

import java.io.IOException;
import java.net.SocketAddress;

import bootstrap.NioBootStrap;
import channel.ChannelFuture;
import channel.ChannelInitializer;
import channel.ServerListenChannel;
import net.EventLoopGroup;

public class NioServer extends NioBootStrap {
  public EventLoopGroup getServerEventLoopGroup() {
    return serverGroup;
  }

  public ChannelFuture listen(SocketAddress address) throws IOException {
    this.channel = new ServerListenChannel(this, this.childInitializer);
    if (this.channelInitializer != null) {
      this.channelInitializer.initChannel(this.channel);
    }
    return this.channel.bind(address);
  }
}
