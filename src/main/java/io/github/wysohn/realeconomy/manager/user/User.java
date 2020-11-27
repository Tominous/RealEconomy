package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

public class User extends BukkitPlayer implements IBankUser {
    @Inject
    private ManagerLanguage lang;
    @Inject
    private AssetListingManager listingManager;
    @Inject
    private ITaskSupervisor task;
    @Inject
    @MinCapital
    private BigDecimal minimum;
    @Inject
    @MaxCapital
    private BigDecimal maximum;

    private final Map<UUID, BigDecimal> wallet = new HashMap<>();
    private final Set<Integer> buyOrderIdSet = new HashSet<>();
    private final Set<Integer> sellOrderIdSet = new HashSet<>();

    private transient DataProvider<Pair<UUID, BigDecimal>> balanceProvider;

    private User() {
        super(null);
    }

    public User(UUID key) {
        super(key);
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        synchronized (wallet) {
            return TransactionUtil.balance(wallet, currency);
        }
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        synchronized (wallet) {
            final boolean deposit = TransactionUtil.deposit(maximum, wallet, value, currency);
            notifyObservers();
            return deposit;
        }
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        synchronized (wallet) {
            final boolean withdraw = TransactionUtil.withdraw(minimum, wallet, value, currency);
            notifyObservers();
            return withdraw;
        }
    }

    /**
     * Clear all wallet and return all of the content of the wallet.
     *
     * @return
     */
    public List<Pair<UUID, BigDecimal>> clearWallet() {
        List<Pair<UUID, BigDecimal>> copy = new ArrayList<>();
        synchronized (wallet) {
            wallet.forEach((uuid, bigDecimal) -> copy.add(Pair.of(uuid, bigDecimal)));
            wallet.clear();
            notifyObservers();
        }
        return copy;
    }

    public DataProvider<Pair<UUID, BigDecimal>> balancesPagination() {
        if (balanceProvider == null) {
            balanceProvider = new DataProviderProxy<>(range -> {
                List<Pair<UUID, BigDecimal>> copy = new ArrayList<>();
                synchronized (wallet) {
                    wallet.forEach((uuid, bigDecimal) -> copy.add(Pair.of(uuid, bigDecimal)));
                }
                copy.sort((a, b) -> b.value.compareTo(a.value));
                return copy.subList(range.index, Math.min(copy.size(), range.index + range.size));
            }, () -> {
                synchronized (wallet) {
                    return wallet.size();
                }
            });
        }
        return balanceProvider;
    }

    @Override
    public boolean addOrderId(OrderType type, int orderId) {
        boolean bool = false;
        switch (type) {
            case BUY:
                bool = buyOrderIdSet.add(orderId);
                break;
            case SELL:
                bool = sellOrderIdSet.add(orderId);
                break;
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
        notifyObservers();
        return bool;
    }

    @Override
    public boolean hasOrderId(OrderType type, int orderId) {
        switch (type) {
            case BUY:
                return buyOrderIdSet.contains(orderId);
            case SELL:
                return sellOrderIdSet.contains(orderId);
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
    }

    @Override
    public boolean removeOrderId(OrderType type, int orderId) {
        boolean bool = false;
        switch (type) {
            case BUY:
                bool = buyOrderIdSet.remove(orderId);
                break;
            case SELL:
                bool = sellOrderIdSet.remove(orderId);
                break;
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
        notifyObservers();
        return bool;
    }

    @Override
    public Collection<Integer> getOrderIds(OrderType type) {
        switch (type) {
            case BUY:
                return new HashSet<>(buyOrderIdSet);
            case SELL:
                return new HashSet<>(sellOrderIdSet);
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
    }

    @Override
    public int realizeAsset(Asset asset) {
        if (asset instanceof Item) {
            ItemStackSignature signature = (ItemStackSignature) asset.getSignature();
            return give(signature.getItemStack(), ((Item) asset).getAmount());
        } else {
            throw new RuntimeException("Not yet implemented: " + asset);
        }
    }

    /**
     * Warning) This does not save the parent state, which contains
     * the snapshot of the player's inventory content.
     *
     * This is because this class can be used even if the player is offline,
     * yet the parent class's state require player to be online.
     *
     * And since any trade will be held through TRADING account, bukkit
     * inventory is not necessary.
     * @return
     */
    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;

        synchronized (wallet) {
            wallet.clear();
            wallet.putAll(mem.wallet);
            notifyObservers();
        }

        buyOrderIdSet.clear();
        buyOrderIdSet.addAll(mem.buyOrderIdSet);

        sellOrderIdSet.clear();
        sellOrderIdSet.addAll(mem.sellOrderIdSet);
    }

    private static class Memento implements IMemento {
        private final Map<UUID, BigDecimal> wallet = new HashMap<>();
        private final Set<Integer> buyOrderIdSet = new HashSet<>();
        private final Set<Integer> sellOrderIdSet = new HashSet<>();

        public Memento(User user) {
            synchronized (wallet) {
                //UUID and BigDecimal are both immutable
                wallet.putAll(user.wallet);
                buyOrderIdSet.addAll(user.buyOrderIdSet);
                sellOrderIdSet.addAll(user.sellOrderIdSet);
            }
        }
    }

    @Override
    public String toString() {
        return Optional.ofNullable(getStringKey())
                .orElse("[?]");
    }
}
