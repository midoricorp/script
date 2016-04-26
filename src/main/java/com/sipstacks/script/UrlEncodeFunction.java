package com.sipstacks.script;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.List;


public class UrlEncodeFunction extends ExternalCommand {
	
	public String run(List<String> args) {
		String result = "";

		if (args.size() == 0) {
			return "url_encode() requires 1 arg";
		}

		result = args.get(0);
		try {
			result = URLEncoder.encode(result, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		return result;
	}
}
