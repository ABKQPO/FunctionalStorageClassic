package com.hfstudio.functionalstorage.api.storage;

/**
 * Idempotently closeable registration for storage change notifications.
 */
public interface StorageSubscription extends AutoCloseable {

    /**
     * Shared already-closed subscription used by handlers without an event source.
     */
    StorageSubscription CLOSED = new StorageSubscription() {

        @Override
        public void close() {}

        @Override
        public boolean isClosed() {
            return true;
        }
    };

    /**
     * Stops future notifications. Calling this method repeatedly has no effect.
     */
    @Override
    void close();

    /**
     * @return whether this subscription has been closed
     */
    boolean isClosed();
}
