package code.snippets.client;

public interface Callback<T> {
  public void invoke(T ret);
  public void onFail(Exception e);
}
