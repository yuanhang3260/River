package channel;

import channel.ChannelHandler;

public abstract class ChannelOutboundHandler extends ChannelHandler {
  @Override
  public boolean isInbound() {
    return false;
  }
}
