package com.qinyadan.system.dsp.storage.rocksdb;


public class Test {
    public static void main(String[] args) {


        new Thread(() -> {

            int i = 0;
            while (true) {
//        if (i > 5) {
//          break;
//        }
                System.out.println("child thread");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                i++;
            }
        }).start();


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
