package com.vnator.adminshop.money;

public class ClientMoneyData {
    private static long money;

    public static void setMoney(long amt){
        money = amt;
    }

    public static long getMoney(){
        return money;
    }
}
