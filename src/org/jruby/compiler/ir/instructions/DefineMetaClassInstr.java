package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineMetaClassInstr extends Instr {
    private IRMetaClass dummyMetaClass;
    private Operand object;
    
    public DefineMetaClassInstr(Variable dest, Operand object, IRMetaClass dummyMetaClass) {
        super(Operation.DEF_META_CLASS, dest);
        
        this.dummyMetaClass = dummyMetaClass;
		  this.object = object;
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        object = object.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + object + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineMetaClassInstr(ii.getRenamedVariable(getResult()), object.cloneForInlining(ii), dummyMetaClass);
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        IRubyObject obj = (IRubyObject)object.retrieve(interp, context, self);
        
        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("no virtual class for " + obj.getMetaClass().getBaseName());
        } else {
            if (runtime.getSafeLevel() >= 4 && !obj.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
            }
            
            RubyClass singletonClass = obj.getSingletonClass();
            dummyMetaClass.getStaticScope().setModule(singletonClass);
            DynamicMethod method = new InterpretedIRMethod(dummyMetaClass.getRootMethod(), Visibility.PUBLIC, singletonClass);
            // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineMetaClass instr?
            Object v = method.call(context, singletonClass, singletonClass, "", new IRubyObject[]{}, interp.getBlock());
            getResult().store(interp, context, self, v);
            return null;
        }
    }
}
