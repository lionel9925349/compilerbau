package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.ParameterType;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;
import de.thm.mni.compilerbau.utils.*;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This class is used to calculate the memory needed for variables and stack frames of the currently compiled SPL program.
 * Those value have to be stored in their corresponding fields in the {@link ProcedureEntry}, {@link VariableEntry} and
 * {@link ParameterType} classes.
 */
public class VarAllocator {
    public static final int REFERENCE_BYTESIZE = 4;

    private final boolean showVarAlloc;
    private final boolean ershovOptimization;
    private static int mostParam = 0;
    public static int numberOfCall = 0;

    /**
     * @param showVarAlloc       Whether to show the results of the variable allocation after it is finished
     * @param ershovOptimization Whether the ershov register optimization should be used (--ershov)
     */
    public VarAllocator(boolean showVarAlloc, boolean ershovOptimization) {
        this.showVarAlloc = showVarAlloc;
        this.ershovOptimization = ershovOptimization;
    }
    private static class VarallocatorVisitor extends DoNothingVisitor{
        private SymbolTable symbolTable;

        public VarallocatorVisitor(SymbolTable symbolTable){
            this.symbolTable = symbolTable;
        }

        //tous les Overwrite


        //IfStatement
        @Override
        public void visit(IfStatement ifStatement) {
            ifStatement.thenPart.accept(this);

            if (ifStatement.elsePart !=null){
                ifStatement.elsePart.accept(this);
            }


        }
        //WhileStatement
        @Override
        public void visit(WhileStatement whileStatement) {
            whileStatement.body.accept(this);


        }
        //CallStatement
        @Override
        public void visit(CallStatement callStatement) {
            numberOfCall ++;
            ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(callStatement.procedureName);
            if ((procedureEntry.parameterTypes.size() * REFERENCE_BYTESIZE) > mostParam){
                mostParam = procedureEntry.parameterTypes.size() * REFERENCE_BYTESIZE;
            }

        }
        //CompoundStatement
        @Override
        public void visit(CompoundStatement compoundStatement) {
            for (Statement cs : compoundStatement.statements){
                cs.accept(this);
            }

        }
        //ProcedureDeclaration
        @Override
        public void visit(ProcedureDeclaration procedureDeclaration) {

            for (Statement stInBody : procedureDeclaration.body){
                stInBody.accept(this);
            }
            ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name);
            if (numberOfCall == 0){
                procedureEntry.stackLayout.outgoingAreaSize = -1;
            }
            else if (mostParam == 0) {
                procedureEntry.stackLayout.outgoingAreaSize = 0;
            }
            else {
                procedureEntry.stackLayout.outgoingAreaSize = mostParam;
                mostParam = 0;
                numberOfCall = 0;
            }
        }

        //Programm
        @Override
        public void visit(Program program) {
            for (GlobalDeclaration gb : program.declarations){
                gb.accept(this);
            }
        }
}

    private static class VarallocatorVisitor2 extends DoNothingVisitor{
        private SymbolTable symbolTable;
        public VarallocatorVisitor2(SymbolTable symbolTable){
            this.symbolTable = symbolTable;
        }

        //ProcedureDeclaration
        @Override
        public void visit(ProcedureDeclaration procedureDeclaration) {
        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name);
            List<ParameterDeclaration> parDEcList = procedureDeclaration.parameters;
            List<ParameterType> parTypList = procedureEntry.parameterTypes;
            int initArgSize = 0;
            int initVarSize = 0;
            int temp = 0;
        for (ParameterDeclaration parameterDeclaration : parDEcList){
            VariableEntry variableEntry = (VariableEntry) procedureEntry.localTable.lookup(parameterDeclaration.name);
            variableEntry.offset = initArgSize;
            parTypList.get(temp).offset = initArgSize;
            initArgSize += 4;

            temp++;
        }
        procedureEntry.stackLayout.argumentAreaSize = initArgSize;

         for (VariableDeclaration variableDeclaration : procedureDeclaration.variables) {
             VariableEntry variableEntry = (VariableEntry) procedureEntry.localTable.lookup(variableDeclaration.name);
             initVarSize -= variableDeclaration.typeExpression.dataType.byteSize;
             variableEntry.offset = initVarSize;
         }
         procedureEntry.stackLayout.localVarAreaSize = -1*initVarSize;

        }

        public void visit(Program program){
            for (GlobalDeclaration gb : program.declarations){
                gb.accept(this);
            }

        }
    }



    public void allocVars(Program program, SymbolTable table) {
    // utiliser programm et faire tous les accept
        program.accept(new VarallocatorVisitor2(table));

        program.accept(new VarallocatorVisitor(table));

        if (showVarAlloc) formatVars(program, table);
    }


    /**
     * Formats and prints the variable allocation to a human-readable format
     * The stack layout
     *
     * @param program The abstract syntax tree of the program
     * @param table   The symbol table containing all symbols of the spl program
     */
    private void formatVars(Program program, SymbolTable table) {
        program.declarations.stream().filter(dec -> dec instanceof ProcedureDeclaration).map(dec -> (ProcedureDeclaration) dec).forEach(procDec -> {
            ProcedureEntry entry = (ProcedureEntry) table.lookup(procDec.name);

            AsciiGraphicalTableBuilder ascii = new AsciiGraphicalTableBuilder();
            ascii.line("...", AsciiGraphicalTableBuilder.Alignment.CENTER);

            {
                final var zipped = IntStream.range(0, procDec.parameters.size()).boxed()
                        .map(i -> new Pair<>(procDec.parameters.get(i), new Pair<>(((VariableEntry) entry.localTable.lookup(procDec.parameters.get(i).name)), entry.parameterTypes.get(i))))
                        .sorted(Comparator.comparing(p -> Optional.ofNullable(p.second.first.offset).map(o -> -o).orElse(Integer.MIN_VALUE)));

                zipped.forEach(v -> {
                    boolean consistent = Objects.equals(v.second.first.offset, v.second.second.offset);

                    ascii.line("par " + v.first.name.toString(), "<- FP + " +
                                    (consistent ?
                                            StringOps.toString(v.second.first.offset) :
                                            String.format("INCONSISTENT(%s/%s)",
                                                    StringOps.toString(v.second.first.offset),
                                                    StringOps.toString(v.second.second.offset))),
                            AsciiGraphicalTableBuilder.Alignment.LEFT);
                });
            }

            ascii.sep("BEGIN", "<- FP");
            if (!procDec.variables.isEmpty()) {
                procDec.variables.stream()
                        .map(v -> new AbstractMap.SimpleImmutableEntry<>(v, ((VariableEntry) entry.localTable.lookup(v.name))))
                        .sorted(Comparator.comparing(e -> Try.execute(() -> -e.getValue().offset).getOrElse(0)))
                        .forEach(v -> ascii.line("var " + v.getKey().name.toString(),
                                "<- FP - " + Optional.ofNullable(v.getValue().offset).map(o -> -o).map(StringOps::toString).orElse("NULL"),
                                AsciiGraphicalTableBuilder.Alignment.LEFT));

                ascii.sep("");
            }

            ascii.line("Old FP",
                    "<- SP + " + Try.execute(entry.stackLayout::oldFramePointerOffset).map(Objects::toString).getOrElse("UNKNOWN"),
                    AsciiGraphicalTableBuilder.Alignment.LEFT);

            if (Try.execute(entry.stackLayout::isLeafProcedure).getOrElse(false)) ascii.close("END", "<- SP");
            else {
                ascii.line("Old Return",
                        "<- FP - " + Try.execute(() -> -entry.stackLayout.oldReturnAddressOffset()).map(Objects::toString).getOrElse("UNKNOWN"),
                        AsciiGraphicalTableBuilder.Alignment.LEFT);

                if (entry.stackLayout.outgoingAreaSize == null || entry.stackLayout.outgoingAreaSize > 0) {

                    ascii.sep("outgoing area");

                    if (entry.stackLayout.outgoingAreaSize != null) {
                        var max_args = entry.stackLayout.outgoingAreaSize / 4;

                        for (int i = 0; i < max_args; ++i) {
                            ascii.line(String.format("arg %d", max_args - i),
                                    String.format("<- SP + %d", (max_args - i - 1) * 4),
                                    AsciiGraphicalTableBuilder.Alignment.LEFT);
                        }
                    } else {
                        ascii.line("UNKNOWN SIZE", AsciiGraphicalTableBuilder.Alignment.LEFT);
                    }
                }

                ascii.sep("END", "<- SP");
                ascii.line("...", AsciiGraphicalTableBuilder.Alignment.CENTER);
            }

            System.out.printf("Variable allocation for procedure '%s':\n", procDec.name);
            System.out.printf("  - size of argument area = %s\n", StringOps.toString(entry.stackLayout.argumentAreaSize));
            System.out.printf("  - size of localvar area = %s\n", StringOps.toString(entry.stackLayout.localVarAreaSize));
            System.out.printf("  - size of outgoing area = %s\n", StringOps.toString(entry.stackLayout.outgoingAreaSize));
            System.out.printf("  - frame size = %s\n", Try.execute(entry.stackLayout::frameSize).map(Objects::toString).getOrElse("UNKNOWN"));
            System.out.println();
            System.out.println("  Stack layout:");
            System.out.println(StringOps.indent(ascii.toString(), 4));
            System.out.println();
        });
    }
}
