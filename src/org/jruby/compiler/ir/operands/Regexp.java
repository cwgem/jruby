package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.List;
import java.util.Map;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RegexpOptions;

// Represents a regexp from ruby
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this regexp operand could get converted to calls
// that actually build the Regexp object
public class Regexp extends Operand {
    final public RegexpOptions options;
    Operand regexp;
    private RubyRegexp rubyRegexp;

    public Regexp(Operand regexp, RegexpOptions options) {
        this.regexp = regexp;
        this.options = options;
    }

    @Override
    public boolean isConstant() {
        return regexp.isConstant();
    }

    @Override
    public String toString() {
        return "RE:|" + regexp + "|" + options;
    }

    @Override
    public boolean isNonAtomicValue() {
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        regexp = regexp.getSimplifiedOperand(valueMap);
        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        regexp.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return isConstant() ? this : new Regexp(regexp.cloneForInlining(ii), options);
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // If we have a constant regexp string or if the regexp patterns asks for caching, cache the regexp
        if ((!regexp.isConstant() && !options.isOnce()) || (rubyRegexp == null) || context.getRuntime().getKCode() != rubyRegexp.getKCode()) {
            RubyRegexp reg = RubyRegexp.newRegexp(context.getRuntime(),
                    ((RubyString) regexp.retrieve(interp, context, self)).getByteList(), options);
            reg.setLiteral();
            rubyRegexp = reg;
        }

        return rubyRegexp;
    }
}
