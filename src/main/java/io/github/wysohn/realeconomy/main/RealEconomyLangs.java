package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public enum RealEconomyLangs implements ILang {
    Wallet("Wallet"),
    Currencies("Currencies"),

    BankingType_Checking("Checking Account"),
    BankingType_Trading("Trading Account"),

    GUI_PreviousPage("&dPrevious Page"),
    GUI_Home_Title("&dFirst Page"),
    GUI_Home_Lore("", "&7Current Page&8: &6${integer}"),
    GUI_NextPage("&dNext Page"),
    GUI_Assets_IssuedDate("&dIssued on &a${date}"),
    GUI_Assets_Physical_Amount("&dAmount&8: &6${integer}"),

    TradeResult_Buy("&dBUY"),
    TradeResult_Sell("&6SELL"),
    TradeResult_InvalidInfo("&cSomething went"),
    TradeResult_Format("&8[${string}&8] &7Asset&8: &6${string}&8, &7Id&8: &6${integer}&8, &7Amount&8: &6${integer}"),
    TradeResult_Format2("  &8- &f${string}"),

    Command_Common_UserNotFound("&cNo player found with name &6${string}&c."),
    Command_Common_BankNotFound("&cNo bank found with name &6${string}&c."),
    Command_Common_CurrencyNotFound("&cNo currency found with name &6${string}&c."),
    Command_Common_SenderReceiverSame("&7The sender and the receiver cannot be the same."),
    Command_Common_NoCurrencyOwner("&7The currency &6${string} &7has no owner."),
    Command_Common_WithdrawRefused("&cWithdraw failed. &7Possible reasons:",
            "&71. Most likely, payer doesn't have enough money in the account.",
            "&72. If payer is a bank, it may ran out of papers to print more currencies."),
    Command_Common_DepositRefused("&cDeposit failed. &7Possible reasons:",
            "&71. Account no longer exist. Maybe the recipient account is closed?",
            "&72. Account reached maximum allowed limit."),
    Command_Common_SendSuccess("&d${string} &f=> &6${string} ${string} &f=> &d${string}&a."),
    Command_Common_InvalidOrderId("&cOrder id must be greater than 0."),
    Command_Common_PriceRange("&cPrice must be greater than 0.0 and less than ${double}"),
    Command_Common_NotInABank("&cThis command can be used only when you are in a bank."),
    Command_Common_NoAccount("&cYou must open &6${string} &caccount to do this."),
    Command_Common_InvalidAmount("&cAmount must be larger than 0."),

    Command_Wallet_Desc("Check your wallet."),
    Command_Wallet_Usage("&d/eco bal 1",
            " &8- &7First page of your wallet."),

    Command_Pay_Desc("Pay your currency to someone else."),
    Command_Pay_Usage("&d/eco pay wysohn 1304.22 dollar",
            " &8- &7Give wysohn 1304.22 dollar."),

    Command_Currencies_Desc("Show list of all currencies available in the server."),
    Command_Currencies_Usage("&d/eco give wysohn 1003.67 dollar",
            " &8- &7wysohn will get 1003.67 dollar out of thin air."),

    Command_Give_Desc("'Print' new currency and give it to target"),
    Command_Give_Usage("&d/eco give wysohn 1003.67 dollar",
            " &8- &7wysohn will get 1003.67 dollar out of thin air."),

    Command_Take_Desc("'Destroy' currency by taking it from target"),
    Command_Take_Usage("&d/eco take wysohn 1003.67 dollar",
            " &8- &7take 1003.67 dollar from wysohn. Whoosh!"),

    Command_Bank_Desc("Various operations related to bank"),
    Command_Bank_Usage("&d/eco bank info [type]",
            " &8- &7check summary of the bank. If you enter the account type, it will show the balance of your account.",
            "&d/eco bank open <type>",
            " &8- &7Open a new account of given type at current bank.",
            "&d/eco bank deposit <type> <amount>",
            " &8- &7Deposit currency to the account of given type at current bank.",
            "&d/eco bank withdraw <type> <amount>",
            " &8- &7Withdraw currency from the account of given type to current bank.",
            "&d/eco bank assets <type>",
            " &8- &7List assets stored in the acccount."),
    Command_Bank_Balance("&dBalance &8: &6${string} ${string}"),
    Command_Bank_Assets_InvalidType("&cThat account cannot store assets."),

    Command_Bank_Open_Success("&aNew account has opened!"),
    Command_Bank_Open_AlreadyExist("&7You already have the account in this bank."),
    Command_Bank_NoCurrencySet("&7The bank you are in does not have currency set."),
    Command_Bank_NoAccount("&cYou need appropriate account to proceed."),
    Command_Bank_NoCurrencyOwner("&cCurrency does not have owner."),
    Command_Bank_Success("&aTransaction successfully finished!"),

    Command_Items_Desc("List all selling items."),
    Command_Items_Usage("&d/eco items [page] [category] &8- &7list all items currently selling"),
    Command_Items_InvalidCategory("&cInvalid category &6${string}"),
    //signature, price, currency, order_id
    Command_Items_Format("&8: &6${double} ${string} &8[&bOrderId &d${integer}&8]"),

    Command_Buy_Desc("Buy a listed item."),
    Command_Buy_Usage("&d/eco buy <order id> <price> <currency> &8- &7Bid to purchase the given asset.",
            "&7Bidding on asset does not guarantee the successful trade.",
            "&Trade is made whenever there is a listed order with the price below the price you provided."),
    Command_Buy_NotEnoughCurrency("&cNot enough currency in your account.",
            "&7Remember that you are paying &6after &7the deals are matched by the system, so your &6TRADING &7account" +
                    " must be filled with enough currency in it. Otherwise, order will be canceled automatically."),

    Command_Sell_Desc("Sell an item."),
    Command_Sell_Usage("&d/eco sell <price> <currency> &8- &7sell the item in hand for specified price."),

    Command_Orders_Desc("List all orders issued by you."),
    Command_Orders_Usage("&d/eco orders &8- &7list all orders."),
    Command_Orders_Buys("&8[&aBUY &7id&8:&d${integer}&8] &7at &6${double} ${string} &e\u26c1${integer}"),
    Command_Orders_Sells("&8[&6SELL &7id&8:&d${integer}&8] &7at &6${double} ${string} &e\u26c1${integer}"),

    Command_Cancel_Desc("Cancel buy/sell order that has not processed yet."),
    Command_Cancel_Usage("&d/eco cancel <type> <order id> &8- &7cancel the order with given id.",
            "&dTypes&8: ${string}"),
    Command_Cancel_InvalidOrderType("&6${string} &cis not a valid order type."),
    Command_Cancel_Ownership("&cThat order is not issued by you."),


    Bank_Owner("&9Owner"),
    Bank_BaseCurrency("&9BaseCurrency"),
    Bank_NumAccounts("&9Number of accounts"),
    Bank_Liquidity("&9Currency in circulation"),
    Bank_Papers("&9Number of papers"),
    Bank_PaperUnlimited("&9Paper unlimited"),
    Bank_Finance("&9Finance"),

    ;

    private final String[] def;

    RealEconomyLangs(String... def) {
        this.def = def;
    }

    @Override
    public String[] getEngDefault() {
        return def;
    }
}
