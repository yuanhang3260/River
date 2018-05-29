package bootstrap;

import java.io.IOException;
import java.net.SocketAddress;

import bootstrap.NioBootStrap;
import buffer.ByteBuf;
import channel.ChannelFuture;
import channel.ChannelExceptions;
import channel.ClientChannel;
import channel.DefaultChannelFuture;
import net.EventLoopGroup;

public class NioClient extends NioBootStrap {
  public ChannelFuture connect(SocketAddress remote) throws IOException {
    this.channel = new ClientChannel(this);
    if (this.channelInitializer != null) {
      this.channelInitializer.initChannel(this.channel);
    }
    return this.channel.connect(remote);
  }
}
