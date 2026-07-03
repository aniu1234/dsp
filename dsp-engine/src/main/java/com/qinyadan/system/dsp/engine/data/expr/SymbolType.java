package com.qinyadan.system.dsp.engine.data.expr;


public enum SymbolType {

    /**
     * Function
     */
    FUNCTION,

    /**
     * Literal
     */
    LITERAL,

    /**
     * REFERENCE
     * <p>
     * for select directly
     */
    REFERNCE,

    /**
     * INPUT_COLUMN
     * for tablescan
     */
    INPUT_COLUMN;
}
