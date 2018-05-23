package multithread;

public interface IPromise<V> {
  boolean setSuccess(V result);
  boolean setFailure(Throwable cause);
}
