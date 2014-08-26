package me.elrod.pureio;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A trampoline is a sum type with two basic constructors: {@link Pure} and
 * {@link Suspend}. A {@link Pure} is a leaf value produced at the end of a
 * tree of computations; A {@link Suspend} holds a computation that can be
 * resumed.
 *
 * We also introduce the notion of a {@link Codensity} which allows us to bake
 * a monad directly into the trampoline structure.
 * c.f. Bjarnason, Rúnar Oli. "Stackless Scala With Free Monads." (2012).
 *
 * In Java (as opposed to Scala), it is necessary to create {@link Codensity}
 * separately than the other two ({@link Suspend}, {@link Pure}) constructors.
 * The reason for this is that otherwise we cannot construct a catamorphism for
 * the "monadic constructor" (what we end up calling {@link Codensity} that will
 * typecheck.
 *
 * It is worth noting that we might be able to achieve some of this
 * functionality by making use of Java 8 streams, but the ease with which this
 * code could be backported to Java 7 makes me reluctant to do it differently.
 *
 * Very little code here is specific to Java 8 -- that is, one could replace the
 * lambda syntax with an explicit anonymous {@link Function} implementation
 * (where a custom <code>interface Function<A, B> { B apply(A x); }</code>
 * exists) and this code would likely run on Java 7 with no issue whatsoever.
 *
 * We use Java 8 only for demonstration purposes and to lose a bit of the
 * boilerplate.
 *
 * Internally, we use {@link Either} to construct the tree of
 * computations.
 */
public abstract class Trampoline<A> {
    private Trampoline() {}

    /**
     * Perform one step of the computation and return the next step.
     */
    public abstract Either<Identity<Trampoline<A>>, A> resume();

    public abstract <R> R cata(
        final Function<Normal<A>, R> normal,
        final Function<Codensity<A>, R> codensity);

    public abstract <B> Trampoline<B> flatMap(final Function<A, Trampoline<B>> fn);

    private static abstract class Normal<A> extends Trampoline<A> {
        public abstract <R> R normalCata(
            final Function<A, R> pure,
            final Function<Identity<Trampoline<A>>, R> suspend);

        public <B> Trampoline<B> flatMap(final Function<A, Trampoline<B>> fn) {
            return codensity(this, fn);
        }
    }

    private static final class Suspend<A> extends Normal<A> {
        private final Identity<Trampoline<A>> suspension;
        private Suspend(final Identity<Trampoline<A>> s) {
            this.suspension = s;
        }

        public <R> R cata(
            final Function<Normal<A>, R> normal,
            final Function<Codensity<A>, R> codensity) {
            return normal.apply(this);
        }

        public <R> R normalCata(
            final Function<A, R> pure,
            final Function<Identity<Trampoline<A>>, R> suspend) {
            return suspend.apply(this.suspension);
        }
        public Either<Identity<Trampoline<A>>, A> resume() {
            return Either.left(this.suspension);
        }
    }

    private static final class Pure<A> extends Normal<A> {
        private final A value;
        private Pure(final A x) {
            this.value = x;
        }

        public <R> R cata(
            final Function<Normal<A>, R> normal,
            final Function<Codensity<A>, R> codensity) {
            return normal.apply(this);
        }

        public <R> R normalCata(
            final Function<A, R> pure,
            final Function<Identity<Trampoline<A>>, R> suspend) {
            return pure.apply(this.value);
        }

        public Either<Identity<Trampoline<A>>, A> resume() {
            return Either.right(this.value);
        }
    }

    /**
     * Unfortunately, limits of Java's type system force us to use Object
     * extensively here. However, this is private and it will be correct by
     * construction before it is ever used.
     */
    private static final class Codensity<A> extends Trampoline<A> {
        private final Normal<Object> sub;
        private final Function<Object, Trampoline<A>> k;

        public <R> R cata(
            final Function<Normal<A>, R> normal,
            final Function<Codensity<A>, R> codensity) {
            return codensity.apply(this);
        }

        private Codensity(Normal<Object> sub, Function<Object, Trampoline<A>> k) {
            this.sub = sub;
            this.k = k;
        }

        public <B> Trampoline<B> flatMap(final Function<A, Trampoline<B>> fn) {
            return codensity(
                sub,
                o -> suspend(new Identity<Trampoline<B>>() {
                        public Trampoline<B> run() {
                            return k.apply(o).flatMap(fn);
                        }
                    }));
        }

        public Either<Identity<Trampoline<A>>, A> resume() {
            return Either.left(
                sub.resume().cata(
                    // Left
                    p -> p.map(
                        ot -> ot.cata(
                            o -> o.normalCata(
                                obj -> k.apply(obj),
                                t -> t.run().flatMap(k)),

                            // Due to a regression from java 1.8.0_11 to 1.8.0_20 (and 1.8.0_40), these types
                            // are *required* to be written out fully.
                            new Function<Codensity<Object>, Trampoline<A>>() {
                                public Trampoline<A> apply(Codensity<Object> c) {
                                    return codensity(
                                        c.sub,
                                        o -> c.k.apply(o).flatMap(k));
                                }
                            })),
                    // Right
                    o -> new Identity<Trampoline<A>>() {
                        public Trampoline<A> run() {
                            return k.apply(o);
                        }
                    }));
        }
    }

    @SuppressWarnings("unchecked")
    protected static <A, B> Codensity<B> codensity(
        final Normal<A> a,
        final Function<A, Trampoline<B>> k) {
        return new Codensity<B>((Normal<Object>) a, (Function<Object, Trampoline<B>>) k);
    }

    public static <A> Trampoline<A> pure(final A x) {
        return new Pure<A>(x);
    }

    public static <A> Trampoline<A> suspend(final Identity<Trampoline<A>> x) {
        return new Suspend<A>(x);
    }

    // This is taken almost directly from FJ for now.
    // Credit:
    // https://github.com/functionaljava/functionaljava/blob/master/core/src/main/java/fj/control/Trampoline.java
    public A run() {
        Trampoline<A> current = this;
        while (true) {
            final Either<Identity<Trampoline<A>>, A> x = current.resume();
            if (x.isLeft()) {
                Either.LeftP<Identity<Trampoline<A>>, A> y = x.projectLeft();
                current = y.unsafeValue().run();
            } else {
                Either.RightP<Identity<Trampoline<A>>, A> y = x.projectRight();
                return y.unsafeValue();
            }
        }
    }
}
