package utils.decomposers.tabledecomposers;

import utils.Positio;

public class CubeDecomposer implements TableDecomposer {


    public Positio[] getRowPositios(int length)  {
        Positio[] positions = new Positio[length];

        for (int i = 0; i < length; i++) {
            positions[i] = new Positio(1, i + 1);
        }

        return positions;
    }

    public Positio[] getColoumnPositios(int length) {
        Positio[] positions = new Positio[length];

        for (int i = 0; i < length; i++) {
            positions[i] = new Positio(i + 1, 1);
        }

        return positions;
    }

    public Positio[] getRightDiagPositios(int... lengths) {
        int length = lengths[0];
        Positio[] positions = new Positio[length * 2 - 1];

        for (int i = 0; i < length; i++) {
            positions[i] = new Positio(i + 1, 1);
        }
        for (int i = length; i < length + length - 1; i++) {
            positions[i] = new Positio(1, i + 2 - length);
        }

        return positions;
    }

    public Positio[] getLeftDiagPositios(int... lengths) {
        int length = lengths[0];
        Positio[] positions = new Positio[length * 2 - 1];

        for (int i = 0; i < length; i++) {
            positions[i] = new Positio(i + 1, 1);
        }
        for (int i = length; i < length + length - 1; i++) {
            positions[i] = new Positio(length, i + 2 - length);
        }

        return positions;
    }

    public String[] getColoumns(String[] table) {
        int length = table.length;
        String[] wordColumns = new String[length];

        for (int i = 0; i < length; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < length; j++) {
                builder.append(table[j].substring(i, i + 1));
            }
            wordColumns[i] = builder.toString();
        }

     return wordColumns;
    }

    public String[] getRightDiagonals(String[] tableRows, String[] tableColumns) {
        int length = tableRows.length;
        String[] diagColumns = new String[length * 2 - 1];

        for (int i = 0; i < length; i++) {
            StringBuilder builder = new StringBuilder();

            for (int j = 0; j + i < length; j++) {
                builder.append(tableRows[j].substring(i + j, i + j + 1));
            }
            diagColumns[i] = builder.toString();
        }
        for (int i = 1; i < length; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j + i < length; j++) {
                builder.append(tableColumns[j].substring(i + j, i + j + 1));
            }
            diagColumns[i + length - 1] = builder.toString();
        }

    /*    System.out.println("\n--------------Right Diags----------------\n");
        for(int i=0;i<length*2-1;i++) {
            System.out.println("Coloumn" + i + ": " + diagColumns[i]);
        } */

        return diagColumns;
    }


    public String[] getLeftDiagonals(String[] tableRows, String[] tableColumns) {
        int length = tableRows.length;
        String[] diagColumns = new String[length * 2 - 1];

        for (int i = length - 1; i >= 0; i--) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; i - j >= 0; j++) {
                builder.append(tableRows[j].substring(i - j, i - j + 1));
            }
            diagColumns[i] = builder.toString();
        }
        for (int i = 1; i < length; i++) {
            StringBuilder builder = new StringBuilder();

            for (int j = 0; j + i < length; j++) {
                builder.append(tableColumns[length - j - 1].substring(i + j, i + j + 1));
            }
            diagColumns[i + length - 1] = builder.toString();
        }


      /*   System.out.println("\n--------------Left Diags----------------\n");
        for(int i=0;i<length*2-1;i++) {
            System.out.println("Coloumn" + i + ": " + diagColumns[i]);
        } */

        return diagColumns;
    }
}