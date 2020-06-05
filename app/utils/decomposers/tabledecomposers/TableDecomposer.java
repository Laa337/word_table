package utils.decomposers.tabledecomposers;

import utils.Positio;

public interface TableDecomposer {
    Positio[] getRowPositios(int length);
    Positio[] getColoumnPositios(int length);
    Positio[] getRightDiagPositios(int... length);
    Positio[] getLeftDiagPositios(int... length);

    String[] getRightDiagonals(String[] tableRows, String[] tableColumns);
    String[] getLeftDiagonals(String[] tableRows, String[] tableColumns);
    String[] getColoumns(String[] table);

}
