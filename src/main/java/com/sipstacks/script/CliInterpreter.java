package com.sipstacks.script;

import java.io.InputStreamReader;
import java.io.Reader;

public class CliInterpreter {
	public static void main(String args[]) {
		try{
			Script s = new Script(new InputStreamReader(System.in));
			String result = s.run();
			System.out.println(result);

		}catch(ScriptParseException spe){
			System.err.println(spe.getMessage());
		}	
	}
}
