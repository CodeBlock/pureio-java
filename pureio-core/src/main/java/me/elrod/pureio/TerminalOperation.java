package me.elrod.pureio;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A sum type for all terminal-related operations one might perform.
 */
 public abstract class TerminalOperation<A> {
    private TerminalOperation() {
    }

    public abstract <B> B fold(
      BiFunction<String, A, B> putStrLn,
      Function<Function<String, A>, B> readLine,
      BiFunction<Integer, A, B> exit
    );

    final static class PutStrLn<A> extends TerminalOperation<A> {
      private String s;
      private A a;

      public PutStrLn(String s, A a) {
          this.s = s;
          this.a = a;
      }

      public <B> B fold(
        BiFunction<String, A, B> putStrLn,
        Function<Function<String, A>, B> readLine,
        BiFunction<Integer, A, B> exit
      ) {
        return putStrLn.apply(s, a);
      }
    }

    final static class ReadLine<A> extends TerminalOperation<A> {
      private Function<String, A> f;

      public ReadLine(Function<String, A> f) {
          this.f = f;
      }

      public <B> B fold(
        BiFunction<String, A, B> putStrLn,
        Function<Function<String, A>, B> readLine,
        BiFunction<Integer, A, B> exit
      ) {
        return readLine.apply(f);
      }
    }

    final static class Exit<A> extends TerminalOperation<A> {
        private Integer exitCode;
        private A a;

      public Exit(Integer exitCode, A a) {
          this.exitCode = exitCode;
          this.a = a;
      }

      public <B> B fold(
        BiFunction<String, A, B> putStrLn,
        Function<Function<String, A>, B> readLine,
        BiFunction<Integer, A, B> exit
      ) {
        return exit.apply(exitCode, a);
      }
    }

    // Functor
    public <B> TerminalOperation<B> map(Function<A, B> f) {
      return fold(
                  // new BiFunction<String, A, B>(s, a) { apply = new PutStrLn<B>(s, f.apply(a); }
        (s, a) -> new PutStrLn<B>(s, f.apply(a)),
        g      -> new ReadLine<B>(s -> f.apply(g.apply(s))),
        (e, a) -> new Exit<B>(e, f.apply(a)));
    }

    public PureIO<A> liftF() {
      return PureIO.free(map(x -> PureIO.pure(x)));
    }
}
