package channel;

import channel.ChannelHandler;

public class ChannelTailHandler extends ChannelHandler {
  public boolean isInbound() {
    return false;
  }
  
}