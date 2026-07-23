package com.qinyadan.system.dsp.engine.data.expr;

import com.qinyadan.system.dsp.core.data.type.DataType;

/**
 * A single argument of a function call.
 */
public interface FuncArg {

    /**
     * Returns the {@link DataType} of this {@link Function} argument.
     *
     * @return The DataType of the value.
     */
    DataType<?> valueType();

    /**
     * Indicates whether a Symbol can be casted or not.
     * Typically, we only allow casting of Literals, but for example
     * even if casting a column to the Literal type is not allowed,
     * casting two columns when used on both sides of an operator or
     * function is allowed.
     * Note: Convertibility checks have to be performed nevertheless.
     * This just indicates whether casting is allowed.
     *
     * @return True is casting is possible, false otherwise.
     */
    boolean canBeCasted();

    /**
     * Returns true if this Symbol holds a value that may be cast
     * preferably before other Symbols, ie. Literal and ParameterSymbols.
     *
     * @return True if it is a value symbol, False otherwise
     */
    default boolean isValueSymbol() {
        return false;
    }
}
