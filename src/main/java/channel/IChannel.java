package channel;

import java.net.SocketAddress;

import buffer.ByteBuf;
import channel.ChannelFuture;

public interface IChannel {
  ChannelFuture bind(SocketAddress local);
  ChannelFuture connect(SocketAddress remote);
  ChannelFuture write(ByteBuf buf);
  ChannelFuture flush();
  ChannelFuture close();
}
