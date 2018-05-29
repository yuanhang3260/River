package handler;

import handler.ChannelOutboundHandler;

public class ChannelTailHandler extends ChannelOutboundHandler {
  public ChannelTailHandler() {
    setName("TailHandler");
  }
}