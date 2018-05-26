package channel;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import buffer.ByteBuf;
import channel.ChannelHeaderHandler;
import channel.ChannelTailHandler;
import channel.IChannel;
import multithread.TaskExecutorGroup;

public abstract class BaseChannel implements IChannel {
  // SelectionKey that this channel is bound to.
  protected SelectionKey key;

  // Handler pipeline.
  protected ChannelHeaderHandler header;
  protected ChannelTailHandler tail;

  // EventLoop that this channel is bound to.
  //protected EventLoop eventLoop;

  // If specified, will be used to execute handler methods.
  protected TaskExecutorGroup executorGroup;
}
