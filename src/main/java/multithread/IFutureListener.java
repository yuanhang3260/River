package multithread;

import multithread.IFuture;
import java.util.concurrent.CancellationException;

public interface IFutureListener<V> {
  // Callback to execute when future is done.
  void taskDone(IFuture<V> future) throws Exception;
}
