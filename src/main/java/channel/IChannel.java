package channel;

import java.net.SocketAddress;

import buffer.ByteBuf;
import channel.ChannelFuture;

public interface IChannel {
  void bind(SocketAddress local, ChannelFuture future);
  void connect(SocketAddress local, SocketAddress remote, ChannelFuture future);
  void read(ByteBuf buf);
  void write(ByteBuf data, ChannelFuture future);
  void flush(ChannelFuture future);
  void close(ChannelFuture future);
}
