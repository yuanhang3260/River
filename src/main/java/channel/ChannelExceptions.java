package channel;

public class ChannelExceptions {
  public static class MethodNotSupported extends Exception {
    public MethodNotSupported() {
      super();
    }

    public MethodNotSupported(String msg) {
      super(msg);
    }
  }

  public static class ChannelUsedException extends Exception {
    public ChannelUsedException() {
      super();
    }

    public ChannelUsedException(String msg) {
      super(msg);
    }
  }

  public static class UnexpectedException extends Exception {
    public UnexpectedException(String msg) {
      super(msg);
    }
  }

  public static class ChannelNotInitialized extends Exception {
    public ChannelNotInitialized() {
      super();
    }
  }
}
