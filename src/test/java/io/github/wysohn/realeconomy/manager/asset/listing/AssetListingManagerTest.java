package io.github.wysohn.realeconomy.manager.asset.listing;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.core.inject.module.PluginInfoModule;
import io.github.wysohn.rapidframework3.core.inject.module.TypeAsserterModule;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.testmodules.*;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.module.OrderPlacementHandlerModule;
import io.github.wysohn.realeconomy.inject.module.OrderSQLModule;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssetListingManagerTest {
    List<Module> moduleList = new LinkedList<>();
    private ISerializer mockSerializer;

    @Before
    public void init() {
        mockSerializer = mock(ISerializer.class);

        moduleList.add(new PluginInfoModule("test", "test", "test"));
        moduleList.add(new TypeAsserterModule());
        moduleList.add(new OrderSQLModule());
        moduleList.add(new OrderPlacementHandlerModule());
        moduleList.add(new MockLoggerModule());
        moduleList.add(new MockConfigModule(Pair.of(CurrencyManager.KEY_MAX_LEN, 3)));
        moduleList.add(new MockPluginDirectoryModule());
        moduleList.add(new MockSerializerModule(mockSerializer));
        moduleList.add(new MockShutdownModule(() -> {

        }));
        moduleList.add(new AbstractModule() {
            @Provides
            IPluginResourceProvider resourceProvider() {
                File folder = new File("src/main/resources/");
                return name -> {
                    try {
                        return new FileInputStream(new File(folder, name));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                };
            }
        });
    }

    @Test
    public void newListing() throws Exception {
        AssetListingManager assetListingManager = Guice.createInjector(moduleList)
                .getInstance(AssetListingManager.class);
        assetListingManager.enable();

        AssetSignature signature = mock(AssetSignature.class);
        assetListingManager.newListing(signature);

        assertNotNull(assetListingManager.fromSignature(signature));
    }

    @Test
    public void addOrder() throws Exception {
        new File("build/tmp/plugindir/orders.db").delete();

        AssetListingManager assetListingManager = Guice.createInjector(moduleList)
                .getInstance(AssetListingManager.class);
        assetListingManager.enable();

        AssetSignature signature = mock(AssetSignature.class);
        IOrderIssuer orderIssuer = new OrderIssuer(UUID.randomUUID());
        Currency currency = mock(Currency.class);

        when(currency.getKey()).thenReturn(UUID.randomUUID());

        assetListingManager.newListing(signature);

        assetListingManager.addOrder(signature,
                OrderType.BUY,
                orderIssuer,
                1034.55,
                currency,
                30);
        assetListingManager.addOrder(signature,
                OrderType.BUY,
                orderIssuer,
                4506.34,
                currency,
                30);
        assetListingManager.addOrder(signature,
                OrderType.BUY,
                orderIssuer,
                20453.55,
                currency,
                30);

        assertEquals(3, orderIssuer.getOrderIds(OrderType.BUY).size());

        List<Integer> orderIds = new ArrayList<>(orderIssuer.getOrderIds(OrderType.BUY));
        orderIds.forEach(id -> {
            try {
                assetListingManager.cancelOrder(id,
                        OrderType.BUY,
                        index -> orderIssuer.removeOrderId(OrderType.BUY, index));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        assertEquals(0, orderIssuer.getOrderIds(OrderType.BUY).size());
    }

    private static class OrderIssuer implements IOrderIssuer {
        private final Map<OrderType, Set<Integer>> orderMap = new EnumMap<>(OrderType.class);
        private final Map<UUID, BigDecimal> wallet = new HashMap<>();
        private final Set<Integer> buyOrderIdSet = new HashSet<>();
        private final Set<Integer> sellOrderIdSet = new HashSet<>();
        private final List<Asset> ownedAssets = new ArrayList<>();

        private final UUID uuid;

        public OrderIssuer(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public boolean addOrderId(OrderType type, int orderId) {
            return orderMap.computeIfAbsent(type, t -> new HashSet<>())
                    .add(orderId);
        }

        @Override
        public boolean hasOrderId(OrderType type, int orderId) {
            Set<Integer> orderSet = orderMap.get(type);
            if (orderSet == null)
                return false;

            return orderSet.contains(orderId);
        }

        @Override
        public boolean removeOrderId(OrderType type, int orderId) {
            return orderMap.computeIfAbsent(type, t -> new HashSet<>())
                    .remove(orderId);
        }

        @Override
        public Collection<Integer> getOrderIds(OrderType type) {
            return orderMap.computeIfAbsent(type, t -> new HashSet<>());
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public BigDecimal balance(Currency currency) {
            return wallet.get(currency.getKey());
        }

        @Override
        public boolean deposit(BigDecimal value, Currency currency) {
            return wallet.put(currency.getKey(),
                    wallet.getOrDefault(currency.getKey(), BigDecimal.ZERO).add(value)) != null;
        }

        @Override
        public boolean withdraw(BigDecimal value, Currency currency) {
            return wallet.put(currency.getKey(),
                    wallet.getOrDefault(currency.getKey(), BigDecimal.ZERO).subtract(value)) != null;
        }

        @Override
        public IMemento saveState() {
            return null;
        }

        @Override
        public void restoreState(IMemento savedState) {

        }

        @Override
        public void addAsset(Asset asset) {
            ownedAssets.add(asset);
        }

        @Override
        public int removeAsset(AssetSignature signature, int amount) {
            return amount;
        }

        @Override
        public DataProvider<Asset> assetDataProvider() {
            return null;
        }
    }
}