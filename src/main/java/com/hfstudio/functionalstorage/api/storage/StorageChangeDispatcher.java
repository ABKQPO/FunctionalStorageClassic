package com.hfstudio.functionalstorage.api.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

/**
 * Multi-listener storage event dispatcher. Events published from inside a
 * callback are queued until the current event finishes, so callback-driven
 * mutation cannot recurse through a partially dispatched listener list.
 *
 * @param <S> concrete snapshot type
 * @param <K> immutable resource key type
 */
public class StorageChangeDispatcher<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    private final List<Registration> registrations = new ArrayList<>();
    private final Queue<StorageChange<S, K>> pending = new ArrayDeque<>();
    private boolean dispatching;

    private static void rethrow(Throwable failure) {
        if (failure == null) {
            return;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        throw new IllegalStateException("storage change listener failed", failure);
    }

    /**
     * Registers a listener. A listener added mid-dispatch sees only later events.
     *
     * @param listener notified for each subsequent event
     * @return a subscription that removes the listener when closed
     */
    @Nonnull
    public synchronized StorageSubscription subscribe(@Nonnull Consumer<? super StorageChange<S, K>> listener) {
        Registration registration = new Registration(Objects.requireNonNull(listener, "listener"));
        registrations.add(registration);
        return registration;
    }

    /**
     * @return whether at least one active listener is registered
     */
    public synchronized boolean hasSubscribers() {
        return !registrations.isEmpty();
    }

    /**
     * Publishes an event to a stable listener snapshot. Closing a subscription
     * takes effect immediately, including before its turn in the current event.
     *
     * @param change event to publish
     */
    public synchronized void dispatch(@Nonnull StorageChange<S, K> change) {
        pending.add(Objects.requireNonNull(change, "change"));
        if (dispatching) {
            return;
        }
        dispatching = true;
        Throwable failure = null;
        try {
            StorageChange<S, K> next;
            while ((next = pending.poll()) != null) {
                List<Registration> snapshot = new ArrayList<>(registrations);
                for (Registration registration : snapshot) {
                    if (registration.closed) {
                        continue;
                    }
                    try {
                        registration.listener.accept(next);
                    } catch (Throwable thrown) {
                        if (failure == null) {
                            failure = thrown;
                        } else if (failure != thrown) {
                            failure.addSuppressed(thrown);
                        }
                    }
                }
            }
        } finally {
            dispatching = false;
        }
        if (failure != null) {
            rethrow(failure);
        }
    }

    private class Registration implements StorageSubscription {

        private final Consumer<? super StorageChange<S, K>> listener;
        private boolean closed;

        private Registration(Consumer<? super StorageChange<S, K>> listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            synchronized (StorageChangeDispatcher.this) {
                if (closed) {
                    return;
                }
                closed = true;
                registrations.remove(this);
            }
        }

        @Override
        public boolean isClosed() {
            synchronized (StorageChangeDispatcher.this) {
                return closed;
            }
        }
    }
}
