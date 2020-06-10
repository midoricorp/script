package com.sipstacks.script;

import java.util.List;

/**
 * Created by torrey on 26/04/16.
 */
public class StatementFunction extends Function implements Cloneable  {
    Statement stmt;

    public Statement getStatement() {
        return stmt;
    }

    public Object eval() throws ScriptParseException {
        super.eval();
        List<String> strs = getParamStrings();

        try {
            OutputStream result = new OutputStream();
            stmt.exec(result, strs);
            result.trimText();
            return result;
        } catch (ScriptFlowException e) {
            throw new ScriptParseException(e.getMessage());
        }
    }

    public StatementFunction clone() {
        StatementFunction f = new StatementFunction();
        f.name = name;
        f.stmt = stmt;
        return f;
    }

    public void reset() {
        stmt.reset();
    }
}
