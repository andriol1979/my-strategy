package com.vut.mystrategy.helper;

public class LogMessage {
    public static String printLogMessage(String message) {
        return message + " on thread " + Thread.currentThread().getName();
    }
}
