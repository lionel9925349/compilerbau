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

import java.awt.desktop.OpenURIEvent;
import java.io.PrintWriter;
import java.util.List;

/**
 * This class is used to generate the assembly code for the compiled program.
 * This code is emitted via the {@link CodePrinter} in the output field of this class.
 */
public class CodeGenerator {
    private final CodePrinter output;
    private final boolean ershovOptimization;
    private final static int OLD_FRAME_POINTER = 4;
    private final static int REFERENCE_BYTESIZE = 4;
    private final static int OLD_RETURN_ADRESSSIZE = 4;
    private final  Register nullRegister = new Register(0);
    private Register temporaryRegister = new Register(8);
    private final  Register fp = new Register(25);
    private final  Register sp = new Register(29);
    private final  Register returnPointer = new Register(31);

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
    private Register lastFreeRegister(){
        if (temporaryRegister.isFreeUse()){
            Register varTempReg = temporaryRegister;
            temporaryRegister = varTempReg.next();
            return varTempReg;
        }
        else {
            throw SplError.RegisterOverflow();
        }
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
            output.emitInstruction("add",lastFreeRegister(),nullRegister,intLiteral.value);
        }

        //BinaryExpression
        @Override
        public void visit(BinaryExpression binaryExpression){
            binaryExpression.leftOperand.accept(this);
            binaryExpression.rightOperand.accept(this);

            switch (binaryExpression.operator){
                case ADD:
                    output.emitInstruction("add",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(),temporaryRegister.previous());
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case SUB:
                    output.emitInstruction("sub",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(),temporaryRegister.previous().previous());
                    temporaryRegister = temporaryRegister.previous();
                    break;
            }


        }
        //NamedVariable
        @Override
        public void visit(NamedVariable namedVariable){
            VariableEntry variableEntry = (VariableEntry) localTable.lookup(namedVariable.name);
            output.emitInstruction("add",lastFreeRegister(),sp,variableEntry.offset);
            if (variableEntry.isReference){
                output.emitInstruction("ldw",temporaryRegister.previous(),temporaryRegister.previous(),0);
            }
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
            output.emitInstruction("stw",temporaryRegister.previous(),temporaryRegister.previous().previous(),0);

        }
        //ArrayAccess
        @Override
        public void visit(ArrayAccess arrayAccess){
             arrayAccess.array.accept(this);
             arrayAccess.index.accept(this);
             ArrayType arrayType = (ArrayType) arrayAccess.array.dataType;
             output.emitInstruction("add",lastFreeRegister(),nullRegister,arrayType.arraySize);
             output.emitInstruction("bgeu",temporaryRegister.previous().previous(),temporaryRegister.previous(),"_indexError");
             output.emitInstruction("mul",temporaryRegister.previous(),temporaryRegister.previous(),arrayType.baseType.byteSize);
             temporaryRegister = temporaryRegister.previous();
             output.emitInstruction("add",temporaryRegister.previous(),temporaryRegister.previous(),temporaryRegister.previous().previous());
             temporaryRegister = temporaryRegister.previous();


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
            int returnAdresse;
            int frameSize ;
            int oldFramePointer;
            output.emit("\t.export   "+ procedureDeclaration.name.toString());
            output.emitLabel(procedureDeclaration.name.toString());
            returnAdresse = procedureEntry.stackLayout.oldReturnAddressOffset();

            if (procedureEntry.stackLayout.outgoingAreaSize == -1){
                frameSize = procedureEntry.stackLayout.frameSize();
                oldFramePointer = 0;
            }
            else {
                frameSize = procedureEntry.stackLayout.frameSize();
                oldFramePointer = procedureEntry.stackLayout.oldFramePointerOffset();
            }
            output.emitInstruction("sub",fp,fp,frameSize,"allocate frame");
            output.emitInstruction("stw",sp,fp,oldFramePointer,"save old frame Pointer");
            output.emitInstruction("add",sp,fp,frameSize,"setup new frame pointer");

            if (procedureEntry.stackLayout.outgoingAreaSize != -1){
                output.emitInstruction("stw",returnPointer,sp,returnAdresse,"save return register");
            }
            for (Statement stInBody : procedureDeclaration.body){
                stInBody.accept(this);
            }
            if (procedureEntry.stackLayout.outgoingAreaSize != -1){
                output.emitInstruction("ldw",returnPointer,sp,returnAdresse,"restore return register");
            }
            output.emitInstruction("ldw",sp,fp,oldFramePointer,"restore old frame pointer");
            output.emitInstruction("add",fp,fp,frameSize,"release frame");
            output.emitInstruction("jr",returnPointer,"return");

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
