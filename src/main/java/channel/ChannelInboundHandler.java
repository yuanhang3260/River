package channel;

import channel.ChannelHandler;

public abstract class ChannelInboundHandler extends ChannelHandler {
  @Override
  public boolean isInbound() {
    return true;
  }
}
