package bootstrap;

import java.io.IOException;

import bootstrap.NioBootStrap;
import channel.ServerListenChannel;
import net.EventLoopGroup;

public class NioServer extends NioBootStrap {
  private EventLoopGroup serverGroup;

  public static class Option {
    public int clientEventLoopGroupSize;
  }

  public NioServer(NioServer.Option option) throws IOException {
    this.serverGroup = new EventLoopGroup(1);
    this.channel = new ServerListenChannel(this, serverGroup.next());
    this.eventLoopGroup = new EventLoopGroup(option.clientEventLoopGroupSize);
  }
}
