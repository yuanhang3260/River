package handler;

import handler.ChannelInboundHandler;

public class ChannelHeaderHandler extends ChannelInboundHandler {
  public ChannelHeaderHandler() {
    setName("HeaderHandler");
  }
}
