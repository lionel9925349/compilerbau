package de.thm.mni.compilerbau.phases._04b_semant;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;//
import de.thm.mni.compilerbau.table.Entry;
import de.thm.mni.compilerbau.table.ProcedureEntry;//
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;//
import de.thm.mni.compilerbau.types.ArrayType;//
import de.thm.mni.compilerbau.types.PrimitiveType;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.NotImplemented;
import de.thm.mni.compilerbau.utils.SplError;

/**
 * This class is used to check if the currently compiled SPL program is semantically valid.
 * The body of each procedure has to be checked, consisting of {@link Statement}s, {@link Variable}s and {@link Expression}s.
 * Each node has to be checked for type issues or other semantic issues.
 * Calculated {@link Type}s can be stored in and read from the dataType field of the {@link Expression} and {@link Variable} classes.
 */
public class ProcedureBodyChecker {
    public void checkProcedures(Program program, SymbolTable globalTable) {
      program.accept(new ProcedureBodyVisitor(globalTable));
    }
    private class ProcedureBodyVisitor extends DoNothingVisitor {
        SymbolTable symbolTable ;


        public ProcedureBodyVisitor (SymbolTable gTable){
            this.symbolTable = gTable;
        }

        //AssignStatement
        @Override
        public void visit(AssignStatement assignStatement) {
            assignStatement.target.accept(this);
            assignStatement.value.accept(this);
            if( !(assignStatement.target.dataType == assignStatement.value.dataType)){
                throw SplError.AssignmentHasDifferentTypes(assignStatement.position);
            }else if((assignStatement.target.dataType != PrimitiveType.intType)){
                throw SplError.AssignmentRequiresIntegers(assignStatement.position);
            }
        }

        //IfStatement
        @Override
        public void visit(IfStatement ifStatement) {
            ifStatement.condition.accept(this);
            ifStatement.thenPart.accept(this);
            if (ifStatement.elsePart !=null){
                ifStatement.elsePart.accept(this);
            }
            if(!(ifStatement.condition.dataType == PrimitiveType.boolType)){
                throw SplError.IfConditionMustBeBoolean(ifStatement.position);
            }

        }
        //WhileStatement
        @Override
        public void visit(WhileStatement whileStatement) {
            whileStatement.condition.accept(this);
            whileStatement.body.accept(this);

            if(!(whileStatement.condition.dataType == PrimitiveType.boolType)){
                throw SplError.WhileConditionMustBeBoolean(whileStatement.position);
            }

        }
        //ProcedureDeclaration
        @Override
        public void visit(ProcedureDeclaration procedureDeclaration) {
            ProcedureEntry procedureEntry = (ProcedureEntry) this.symbolTable.lookup(procedureDeclaration.name);
            ProcedureBodyVisitor newVisitor = new ProcedureBodyVisitor(procedureEntry.localTable);

            for (Statement stInBody : procedureDeclaration.body){
                stInBody.accept(newVisitor);
            }
        }
        //CallStatement
        @Override
        public void visit(CallStatement callStatement) {
            Entry entry = this.symbolTable.lookup(callStatement.procedureName);
            if (entry == null){
                throw  SplError.UndefinedProcedure(callStatement.position,callStatement.procedureName);
            }
            else if( !(entry instanceof ProcedureEntry) ) {
                throw  SplError.CallOfNonProcedure(callStatement.position,callStatement.procedureName);
            }
            else {
                ProcedureEntry procedureEntry = (ProcedureEntry)entry;
                if (callStatement.arguments.size() > procedureEntry.parameterTypes.size()){
                    throw SplError.TooManyArguments(callStatement.position,callStatement.procedureName);
                }
                if (callStatement.arguments.size() < procedureEntry.parameterTypes.size()){
                    throw SplError.TooFewArguments(callStatement.position,callStatement.procedureName);
                }
                for ( int i=0;i < callStatement.arguments.size();i++){
                    if ( callStatement.arguments.get(i).dataType != procedureEntry.parameterTypes.get(i).type){
                        throw SplError.ArgumentTypeMismatch(callStatement.position,callStatement.procedureName,i);
                    }
                    if ( procedureEntry.parameterTypes.get(i).isReference && !(callStatement.arguments.get(i) instanceof VariableExpression)){
                        throw SplError.ArgumentMustBeAVariable(callStatement.position,callStatement.procedureName,i);
                    }


                }

            }






        }




        //BinaryExpression
        public void visit(BinaryExpression binaryExpression) {


        }
        //CompoundStatement
        @Override
        public void visit(CompoundStatement compoundStatement) {
            for (Statement cs : compoundStatement.statements){
                cs.accept(this);
            }
        }
        //IntLiteral
        @Override
        public void visit(IntLiteral intLiteral) {
           intLiteral.dataType = PrimitiveType.intType;
        }
        //VariableExpression
        @Override
        public void visit (VariableExpression variableExpression){
            variableExpression.variable.accept(this);
            variableExpression.dataType = variableExpression.variable.dataType;

        }
        //Programm
        @Override
        public void visit(Program program) {
            for (GlobalDeclaration gb : program.declarations){
                gb.accept(this);
            }
        }

    }
}
