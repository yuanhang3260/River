package channel;

import java.net.SocketAddress;

import buffer.ByteBuf;
import channel.ChannelPromise;

public interface IChannel {
  void bind(SocketAddress local, ChannelPromise promise);
  void connect(SocketAddress local, SocketAddress remote, ChannelPromise promise);
  void read(ByteBuf buf);
  void write(ByteBuf data, ChannelPromise promise);
  void flush(ChannelPromise promise);
  void close(ChannelPromise promise);
}
