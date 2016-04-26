package com.sipstacks.script;


import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitFunction extends Function implements Cloneable {
    public SplitFunction() {
        name = "split";
    }

    public Object eval() throws ScriptParseException {
        super.eval();
        List<String> strs = getParamStrings();

        String pattern = "\\s+";
        String expression;
        if(strs.size() == 0) {
            return "split(): must specify string to split";
        }

        expression = strs.get(0);

        if(strs.size() > 1) {
            pattern = strs.get(1);
        }

        String tmpResult[] = expression.split(pattern);
        JSONArray result = new JSONArray();
        for(String s : tmpResult) {
            result.add(s);
        }

        return result;
    }
    public Function clone() {
        return new SplitFunction();
    }


}
