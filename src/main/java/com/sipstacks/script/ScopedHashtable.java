package com.sipstacks.script;

import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Stack;

public class ScopedHashtable {
    Stack<Hashtable<String, Object>> symbolTable;

    public ScopedHashtable() {
        symbolTable = new Stack<Hashtable<String,Object>>();
        enterScope();
    }

    public void enterScope() {
        symbolTable.push(new Hashtable<String,Object>());
    }

    public void exitScope() {
        symbolTable.pop();
    }

    public Object get(String symbol) {
        ListIterator<Hashtable<String, Object>> symbolTableIterator = symbolTable.listIterator(symbolTable.size());

        while(symbolTableIterator.hasPrevious()) {
            Hashtable<String, Object> table = symbolTableIterator.previous();

            Object symbolValue = table.get(symbol);
            if (symbolValue != null) {
                return symbolValue;
            }
        }
        return null;
    }

    public void put(String symbol, Object symbolValue) {
        symbolTable.peek().put(symbol,symbolValue);
    }

    public void update(String symbol, Object symbolValue) {
        ListIterator<Hashtable<String, Object>> symbolTableIterator = symbolTable.listIterator(symbolTable.size());

        while(symbolTableIterator.hasPrevious()) {
            Hashtable<String, Object> table = symbolTableIterator.previous();

            Object oldVal = table.get(symbol);
            if (oldVal != null) {
                table.put(symbol, symbolValue);
                return;
            }
        }
    }
}
