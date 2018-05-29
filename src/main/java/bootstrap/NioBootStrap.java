package bootstrap;

import java.net.SocketAddress;

import buffer.ByteBuf;
import channel.BaseChannel;
import channel.ChannelFuture;
import channel.ChannelInitializer;
import net.EventLoopGroup;

public abstract class NioBootStrap {
  protected EventLoopGroup eventLoopGroup;
  protected EventLoopGroup serverGroup;
  protected BaseChannel channel;

  protected ChannelInitializer channelInitializer;
  protected ChannelInitializer childInitializer;

  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  public NioBootStrap group(EventLoopGroup boss, EventLoopGroup worker) {
    this.serverGroup = boss;
    this.eventLoopGroup = worker;
    return this;
  }

  public NioBootStrap group(EventLoopGroup worker) {
    this.eventLoopGroup = worker;
    return this;
  }

  public NioBootStrap handler(ChannelInitializer initializer) {
    this.channelInitializer = initializer;
    return this;
  }

  public NioBootStrap childHandler(ChannelInitializer childInitializer) {
    this.childInitializer = childInitializer;
    return this;
  }

  public BaseChannel channel() {
    return this.channel;
  }
}
