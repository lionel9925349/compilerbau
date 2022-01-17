package de.thm.mni.compilerbau.phases._04b_semant;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;//
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
        //TODO (assignment 4b): Check all procedure bodies for semantic errors
        throw new NotImplemented();
    }
    private class ProcedureBodyVisitor extends DoNothingVisitor {

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
