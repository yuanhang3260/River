package channel;

import channel.ChannelHandler;

public class ChannelHeaderHandler extends ChannelHandler {
  // Header handler is both inbound and outbound handler.
  public boolean isInbound() {
    return true;
  }

  public boolean isOutbound() {
    return true;
  }

  
}