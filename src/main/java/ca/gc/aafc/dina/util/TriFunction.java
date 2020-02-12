package ca.gc.aafc.dina.util;

/**
 * A function that accepts three input arguments and returns a result.
 * 
 * @param <A> first argument type
 * @param <B> second argument type
 * @param <C> third argument type
 * @param <R> return type
 */
public interface TriFunction<A, B, C, R> {
  R apply(A a, B b, C c);
}
