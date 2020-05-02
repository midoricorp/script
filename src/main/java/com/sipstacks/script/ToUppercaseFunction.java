package com.sipstacks.script;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;


public class ToUppercaseFunction extends ExternalCommand {
	
	public String run(List<String> args) {
		String result = "";

		if (args.size() == 0) {
			return "uc() requires 1 arg";
		}

		result = args.get(0);
		result = result.toUpperCase();

		return result;
	}
}
