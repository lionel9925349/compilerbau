package de.thm.mni.compilerbau.phases._06_codegen;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.ParameterType;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.utils.SplError;
import java.io.PrintWriter;
import java.util.*;

/**
 * This class is used to generate the assembly code for the compiled program.
 * This code is emitted via the {@link CodePrinter} in the output field of this class.
 */
public class CodeGenerator {
    private final CodePrinter output;
    private final boolean ershovOptimization;
    private final  Register nullRegister = new Register(0);
    private Register temporaryRegister = new Register(8);
    private final  Register fp = new Register(25);
    private final  Register sp = new Register(29);
    private final  Register returnPointer = new Register(31);
    private  int labelZaeler;
    private Stack<Integer> stack = new Stack<Integer>();
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
        labelZaeler  = 0;
        assemblerProlog();
        program.accept(new VisitorOfCodeGenerator(table));

    }
    private class VisitorOfCodeGenerator extends DoNothingVisitor{
        SymbolTable symbolTable ;

        public VisitorOfCodeGenerator(SymbolTable symbolTable){
            this.symbolTable = symbolTable;

        }


        //IntLiteral
        @Override
        public void visit(IntLiteral intLiteral){
            output.emitInstruction("add",lastFreeRegister(),nullRegister,intLiteral.value);
        }

        //BinaryExpression   !!!!!!!!!!!!!!!!!!!!!!!   ?????????????????
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
                    output.emitInstruction("sub",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(),temporaryRegister.previous());
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case MUL:
                    output.emitInstruction("mul",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(),temporaryRegister.previous());
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case DIV:
                    output.emitInstruction("div",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(),temporaryRegister.previous());
                    temporaryRegister = temporaryRegister.previous();
                    break;

                case NEQ:
                    output.emitInstruction("beq",temporaryRegister.previous().previous(),temporaryRegister.previous(),"L"+labelZaeler);
                    temporaryRegister = temporaryRegister.previous();
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case EQU:
                    output.emitInstruction("bne",temporaryRegister.previous().previous(),temporaryRegister.previous(),"L"+labelZaeler);
                    temporaryRegister = temporaryRegister.previous();
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case LSE:
                    output.emitInstruction("bgt",temporaryRegister.previous().previous(),temporaryRegister.previous(),"L"+labelZaeler);
                    temporaryRegister = temporaryRegister.previous();
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case LST:
                    output.emitInstruction("bge",temporaryRegister.previous().previous(),temporaryRegister.previous(),"L"+labelZaeler);
                    temporaryRegister = temporaryRegister.previous();
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case GRE:
                    output.emitInstruction("blt",temporaryRegister.previous().previous(),temporaryRegister.previous(),"L"+labelZaeler);
                    temporaryRegister = temporaryRegister.previous();
                    temporaryRegister = temporaryRegister.previous();
                    break;
                case GRT:
                    output.emitInstruction("ble",temporaryRegister.previous().previous(),temporaryRegister.previous(),"L"+labelZaeler);
                    temporaryRegister = temporaryRegister.previous();
                    temporaryRegister = temporaryRegister.previous();
                    break;
            }




        }
        //NamedVariable
        @Override
        public void visit(NamedVariable namedVariable){
            VariableEntry variableEntry = (VariableEntry) symbolTable.lookup(namedVariable.name);
            output.emitInstruction("add",lastFreeRegister(),fp,variableEntry.offset);
            if (variableEntry.isReference){
                output.emitInstruction("ldw",temporaryRegister.previous(),temporaryRegister.previous(),0);
            }
        }
        //VariableExpression
        @Override
        public void visit(VariableExpression variableExpression){
            variableExpression.variable.accept(this);
            output.emitInstruction("ldw",temporaryRegister.previous(),temporaryRegister.previous(),0);

        }
        //AssignStatement
        @Override
        public void visit(AssignStatement assignStatement){
            assignStatement.target.accept(this);
            assignStatement.value.accept(this);
            output.emitInstruction("stw",temporaryRegister.previous(),temporaryRegister.previous().previous(),0);
            temporaryRegister = temporaryRegister.previous();
            temporaryRegister = temporaryRegister.previous();


        }
        //ArrayAccess
        @Override
        public void visit(ArrayAccess arrayAccess){
            arrayAccess.array.accept(this);
            arrayAccess.index.accept(this);
            ArrayType arrayType = (ArrayType) arrayAccess.array.dataType;
            output.emitInstruction("add",lastFreeRegister(),nullRegister,arrayType.arraySize);
            output.emitInstruction("bgeu",temporaryRegister.previous().previous(),temporaryRegister.previous(),"_indexError");
            output.emitInstruction("mul",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(), arrayType.baseType.byteSize);
            temporaryRegister = temporaryRegister.previous();
            output.emitInstruction("add",temporaryRegister.previous().previous(),temporaryRegister.previous().previous(),temporaryRegister.previous());
            temporaryRegister = temporaryRegister.previous();


        }
        //WhileStatement  ??????????????????????????????
        @Override
        public void visit(WhileStatement whileStatement){
            int ersteLabel = labelZaeler;
            output.emitLabel("L"+labelZaeler);
            labelZaeler++;
            whileStatement.condition.accept(this);
            labelZaeler++;
            whileStatement.body.accept(this);
            output.emitInstruction("j","L" + ersteLabel);
            output.emitLabel("L"+(ersteLabel+1));
            labelZaeler++;
        }

        //IfStatement   ??????????????????????????????
        @Override
        public void visit(IfStatement ifStatement){
            labelZaeler++;
            ifStatement.condition.accept(this);
            if (ifStatement.elsePart instanceof EmptyStatement) {
                ifStatement.thenPart.accept(this);
                output.emitLabel("L" + (labelZaeler));
                labelZaeler++;
            }else {
                ifStatement.thenPart.accept(this);
                output.emitInstruction("j","L"+labelZaeler);
                output.emitLabel("L" + (labelZaeler-1));
                labelZaeler++;
                ifStatement.thenPart.accept(this);
                output.emitLabel("L" + (labelZaeler-1));
                labelZaeler++;

            }
        }
        //CallStatement
        @Override
        public void visit(CallStatement callStatement){
            ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(callStatement.procedureName);
            List<Expression> argumentList = callStatement.arguments;
            List<ParameterType> parameterTypeList = procedureEntry.parameterTypes;
            for(int i = 0; i < argumentList.size();i++){
                if (parameterTypeList.get(i).isReference){
                    VariableExpression variableExpression = (VariableExpression) argumentList.get(i);
                    variableExpression.variable.accept(this);
                }
                else {
                    argumentList.get(i).accept(this);
                }
                output.emitInstruction("stw",temporaryRegister.previous(),sp,i * 4,
                        "store argument #" + i);
                temporaryRegister = temporaryRegister.previous();
            }
            output.emitInstruction("jal",callStatement.procedureName.toString());
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
            output.emitInstruction("sub",sp,sp,frameSize,"allocate frame");
            output.emitInstruction("stw",fp,sp,oldFramePointer,"save old frame Pointer");
            output.emitInstruction("add",fp,sp,frameSize,"setup new frame pointer");

            if (procedureEntry.stackLayout.outgoingAreaSize != -1){
                output.emitInstruction("stw",returnPointer,fp,returnAdresse,"save return register");
            }
            for (Statement stInBody : procedureDeclaration.body){
                CodeGenerator.VisitorOfCodeGenerator newproc = new CodeGenerator.VisitorOfCodeGenerator(procedureEntry.localTable);
                stInBody.accept(newproc);
            }
            if (procedureEntry.stackLayout.outgoingAreaSize != -1){
                output.emitInstruction("ldw",returnPointer,fp,returnAdresse,"restore return register");
            }
            output.emitInstruction("ldw",fp,sp,oldFramePointer,"restore old frame pointer");
            output.emitInstruction("add",sp,sp,frameSize,"release frame");
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
            for (GlobalDeclaration gb : program.declarations){
                if(gb instanceof ProcedureDeclaration)
                    gb.accept(this);
            }
        }


    }
}