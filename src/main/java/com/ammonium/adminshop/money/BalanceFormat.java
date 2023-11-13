package com.ammonium.adminshop.money;

import com.ammonium.adminshop.setup.Config;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class BalanceFormat extends DecimalFormat {
    public static final double FORMAT_START = 1000000;

    public enum IgnoreShiftType {
        NONE,
        FORMAT,
        NO_FORMAT
    }

    private BalanceFormat() {
        super("#,###");
        setRoundingMode(RoundingMode.DOWN);
    }

    public static String stdformat(long value) {
        return format(value, IgnoreShiftType.NONE);
    }

    public static String format(long value, IgnoreShiftType ignoreShiftType) {
        if (shouldFormat(value, ignoreShiftType)) {
            NumberName name = NumberName.findName(value);
            if(name == null) return NumberFormat.getNumberInstance(Locale.US).format(value);
            return getShort(value) + (true ? String.format(" %s", name.getName(false)) : name.getName(true));
        } else return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    public static boolean shouldFormat(long value, IgnoreShiftType ignoreShiftType) {
        return (true && ignoreShiftType != IgnoreShiftType.NO_FORMAT && (ignoreShiftType == IgnoreShiftType.FORMAT || !Screen.hasShiftDown()) && value > FORMAT_START);
    }

    public static String getShort(long value) {
        return getShort((int) value);
    }


    static String getShort(int value) {
        String str = String.valueOf(value);
        ArrayList<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for(int i = str.length(); i > 0; i--) {
            if(current.length() >= 3) {
                list.add(current.reverse().toString());
                current.delete(0, current.length());
            }
            current.append(str.charAt(i - 1));
        }
        if(!current.isEmpty()) {
            list.add(current.reverse().toString());
            current.delete(0, current.length());
        }
        String sig = list.get(list.size() - 1);
        String dec = list.get(list.size() - 2).substring(0, 2);
        return String.format("%s.%s", sig, dec);
    }

    public enum NumberName {
        MILLION(1e6, "M"),
        BILLION(1e9, "B"),
        TRILLION(1e12, "T"),
        QUADRILLION(1e15, "Qa"),
        QUINTILLION(1e18, "Qi"),
        SEXTILLION(1e21, "Sx"),
        SEPTILLION(1e24, "Sp"),
        OCTILLION(1e27, "O"),
        NONILLION(1e30, "N"),
        DECILLION(1e33, "D"),
        UNDECILLION(1e36, "U"),
        DUODECILLION(1e39, "Du"),
        TREDECILLION(1e42, "Tr"),
        QUATTUORDECILLION(1e45, "Qt"),
        QUINDECILLION(1e48, "Qd"),
        SEXDECILLION(1e51, "Sd"),
        SEPTENDECILLION( 1e54, "St"),
        OCTODECILLION(1e57, "Oc"),
        NOVEMDECILLION(1e60, "No");

        public static final NumberName[] VALUES = values();

        private final double value;
        private final String shortName;
        NumberName(double val, String shortName) {
            this.value  = val;
            this.shortName = shortName;
        }

        public String getName() {
            return getName(false);
        }

        public String getName(boolean getShort) {
            return getShort ? shortName : (name().charAt(0) + name().toLowerCase(Locale.US).substring(1)).trim();
        }

        public double getValue() {return value;}

        static @Nullable NumberName findName(long value) {
            return Arrays.stream(VALUES).filter(v -> value >= v.getValue()).reduce((first, second) -> second).orElse(null);
        }
    }
}
