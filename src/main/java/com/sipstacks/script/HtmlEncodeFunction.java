package com.sipstacks.script;
import java.util.List;


public class HtmlEncodeFunction extends ExternalFunction {
	
	public String run(List<String> args) {
		String result = "";

		if (args.size() == 0) {
			return "html_encode() requires 1 arg";
		}

		result = args.get(0);
		result = result.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		return result;
	}
}
