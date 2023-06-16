package com.ammonium.adminshop.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ammonium.adminshop.AdminShop;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.Item;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and stores the shop contents from a csv file. Is a singleton.
 */
public class Shop {

    private static final String SHOP_FILE_LOCATION = "config/"+ AdminShop.MODID +"/shop.csv";
    private static final String DEFAULT_SHOP_FILE = "assets/"+ AdminShop.MODID +"/default_shop.csv";

    //Matches "(value) as (var_type)" outside a string to turn back into just the value
    private static final String CT_CAST_REGEX = "([0-9]+(\\.[0-9]*)?|true|false) as " +
            "(int|short|byte|bool|float|double|long)(\\[\\])?(?=[^\"]*(\"[^\"]*\"[^\"]*)*$)";
    private static Shop instance;

    public String shopTextRaw;

    private final List<ShopItem> shopStockBuy;
    private final Map<Item, ShopItem> shopBuyMap;
    private final List<ShopItem> shopStockSell;
    private final Map<Item, ShopItem> shopSellMap;
    public List<String> errors;

    public static Shop get(){
        if(instance == null)
            instance = new Shop();
        return instance;
    }

    public Shop(){
        shopStockBuy = new ArrayList<>();
        shopStockSell = new ArrayList<>();
        shopBuyMap = new HashMap<>();
        shopSellMap = new HashMap<>();
        errors = new ArrayList<>();

        loadFromFile((CommandSource) null);
    }

    public List<ShopItem> getShopStockBuy() {
        return shopStockBuy;
    }

    public List<ShopItem> getShopStockSell() {
        return shopStockSell;
    }

    public Map<Item, ShopItem> getShopBuyMap() {
        return shopBuyMap;
    }

    public Map<Item, ShopItem> getShopSellMap() {
        return shopSellMap;
    }

    public void loadFromFile(CommandSource initiator){
        generateDefaultShopFile();
        try {
            loadFromFile(Files.readString(Path.of(SHOP_FILE_LOCATION)), initiator);
        }catch (FileNotFoundException e) {
            AdminShop.LOGGER.error("Shop file not found. This should not happen!");
        }catch (IOException e){
            AdminShop.LOGGER.error("Problem reading header/skipping first record in shop file!");
            e.printStackTrace();
        }
    }

    public void loadFromFile(String csv, CommandSource initiator) throws IOException {
        //Clear out existing shop data
        shopTextRaw = csv;
        errors.clear();
        shopStockBuy.clear();
        shopStockSell.clear();
        shopBuyMap.clear();
        shopSellMap.clear();

        //Parse file
        List<List<String>> parsedCSV = CSVParser.parseCSV(csv);
        int line = 0;
        for(List<String> record : parsedCSV){
            line++;
            parseLine(record.toArray(new String[]{}), line, errors);
        }

        printErrors(initiator);
    }

    public void printErrors(CommandSource initiator){
        if(initiator != null){
            if(errors.size() == 0)
                initiator.sendMessage(new TextComponent("Shop reloaded, syntax is correct!"), Util.NIL_UUID);
            errors.forEach(e -> initiator.sendMessage(new TextComponent(e), Util.NIL_UUID));
            errors.clear();
        }
    }

    private void parseLine(String[] line, int lineNumber, List<String> errors){
        //Skip empty lines
        if(line.length == 0)
            return;
        //Skip comments
        if(line[0].indexOf("//") == 0)
            return;
        //Skip lines that begin with an empty cell
        if(line[0].equals(""))
            return;

        //Shop item (default)

        //Parse each column and confirm it contains "valid" input
        boolean isError = false;
        if(line.length < 5){
            errors.add("Line "+lineNumber+":\tExpected shop item on this row, which requires 5 columns." +
                    " Not enough columns");
            isError = true;
            return;
        }
            //Check if buy or sell is correctly specified
        if(!(line[0].equalsIgnoreCase("buy") || line[0].equalsIgnoreCase("sell")
                || line[0].equalsIgnoreCase("b") || line[0].equalsIgnoreCase("s"))){
            errors.add("Line "+lineNumber+":\tFirst column must be either \"buy\", \"sell\", \"b\", or \"s\""+
                    "Value: "+line[0]);
            isError = true;
        }

            //Check if item or fluid is correctly specified
        if(!(line[1].equalsIgnoreCase("item") || line[1].equalsIgnoreCase("i")
                || line[1].equals("fluid") || line[1].equalsIgnoreCase("f"))) {
            errors.add("Line "+lineNumber+":\tSecond column must be either \"item\", \"fluid\", \"i\", or \"f\""+
                    "Value: "+line[1]);
            isError = true;
        }

            //Check if price is a number
        long price;
        try {
            price = Long.parseLong(line[3]);
        }catch (NumberFormatException e){
            price = 1;
            errors.add("Line "+lineNumber+":\tFourth column must be a whole number. Value:"+line[3]);
            isError = true;
        }

            // Check if permit tier is a non-negative integer
        int permitTier;
        try {
            permitTier = Integer.parseInt(line[4]);
        } catch (NumberFormatException e){
            permitTier = 0;
            errors.add("Line "+lineNumber+":\tFifth column must be a non-negative integer. Value:"+line[3]);
            isError = true;
        }
        if (permitTier < 0) {
            permitTier = 0;
            errors.add("Line "+lineNumber+":\tFifth column must be a non-negative integer. Value:"+line[3]);
            isError = true;
        }

            //Check if buy or sell
        boolean isBuy = line[0].equalsIgnoreCase("buy") || line[0].equalsIgnoreCase("b");

            //Check if both tag and buying
        boolean isTag = line[2].contains("<tag:") || line[2].contains("#");
        if(isTag && isBuy){
            errors.add("Line "+lineNumber+":\tTags can only be sold, not bought." +
                    " Please specify a unique item or change the first column to sell");
            isError = true;
        }

            //Discovered at least one shop item-breaking error. Return here
        if(isError) {
            return;
        }

        boolean isItem = line[1].equals("item") || line[1].equals("i");

            //Separate nbt from item/fluid name
        String nbtText = null;
        CompoundTag nbt = null;
        if(line[2].contains(".withTag(")){
            String nbtBase = line[2].split("\\.withTag\\(")[1];
            nbtText = nbtBase.substring(0, nbtBase.length());
            line[2] = line[2].split("\\.withTag\\(")[0];
        }else if(line[2].contains("{")){
            nbtText = line[2].substring(line[2].indexOf('{')-1).trim();
            line[2] = line[2].substring(0, line[2].indexOf('{')-1).trim();
        }
            //Check if NBT can be parsed
        if(nbtText != null){
            try {
                nbt = parseNbtString(nbtText);
            }catch (CommandSyntaxException e){
                errors.add("Line "+lineNumber+":\tImproperly formatted NBT. Make sure there aren't too many complex" +
                        " castings if copying directly from crafttweaker. You might need to manually remove them.");
                errors.add("\tNBT: "+nbtText);
                isError = true;
            }
        }

            //Strip extraneous text from item/fluid name
        StringBuilder nameBuilder = new StringBuilder();
        String[] split = line[2].split(":");
            //KubeJS style
        if(split.length == 2){
            if(isTag){
                nameBuilder.append(split[0].substring(1));
                nameBuilder.append(':');
                nameBuilder.append(split[1]);
            }else{
                nameBuilder.append(line[2]);
            }
        }
            //Crafttweaker style
        else{
            //mod name : item name, remove the > at the end
            if(isTag){
                nameBuilder.append(split[2]);
                nameBuilder.append(split[3].substring(0, split[3].length()));
            }else{
                nameBuilder.append(split[1]);
                nameBuilder.append(split[2].substring(0, split[2].length()));
            }
        }

        ShopItem item = new ShopItem.Builder()
                .setIsBuy(isBuy)
                .setIsItem(isItem)
                .setIsTag(isTag)
                .setData(nameBuilder.toString(), nbt)
                .setPrice(price)
                .setPermitTier(permitTier)
                .build();

            //Check if ShopItem was created correctly
        if(!isTag && item.getItem() == null){
            errors.add("Line "+lineNumber+":\tShop Item could not be created. The item or fluid name does not map to" +
                    " an existing item or fluid.");
            isError = true;
        }
            //Check if ShopItem found a matching item/fluid for the supplied tag
        if(isTag && item.getItem() == null){
            errors.add("Line "+lineNumber+":\t[WARNING] Supplied tag does not match any existing item or fluid." +
                    " The shop item will still be created, but will be virtually useless until something is mapped to" +
                    " the supplied tag.");
            //Continue anyway if no other errors have occurred yet
        }

        List<ShopItem> shopList = isBuy ? shopStockBuy : shopStockSell;
        Map<Item, ShopItem> shopMap = isBuy ? shopBuyMap : shopSellMap;
        shopList.add(item);
        shopMap.put(item.getItem().getItem(), item);
    }

    /**
     * Clean the nbt string for any Crafttweaker casting expressions and convert to CompoundTag
     * @param nbt NBT data in String format
     * @return CompoundTag equivalent of the parameter string
     * @throws CommandSyntaxException when nbt cannot be parsed
     */
    private CompoundTag parseNbtString(String nbt) throws CommandSyntaxException {
        Matcher matcher = Pattern.compile(CT_CAST_REGEX).matcher(nbt);
        while (matcher.find()){
            String num = matcher.group().substring(0, matcher.group().indexOf(" as"));
            nbt = nbt.replace(matcher.group(), num);
        }
        return TagParser.parseTag(nbt);
    }

    /**
     * Generate a default shop file if one does not already exist
     */
    private void generateDefaultShopFile(){
        Path shop_file_path = Path.of(SHOP_FILE_LOCATION);
        if(Files.notExists(shop_file_path)){
            try {
                InputStream defStream = AdminShop.class.getClassLoader().getResourceAsStream(DEFAULT_SHOP_FILE);
                byte [] buffer = new byte[defStream.available()];
                defStream.read(buffer);
                Files.createDirectories(Path.of("config/"+AdminShop.MODID));
                Files.createFile(shop_file_path);
                FileOutputStream outStream = new FileOutputStream(new File(SHOP_FILE_LOCATION));
                outStream.write(buffer);
            }catch (IOException e){
                AdminShop.LOGGER.error("Could not copy default shop file to config");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
