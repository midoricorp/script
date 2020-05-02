package com.sipstacks.script;

import java.util.List;


public class ToLowercaseFunction extends ExternalCommand {
	
	public String run(List<String> args) {
		String result = "";

		if (args.size() == 0) {
			return "lc() requires 1 arg";
		}

		result = args.get(0);
		result = result.toLowerCase();

		return result;
	}
}
