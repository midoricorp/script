package com.sipstacks.script;

import java.io.InputStreamReader;
import java.io.Reader;

public class CliInterpreter {
	public static void main(String args[]) {
		try{
			Script s = new Script(new InputStreamReader(System.in));
			s.setLoopLimit(100);
			OutputStream result = s.run();
			String code = s.dump();

			System.out.println(code + "\n\n");
			System.out.println("Text Output:");
			System.out.println(result.getText());
			System.out.println("Html Output:");
			System.out.println(result.getHtml());

		}catch(ScriptParseException spe){
			System.err.println(spe.getMessage());
		}	
	}
}
