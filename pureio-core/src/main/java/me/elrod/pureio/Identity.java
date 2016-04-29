package me.elrod.pureio;

import java.util.function.Function;

/**
 * The identity monad and comonad, used primarily for delaying evaluation.
 */
public abstract class Identity<T> {
    /**
     * Comonadic extraction. AKA "copure", "extract", or, colloquially, "Get the
     * darned value out."
     */
    public abstract T run();

    /**
     * Comonadic duplication. This does not force.
     */
    public Identity<Identity<T>> duplicate() {
        return new Identity<Identity<T>>() {
            public Identity<T> run() {
                return Identity.this;
            }
        };
    }

    /**
     * Comonadic extension.
     */
    public <U> Identity<U> extend(Function<Identity<T>, U> f) {
        return this.duplicate().map(f);
    }

    /**
     * Functor map.
     */
    public <U> Identity<U> map(Function<T, U> f) {
        return new Identity<U>() {
            public U run() {
                return f.apply(Identity.this.run());
            }
        };
    }

    public <U> Identity<U> ρ(Function<T, U> f) {
        return map(f);
    }

    /**
     * Applicative map.
     */
    public <U> Identity<U> applyVia(Identity<Function<T, U>> f) {
        return new Identity<U>() {
            public U run() {
                return f.run().apply(Identity.this.run());
            }
        };
    }

    /**
     * Monadic bind.
     */
    public <U> Identity<U> flatMap(Function<T, Identity<U>> f) {
        return f.apply(run());
    }

    public <U> Identity<U> $(Function<T, Identity<U>> f) {
        return flatMap(f);
    }
}
