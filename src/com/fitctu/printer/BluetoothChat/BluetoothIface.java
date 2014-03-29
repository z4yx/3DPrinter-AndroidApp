package com.fitctu.printer.BluetoothChat;

public interface BluetoothIface {
	 void sendMessage(String message);
	 String buildMessage(String cmd, String arg);
}
