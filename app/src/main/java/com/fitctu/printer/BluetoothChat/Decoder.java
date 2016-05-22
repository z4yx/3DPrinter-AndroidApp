package com.fitctu.printer.BluetoothChat;

import android.util.Log;

public class Decoder {
	public static enum MsgType {
		INFO_LIST_FILES("LIST"), INFO_EXTRUDER("EX1"), INFO_HEATBED("BED1"), INFO_PRINT(
				"PRT"), INFO_G_G0("G0"), INFO_G_G1("G1"), INFO_G_G92("G92"), INFO_G_G161(
				"G161"), INFO_G_M6("M6"), INFO_G_M18("M18"), INFO_G_M73("M73"),
				INFO_REPLY("RE");

		private String text;

		MsgType(String text) {
			this.text = text;
		}

		public String getText() {
			return this.text;
		}

		public static MsgType fromString(String text) {
			if (text != null) {
				for (MsgType b : MsgType.values()) {
					if (text.equalsIgnoreCase(b.text)) {
						return b;
					}
				}
			}
			return null;
		}
	}

	public static class Result {
		public MsgType Type;
		public String[] Params;
	}

	public static Result Decode(String str) throws Exception {
		Result ret = new Result();
		if (!str.startsWith("!I#"))
			return null;
		int index = str.indexOf('#', 3);
		if (index == -1)
			return null;

		ret.Type = MsgType.fromString(str.substring(3, index));
		if(ret.Type == null)
			return null;
		ret.Params = str.substring(index + 1).split(",");

		return ret;
	}
}
