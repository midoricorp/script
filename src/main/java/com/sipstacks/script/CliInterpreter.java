package com.sipstacks.script;

import java.io.InputStreamReader;
import java.io.Reader;

public class CliInterpreter {
	public static void main(String args[]) {
		try{
			Script s = new Script(new InputStreamReader(System.in));
			s.setLoopLimit(100);
			String result = s.run();
			String code = s.dump();

			System.out.println(code + "\n\n");
			System.out.println(result);

		}catch(ScriptParseException spe){
			System.err.println(spe.getMessage());
		}	
	}
}
