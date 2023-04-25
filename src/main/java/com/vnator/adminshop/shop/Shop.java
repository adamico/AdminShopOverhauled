package com.vnator.adminshop.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vnator.adminshop.AdminShop;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TextComponent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and stores the shop contents from a csv file. Is a singleton.
 */
public class Shop {

    private static String SHOP_FILE_LOCATION = "config/"+ AdminShop.MODID +"/shop.csv";
    private static final String DEFAULT_SHOP_FILE = "assets/"+ AdminShop.MODID +"/default_shop.csv";
    private static final String BUY_CATEGORIES_TEXT = "Buy Categories:";
    private static final String SELL_CATEGORIES_TEXT = "Sell Categories:";
    //Matches "(value) as (var_type)" outside of a string to turn back into just the value
    private static final String CT_CAST_REGEX = "([0-9]+(\\.[0-9]*)?|true|false) as " +
            "(int|short|byte|bool|float|double|long)(\\[\\])?(?=[^\"]*(\"[^\"]*\"[^\"]*)*$)";
    private static Shop instance;

    public String shopTextRaw;

    public List<String> categoryNamesBuy;
    public List<String> categoryNamesSell;

    public List<List<ShopItem>> shopStockBuy;
    public List<List<ShopItem>> shopStockSell;

    public List<String> errors;

    public static Shop get(){
        if(instance == null)
            instance = new Shop();
        return instance;
    }

    public Shop(){
        categoryNamesBuy = new ArrayList<>();
        categoryNamesSell = new ArrayList<>();
        shopStockBuy = new ArrayList<>();
        shopStockSell = new ArrayList<>();
        errors = new ArrayList<>();

        loadFromFile((CommandSource) null);
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
        categoryNamesBuy.clear();
        categoryNamesSell.clear();
        shopStockBuy.forEach(s -> s.clear());
        shopStockBuy.clear();
        shopStockSell.forEach(s -> s.clear());
        shopStockSell.clear();

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

        //Category names
        if(line[0].equals(BUY_CATEGORIES_TEXT)){
            for(int i = 1; i < line.length; i++) {
                if(line[i].equals("")) break;
                categoryNamesBuy.add(line[i]);
                shopStockBuy.add(new ArrayList<>());
            }
            return;
        }else if(line[0].equals(SELL_CATEGORIES_TEXT)){
            for(int i = 1; i < line.length; i++) {
                if(line[i].equals("")) break;
                categoryNamesSell.add(line[i]);
                shopStockSell.add(new ArrayList<>());
            }
            return;
        }

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
            errors.add("Line "+lineNumber+":\tFirst column must be either \"buy\", \"sell\", \"b\", or \"s\"");
            isError = true;
        }

            //Check if item or fluid is correctly specified
        if(!(line[1].equalsIgnoreCase("item") || line[1].equalsIgnoreCase("i")
                || line[1].equals("fluid") || line[1].equalsIgnoreCase("f"))) {
            errors.add("Line "+lineNumber+":\tSecond column must be either \"item\", \"fluid\", \"i\", or \"f\"");
            isError = true;
        }

            //Check if price is a number
        try {
            Double.parseDouble(line[3]);
        }catch (NumberFormatException e){
            errors.add("Line "+lineNumber+":\tFourth column must be a number, decimals optional.");
            isError = true;
        }

            //Check if category index is an integer
        int index = -1;
        try{
            index = Integer.parseInt(line[4]);
        }catch (NumberFormatException e){
            errors.add("Line "+lineNumber+":\tFifth column must be a whole number, with NO decimals.");
            isError = true;
        }

            //Check if category index is out of bounds
        boolean isBuy = line[0].equalsIgnoreCase("buy") || line[0].equalsIgnoreCase("b");
        List<String> categoryList = isBuy ? categoryNamesBuy : categoryNamesSell;
        if(index > categoryList.size() || index < 0){
            errors.add("Line "+lineNumber+":\tFifth column must be between 0 and the size of the category list.");
            errors.add("\teg. Can't have a category index of 5 when there are only 2 categories defined.");
            isError = true;
        }

            //Check if both tag and buying
        boolean isTag = line[2].indexOf("<tag:") != -1 || line[2].indexOf("#") != -1;
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
                .build();

            //Check if ShopItem was created correctly
        if(!isTag && item.getItem() == null && item.getFluid() == null){
            errors.add("Line "+lineNumber+":\tShop Item could not be created. The item or fluid name does not map to" +
                    " an existing item or fluid.");
            isError = true;
        }
            //Check if ShopItem found a matching item/fluid for the supplied tag
        if(isTag && item.getItem() == null && item.getFluid() == null){
            errors.add("Line "+lineNumber+":\t[WARNING] Supplied tag does not match any existing item or fluid." +
                    " The shop item will still be created, but will be virtually useless until something is mapped to" +
                    " the supplied tag.");
            //Continue anyway if no other errors have occurred yet
        }

        List<List<ShopItem>> shopList = isBuy ? shopStockBuy : shopStockSell;
        shopList.get(Integer.parseInt(line[4])).add(item);
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
        if(Files.notExists(Path.of(SHOP_FILE_LOCATION))){
            try {
                InputStream defStream = AdminShop.class.getClassLoader().getResourceAsStream(DEFAULT_SHOP_FILE);
                byte [] buffer = new byte[defStream.available()];
                defStream.read(buffer);
                Files.createDirectories(Path.of("config/"+AdminShop.MODID));
                Files.createFile(Path.of(SHOP_FILE_LOCATION));
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
