package de.thm.mni.compilerbau.phases._06_codegen;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.phases._02_03_parser.Sym;
import de.thm.mni.compilerbau.phases._04b_semant.ProcedureBodyChecker;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.utils.NotImplemented;
import de.thm.mni.compilerbau.utils.SplError;

import java.io.PrintWriter;
import java.util.List;

/**
 * This class is used to generate the assembly code for the compiled program.
 * This code is emitted via the {@link CodePrinter} in the output field of this class.
 */
public class CodeGenerator {
    private final CodePrinter output;
    private final boolean ershovOptimization;

    /**
     * Initializes the code generator.
     *
     * @param output             The PrintWriter to the output file.
     * @param ershovOptimization Whether the ershov register optimization should be used (--ershov)
     */
    public CodeGenerator(PrintWriter output, boolean ershovOptimization) {
        this.output = new CodePrinter(output);
        this.ershovOptimization = ershovOptimization;
    }

    /**
     * Emits needed import statements, to allow usage of the predefined functions and sets the correct settings
     * for the assembler.
     */
    private void assemblerProlog() {
        output.emitImport("printi");
        output.emitImport("printc");
        output.emitImport("readi");
        output.emitImport("readc");
        output.emitImport("exit");
        output.emitImport("time");
        output.emitImport("clearAll");
        output.emitImport("setPixel");
        output.emitImport("drawLine");
        output.emitImport("drawCircle");
        output.emitImport("_indexError");
        output.emit("");
        output.emit("\t.code");
        output.emit("\t.align\t4");
    }

    public void generateCode(Program program, SymbolTable table) {
        assemblerProlog();

        //TODO (assignment 6): generate eco32 assembler code for the spl program


    }
    private class VisitorOfCodeGenerator extends DoNothingVisitor{
        SymbolTable symbolTable;
        SymbolTable localTable ;

        public VisitorOfCodeGenerator(SymbolTable symbolTable){
            this.symbolTable = symbolTable;

        }
        //IntLiteral
        @Override
        public void visit(IntLiteral intLiteral){
        }

        //BinaryExpression
        @Override
        public void visit(BinaryExpression binaryExpression){
            binaryExpression.leftOperand.accept(this);
            binaryExpression.rightOperand.accept(this);
        }
        //NamedVariable
        @Override
        public void visit(NamedVariable namedVariable){
            VariableEntry variableEntry = (VariableEntry) localTable.lookup(namedVariable.name);
        }
        //VariableExpression
        @Override
        public void visit(VariableExpression variableExpression){
            variableExpression.variable.accept(this);

        }
        //AssignStatement
        @Override
        public void visit(AssignStatement assignStatement){
            assignStatement.value.accept(this);
            assignStatement.target.accept(this);

        }
        //ArrayAccess
        @Override
        public void visit(ArrayAccess arrayAccess){
             arrayAccess.array.accept(this);
             arrayAccess.index.accept(this);

        }
        //WhileStatement
        @Override
        public void visit(WhileStatement whileStatement){
            whileStatement.body.accept(this);
            whileStatement.condition.accept(this);

        }

        //IfStatement
        @Override
        public void visit(IfStatement ifStatement){
            ifStatement.condition.accept(this);
            ifStatement.thenPart.accept(this);
            ifStatement.elsePart.accept(this);

        }
        //CallStatement
        @Override
        public void visit(CallStatement callStatement){
            ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(callStatement.procedureName);
            List<Expression> argumentList = callStatement.arguments;

        }
        //ProcedureDeclaration
        @Override
        public void visit(ProcedureDeclaration procedureDeclaration){
            ProcedureEntry procedureEntry = (ProcedureEntry) this.symbolTable.lookup(procedureDeclaration.name);

            for (Statement stInBody : procedureDeclaration.body){
                stInBody.accept(this);
            }
        }
        //CompoundStatement
        @Override
        public void visit(CompoundStatement compoundStatement) {
            for (Statement cs : compoundStatement.statements){
                cs.accept(this);
            }
        }
        // Program
        @Override
        public void visit(Program program){
            //????
        }


    }
}
