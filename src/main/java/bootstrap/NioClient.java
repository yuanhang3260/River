package bootstrap;

import bootstrap.NioBootStrap;
import channel.ClientChannel;
import net.EventLoopGroup;

public class NioClient extends NioBootStrap {
  public static class Option {
    public int clientEventLoopGroupSize;
  }

  public NioClient(NioClient.Option option) {
    this.channel = new ClientChannel(this);
    this.eventLoopGroup = new EventLoopGroup(1);
  }
}
