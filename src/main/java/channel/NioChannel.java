package channel;

import handler.ChannelHandler;

public interface NioChannel {
  void addHandler(ChannelHandler handler);
}
