package handler;

import handler.ChannelHandler;

public abstract class ChannelOutboundHandler extends ChannelHandler {
  @Override
  public boolean isInbound() {
    return false;
  }
}
