package com.sipstacks.script;

import org.json.simple.JSONValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;


public class PostFunction extends Function implements Cloneable {

	public PostFunction() {
		name = "post";
	}

	String getFormParams(Object param) throws ScriptParseException {
		if(param instanceof Assignable) {
			Assignable assignable = (Assignable) param;
			param = assignable.getValue();
		}

		if (param instanceof ObjectReference) {
			if (((ObjectReference) param).toJSON() == null) {
				throw new ScriptParseException("post(): argument must be a map\nGot: " + param.toString());
			}
			param = ((ObjectReference) param).getReference();
		}

		if ( !(param instanceof Map)) {
			param = JSONValue.parse(param.toString());
			if (param == null) {
				throw new ScriptParseException("post(): argument must be a Map");
			}
		}

		StringBuffer result = new StringBuffer();

		Map map = (Map)param;
		for(Object key : map.keySet()) {
			Object value = map.get(key);
			encodeElement(key, value, result);
		}
		return result.toString();

	}

	private void encodeElement(Object key, Object value, StringBuffer result) {
		if (!(value instanceof Map) && !(value instanceof List)) {
			Object param = JSONValue.parse(value.toString());
			if (param != null) value = param;
		}
		if (value instanceof Map) {
			Map subMap = (Map) value;
			for (Object subKey : subMap.keySet()) {
				encodeElement(key + "[" + subKey + "]", subMap.get(subKey), result);
			}
		} else if (value instanceof List) {
			List subList = (List) value;
			int index = 0;
			for (Object subValue : subList) {
				encodeElement(key + "[" + index++ + "]", subValue, result);
			}

		} else {
			if (result.length() != 0) {
				result.append("&");
			}
			result.append(key.toString());
			result.append("=");
			try {
				result.append(URLEncoder.encode(value.toString(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}
	}

	public Object eval() throws ScriptParseException  {
		super.eval();
		List<Object> args = getParamObjects();
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";

		if (args.size() == 1) {
			return "post() requires at least 1 arg";
		}

		try {
			url = new URL(args.get(0).toString());
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("User-Agent", "Apache-HttpClient/4.1.1 (java 1.5)");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			if(args.size() > 1) {

				if(args.size() > 2 && Script.parseBoolean(args.get(2).toString())) {
					String json = String.valueOf(args.get(1)).toString();
					conn.setRequestProperty("Content-Type", "application/json");
					conn.setRequestProperty("Content-Length", String.valueOf(json.length()));
					java.io.OutputStream os = conn.getOutputStream();
					BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter(os, "UTF-8"));

					writer.write(json);
					writer.flush();
					writer.close();
					os.close();
				} else {
					conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					String form = getFormParams(args.get(1));
					conn.setRequestProperty("Content-Length", String.valueOf(form.length()));
					java.io.OutputStream os = conn.getOutputStream();
					BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter(os, "UTF-8"));

					writer.write(form);
					writer.flush();
					writer.close();
					os.close();
				}

			}


			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (IOException e) {
			return e.getMessage();
		} catch (Exception e) {
			return e.getMessage();
		}
		return result;
	}

	@Override
	public Function clone() {
		return new PostFunction();
	}
}
