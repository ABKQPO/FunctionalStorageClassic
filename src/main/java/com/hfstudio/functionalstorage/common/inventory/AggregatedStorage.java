package com.hfstudio.functionalstorage.common.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageChangeDispatcher;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

public class AggregatedStorage<S extends StorageSnapshot<S, K>, K extends StorageKey> implements IStorageHandler<S, K> {

    private final S empty;
    private final List<StorageSubscription> subscriptions = new ArrayList<>();
    private final StorageChangeDispatcher<S, K> changes = new StorageChangeDispatcher<>();
    private List<IStorageHandler<S, K>> children = List.of();
    private int[] ends = new int[0];
    private int count;
    private final List<IStorageHandler<S, K>> scratch = new ArrayList<>();
    private final Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());

    protected AggregatedStorage(S empty) {
        this.empty = empty;
    }

    public boolean rebuild(List<? extends IStorageHandler<S, K>> candidates) {
        List<IStorageHandler<S, K>> unique = scratch;
        unique.clear();
        identities.clear();
        for (int candidate = 0; candidate < candidates.size(); candidate++) {
            IStorageHandler<S, K> child = candidates.get(candidate);
            if (child != null && child != this && identities.add(child.getStorageIdentity())) unique.add(child);
        }
        boolean changed = !unique.equals(children);
        if (!changed) {
            int total = 0;
            for (int index = 0; index < unique.size(); index++) {
                total += unique.get(index)
                    .getStorageCount();
                if (total != ends[index]) changed = true;
            }
        }
        identities.clear();
        if (!changed) {
            unique.clear();
            return false;
        }
        for (StorageSubscription subscription : subscriptions) subscription.close();
        subscriptions.clear();
        children = new ArrayList<>(unique);
        ends = new int[unique.size()];
        count = 0;
        for (int index = 0; index < unique.size(); index++) {
            int offset = count;
            count += unique.get(index)
                .getStorageCount();
            ends[index] = count;
            subscriptions.add(
                unique.get(index)
                    .subscribe(change -> forwardChange(offset, change)));
        }
        unique.clear();
        if (changes.hasSubscribers()) changes.dispatch(StorageChange.reset());
        return true;
    }

    private void forwardChange(int offset, StorageChange<S, K> change) {
        if (!changes.hasSubscribers()) return;
        if (change.isReset()) {
            changes.dispatch(StorageChange.reset());
            return;
        }
        List<StorageChange.Entry<S, K>> entries = new ArrayList<>(
            change.getEntries()
                .size());
        for (StorageChange.Entry<S, K> entry : change.getEntries()) {
            entries.add(new StorageChange.Entry<>(offset + entry.getIndex(), entry.getBefore(), entry.getAfter()));
        }
        changes.dispatch(StorageChange.delta(entries));
    }

    @Override
    public StorageSubscription subscribe(Consumer<? super StorageChange<S, K>> listener) {
        return changes.subscribe(listener);
    }

    @Override
    public int getStorageCount() {
        return count;
    }

    @Override
    public S getSnapshot(int index) {
        int child = childIndex(index);
        return child < 0 ? empty
            : children.get(child)
                .getSnapshot(localIndex(index, child));
    }

    @Override
    public long getCapacity(int index) {
        int child = childIndex(index);
        return child < 0 ? 0
            : children.get(child)
                .getCapacity(localIndex(index, child));
    }

    @Override
    public TransferResult<S, K> insert(int index, S request, StorageAction action) {
        int child = childIndex(index);
        return child < 0 ? new TransferResult<>(request.getAmount(), empty, action)
            : children.get(child)
                .insert(localIndex(index, child), request, action);
    }

    @Override
    public TransferResult<S, K> extract(int index, long amount, StorageAction action) {
        int child = childIndex(index);
        return child < 0 ? new TransferResult<>(Math.max(0, amount), empty, action)
            : children.get(child)
                .extract(localIndex(index, child), amount, action);
    }

    @Override
    public boolean voidsOverflow(int index) {
        int child = childIndex(index);
        return child >= 0 && children.get(child)
            .voidsOverflow(localIndex(index, child));
    }

    protected IStorageHandler<S, K> childAt(int index) {
        int child = childIndex(index);
        return child < 0 ? null : children.get(child);
    }

    protected int childSlot(int index) {
        int child = childIndex(index);
        return child < 0 ? -1 : localIndex(index, child);
    }

    private int childIndex(int index) {
        if (index < 0 || index >= count) return -1;
        int low = 0;
        int high = ends.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (index < ends[middle]) high = middle;
            else low = middle + 1;
        }
        return low;
    }

    private int localIndex(int index, int child) {
        return index - (child == 0 ? 0 : ends[child - 1]);
    }

    public static class Items extends AggregatedStorage<BigItemStack, ItemStorageKey> implements IBigItemHandler {

        public Items() {
            super(BigItemStack.empty());
        }

        @Override
        public boolean isEmptyStorageAvailable(int index) {
            IStorageHandler<BigItemStack, ItemStorageKey> child = childAt(index);
            return child instanceof IBigItemHandler items && items.isEmptyStorageAvailable(childSlot(index));
        }
    }

    public static class Fluids extends AggregatedStorage<BigFluidStack, FluidStorageKey> implements IBigFluidHandler {

        public Fluids() {
            super(BigFluidStack.empty());
        }
    }

    public static class Aspects extends AggregatedStorage<BigAspectStack, AspectStorageKey>
        implements IBigAspectHandler {

        public Aspects() {
            super(BigAspectStack.empty());
        }
    }
}
