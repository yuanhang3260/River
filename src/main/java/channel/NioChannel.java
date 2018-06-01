package channel;

import handler.ChannelHandler;

public interface NioChannel {
  void addInboundHandler(ChannelHandler handler);
  void addOutboundHandler(ChannelHandler handler);
}
