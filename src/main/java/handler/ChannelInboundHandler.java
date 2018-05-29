package handler;

import handler.ChannelHandler;

public abstract class ChannelInboundHandler extends ChannelHandler {
  @Override
  public boolean isInbound() {
    return true;
  }
}
