# AdminShop-1.18
Admin Shop Minecraft Mod for 1.18
Adminshop is a powerful mod that allows both server administrator and modpack developers to add their own "shop" into Minecraft, where players can buy and sell items at fixed prices. It uses a custom "bank account" system, in which you can create shared accounts with other players, allowing Co-Op play in servers or modpacks that rely on this mod. The mod was originally for 1.12 by Vnator, but it has been forked to 1.18 and overhauled by Ammonium.

 

# Information for Players:

AdminShop is a mod where you can buy and sell stuff manually or automatically.

To buy and sell items manually, right-click the Shop block.

To buy and sell items automatically, use the Buyer or Seller blocks.

The buyer block needs to be set a "target" by opening its GUI and clicking the item that you want to buy. Not all items are buyable, and some can be locked behind trade permits.

 

AdminShop uses "bank accounts", which means that a player can have more than one account at a time. These accounts can be shared with other players to play Co-Op in servers or modpacks.

Your first (default) bank account cannot be shared with other people, you will need to create a new account.

Bank accounts are made up by two components: The player's in-game name of the account owner, and the bank account's ID number.

For example, valid accounts are: Ammonium:0, Ammonium:1, Notch:0, Notch:1

 

You can change which account you are buying/selling from in the shop, buyer, or seller's GUI by clicking the "Change account" button. The account is displayed on the GUI as well.

 

You can create accounts, add members, and transfer money using the /shopAccounts command. Here are the available commands:

- /shopAccounts listAccounts

List every account you have access to, meaning you are either the owner or someone has added you as a member. This will show additional information, such as balance, members list, and unlocked trade permits.

- /shopAccounts createAccount [\<members\>]

 Create a new account. When you create it you will be replied with the new account's ID number, which can be used to add new members to it. Optionally, you can include member usernames to add to it.

Examples: /shopAccounts createAccount, /shopAccounts createAccount Ammonium_ Notch Jeb_

- /shopAccounts deleteAccount \<id\>

 Deletes an account with the given ID. Note: you can only delete accounts that you are the owner of, and you can't delete your personal (ID: 0) account.

Examples: /shopAccounts deleteAccount 1, /shopAccounts deleteAccount 2

- /shopAccounts addMember \<id\>  \<member\>

 Adds a new member with the given username to the account with the given ID. This member will be able to buy and sell from the account.

Examples: /shopAccounts addMember 1 Ammonium_, /shopAccounts addMember 2 Notch

- /shopAccounts removeMember \<id\> \<member\>

 Removes said member from the account's members. Same format as addMember.

- /shopAccounts transfer \<amount\> \<fromOwner\> \<fromId\> \<toOwner\> \<toId\>

 Transfers \<amount\> money from account \<fromOwner\>:\<fromId\> to account \<toOwner\>:\<toId\>. This is not the only nor easiest way to send money, just buying an expensive item and giving it to the other player for it to sell is much easier.

 

# Information for Server Admins and Modpack Developers:

Adminshop is a "player-to-server" shop, not a "player-to-player" shop. What does this mean?

Adminshop allows you to:

Set up fixed prices at which to buy and sell items from a config file.
The items are created/destroyed the moment that a player buys or sells them, there is no "limit" to the amount that can be bought or sold.
Adminshop does not allow you to:

Buy and sell items between players at a player's desired price.
Have players create their own shops with custom items and prices.
A player-to-server shop is ideal for server administrators that want to offer players the ability to buy and sell items at a specified price that will never change and will never run out of "stock", or for modpack developers that want to include a buying/selling mechanic in their modpack as a way to progress through it.

For modpack developers, they can add their own Trade Permits as a "gatekeep" in progression in the shop. I.E, you can lock the ability to buy and sell specific items behind a craftable trade permit, and only once you craft and redeem it you can unlock said items in the shop.

The shop's contents can be edited though the shop.csv (Spreadsheet) file in the config folder. The format for adding items and categories is displayed within the csv file itself. You can type /reloadshop to reload the changes. 

In order to add a custom trade permit, you need to add a crafting recipe for an adminshop:permit item with a NBT value "key" equal to a positive whole number, this is the "tier" of the permit, which dictates which buy/sell items are unlocked with it from the shop (the "tier" is set in the final column of the items in the shop.csv file.

KubeJS is recommended to add custom crafting recipes for trade permits. Experience with KubeJS is recommended. For example, a trade permit which unlocks tier "1" recipes would be written like such:
```
Item.of('adminshop:permit', "{display:{Lore:['[{\"text\":\"Buy: Emerald Ore\",\"italic\":false}]','[{\"text\":\"Sell: Emerald\",\"italic\":false}]'],Name:'[{\"text\":\"Tier 1 Trade Permit\",\"color\":\"light_purple\",\"italic\":false}]'},key:1}")
```
What matters most is the "key:1" NBT tag, this is what AdminShop uses to unlock buying and selling of items marked with a value of 1 in the last column of the shop.csv file. 
