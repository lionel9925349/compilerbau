package de.thm.mni.compilerbau.phases._04a_tablebuild;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.NotImplemented;
import de.thm.mni.compilerbau.utils.SplError;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is used to create and populate a {@link SymbolTable} containing entries for every symbol in the currently
 * compiled SPL program.
 * Every declaration of the SPL program needs its corresponding entry in the {@link SymbolTable}.
 * <p>
 * Calculated {@link Type}s can be stored in and read from the dataType field of the {@link Expression},
 * {@link TypeExpression} or {@link Variable} classes.
 */
public class TableBuilder {
    private final boolean showTables;

    public TableBuilder(boolean showTables) {
        this.showTables = showTables;
    }

    public SymbolTable buildSymbolTable(Program program) {
        SymbolTable table = TableInitializer.initializeGlobalTable();
        Visitor visitor = new VisiteA(table);
        program.accept(visitor);
        return table;
    }

    /**
     * Prints the local symbol table of a procedure together with a heading-line
     * NOTE: You have to call this after completing the local table to support '--tables'.
     *
     * @param name  The name of the procedure
     * @param entry The entry of the procedure to print
     */
    private static void printSymbolTableAtEndOfProcedure(Identifier name, ProcedureEntry entry) {
        System.out.format("Symbol table at end of procedure '%s':\n", name);
        System.out.println(entry.localTable.toString());
    }

    private class VisiteA extends DoNothingVisitor {
        SymbolTable upperLevelTable;
        public VisiteA(SymbolTable upperLevelTable){
            this.upperLevelTable = upperLevelTable;
        }
        @Override
        public void visit(ParameterDeclaration parameterDeclaration) {
            System.out.println("param dec visited");
        }


        @Override
        public void visit(TypeDeclaration typeDeclaration) {
            // for typedec we use upperLevalTable to insert type
            System.out.println(" type dec visited");

        }

        @Override
        public void visit(ProcedureDeclaration procedureDeclaration) {
            System.out.println("procedure dec visited");
            SymbolTable procSymTable = new SymbolTable(upperLevelTable);
            for (ParameterDeclaration pd : procedureDeclaration.parameters) {
                pd.accept(this);
            }

            // for procedure we use upperLevalTable to insert proc

        }

        public void visit(VariableDeclaration variableDeclaration) {
            System.out.println("variable declaration");
            SymbolTable procSymTable = new SymbolTable(upperLevelTable);
            VariableDeclaration vd = variableDeclaration;
            vd.typeExpression.accept(this);
        }

        @Override
        public void visit(Program program) {
            System.out.println("program visited");
            for (GlobalDeclaration gb: program.declarations) {
                gb.accept(this);
            }
        }

    }
}
