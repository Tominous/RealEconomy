package io.github.wysohn.realeconomy.api.smartinv.gui;

import copy.com.google.gson.Gson;
import copy.com.google.gson.GsonBuilder;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.ItemClickData;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.SlotPos;
import io.github.wysohn.rapidframework3.bukkit.utils.InventoryUtil;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.serialize.BukkitConfigurationSerializer;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTask;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.PhysicalAsset;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TradeAccountGUI implements InventoryProvider {
    private static final int ITEMS_PER_PAGE = 45;

    private final ManagerLanguage lang;
    private final BankingMediator bankingMediator;
    private final TradeMediator tradeMediator;
    private final NamespacedKey keySerialized;
    private final Function<Player, User> userFunction;

    private DataProvider<Asset> dataProvider;
    private int page = 0;
    public static final int PAGE_MAX = 128;

    public TradeAccountGUI(ManagerLanguage lang,
                           BankingMediator bankingMediator,
                           TradeMediator tradeMediator,
                           NamespacedKey keySerialized,
                           Function<Player, User> userFunction) {
        this.lang = lang;
        this.bankingMediator = bankingMediator;
        this.tradeMediator = tradeMediator;
        this.keySerialized = keySerialized;
        this.userFunction = userFunction;
    }

    /**
     * get current bank where the user is using
     *
     * @param bankUser
     * @return the current bank; null if not in a bank or doesn't have the TRADING account.
     */
    private AbstractBank getCurrentBank(IBankUser bankUser) {
        AbstractBank currentBank = bankingMediator.getUsingBank(bankUser);

        if (currentBank == null)
            return null;

        if (!currentBank.hasAccount(bankUser, BankingTypeRegistry.TRADING))
            return null;

        return currentBank;
    }

    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        User bankUser = userFunction.apply(player);
        if (bankUser == null)
            return;

        AbstractBank currentBank = getCurrentBank(bankUser);
        if (currentBank == null)
            return;

        dataProvider = currentBank.accountAssetProvider(bankUser);
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {
        User bankUser = userFunction.apply(player);
        if (bankUser == null)
            return;

        AbstractBank currentBank = getCurrentBank(bankUser);
        if (currentBank == null)
            return;

        if (dataProvider == null)
            return;

        int size = dataProvider.size();
        List<Asset> assets;
        if(page * ITEMS_PER_PAGE < size){
            assets = dataProvider.get(page, ITEMS_PER_PAGE);
        } else {
            assets = Collections.emptyList();
        }

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            inventoryContents.setEditable(SlotPos.of(i / 9, i % 9), true);

            if (i < assets.size()) {
                inventoryContents.set(i, ClickableItem.from(assetToItem(keySerialized,
                        lang,
                        userFunction.apply(player),
                        assets.get(i)), data -> clickedSlot(player, currentBank, data)));
            } else {
                inventoryContents.set(i, ClickableItem.from(new ItemStack(Material.AIR), data ->
                        clickedSlot(player, currentBank, data)));
            }
        }

        inventoryContents.set(SlotPos.of(5, 3), ClickableItem.from(pageButton(lang,
                userFunction.apply(player),
                true),
                data -> page = Math.max(0, Math.min(PAGE_MAX, page - 1))));
        inventoryContents.set(SlotPos.of(5, 4), ClickableItem.from(homeButton(lang,
                userFunction.apply(player),
                page + 1),
                data -> page = 0));
        inventoryContents.set(SlotPos.of(5, 5), ClickableItem.from(pageButton(lang,
                userFunction.apply(player),
                false),
                data -> page = Math.max(0, Math.min(PAGE_MAX, page + 1))));
    }

    private void clickedSlot(Player player, AbstractBank currentBank, ItemClickData data) {
        User bankUser = userFunction.apply(player);
        if (bankUser == null)
            return;

        if (!(data.getEvent() instanceof InventoryClickEvent))
            throw new RuntimeException("Not a click event. How?");

        InventoryClickEvent event = (InventoryClickEvent) data.getEvent();
        ItemStack slot = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (cursor != null
                && tradeMediator.isDeniedType(cursor.getType())
                && event.getAction() == InventoryAction.PLACE_ALL) {
            //trying to place illegal item into GUI. Stop right away.
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != InventoryAction.PLACE_ALL
                && event.getAction() != InventoryAction.PICKUP_ALL) {
            event.setCancelled(true);
            return;
        }

        IMemento mementoBank = currentBank.saveState();
        IMemento mementoUser = bankUser.saveState();
        FailSensitiveTask.of(() -> {
            if (cursor == null || cursor.getType() == Material.AIR) {
                // empty slot and empty item in hand; nothing will happen
                if (slot == null || slot.getType() == Material.AIR)
                    return true;

                // delete
                Asset asset = itemToAsset(keySerialized, slot);
                int assetAmount = assetAmount(asset);
                if (currentBank.removeAccountAsset(bankUser, asset.getSignature(), assetAmount) != assetAmount)
                    return false;

                // now, the slot item needs to be realized
                bankUser.realizeAsset(asset);
                // clear the slot
                event.setCurrentItem(null);
            } else {
                // add
                ItemStackSignature signature = new ItemStackSignature(cursor);
                currentBank.addAccountAsset(bankUser, signature.create(new HashMap<String, Object>() {{
                    put(ItemStackSignature.KEY_AMOUNT, cursor.getAmount());
                }}));
            }

            // since the data is modified, reset the data provider now to see the change
            // and avoid possible duplications
            dataProvider = currentBank.accountAssetProvider(bankUser);
            return true;
        }).handleException(Throwable::printStackTrace).onFail(() -> {
            // if something ever goes wrong, cancel the click event to minimize the impact.
            event.setCancelled(true);
            // also restore state
            bankUser.restoreState(mementoUser);
            currentBank.restoreState(mementoBank);
        }).run();
    }

    static ItemStack pageButton(ManagerLanguage lang,
                                User user,
                                boolean left){
        ItemStack itemStack = new ItemStack(Material.ARROW);
        InventoryUtil.parseFirstToItemTitle(lang,
                user,
                left ? RealEconomyLangs.GUI_PreviousPage : RealEconomyLangs.GUI_NextPage,
                itemStack);
        return itemStack;
    }

    static ItemStack homeButton(ManagerLanguage lang,
                                User user,
                                int displayPage){
        if(displayPage < 1 || displayPage > 64)
            displayPage = 1;

        ItemStack itemStack = new ItemStack(Material.NETHER_STAR, displayPage);
        InventoryUtil.parseFirstToItemTitle(lang,
                user,
                RealEconomyLangs.GUI_Home_Title,
                itemStack);
        int finalDisplayPage1 = displayPage;
        InventoryUtil.parseToItemLores(lang,
                user,
                RealEconomyLangs.GUI_Home_Lore,
                (s, m)-> m.addInteger(finalDisplayPage1),
                itemStack);
        return itemStack;
    }

    static ItemStack assetToItem(NamespacedKey serKey,
                                 ManagerLanguage lang,
                                 User user,
                                 Asset asset) {
        String serialized = GSON.toJson(asset, Asset.class);

        ItemStack itemStack = asset.getIcon();
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta());
        meta.setLore(asset.lore().stream()
                .map(dl -> lang.parseFirst(user, dl.lang, dl.parser))
                .map(raw -> ChatColor.translateAlternateColorCodes('&', raw))
                .collect(Collectors.toList()));
        PersistentDataContainer persistent = meta.getPersistentDataContainer();
        persistent.set(serKey, PersistentDataType.STRING, serialized);
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    static Asset itemToAsset(NamespacedKey serKey, ItemStack itemStack) {
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta());
        PersistentDataContainer persistent = meta.getPersistentDataContainer();
        String serialized = persistent.get(serKey, PersistentDataType.STRING);
        return GSON.fromJson(serialized, Asset.class);
    }

    static int assetAmount(Asset asset) {
        if (asset instanceof PhysicalAsset) {
            return ((PhysicalAsset) asset).getAmount();
        } else {
            return 1;
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC | Modifier.TRANSIENT)
            .registerTypeHierarchyAdapter(ConfigurationSerializable.class, new BukkitConfigurationSerializer())
            .registerTypeAdapter(CustomTypeAdapters.ASSET.key, CustomTypeAdapters.ASSET.value)
            .registerTypeAdapter(CustomTypeAdapters.ASSET_SIGNATURE.key, CustomTypeAdapters.ASSET_SIGNATURE.value)
            .create();
}
