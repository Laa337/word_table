package utils.decomposers.tabledecomposers;

import utils.Positio;

public class RectangleDecomposer implements TableDecomposer {

    public Positio[] getRowPositios(int length) {
        Positio[] positions = new Positio[length];

        for(int i=0; i<length; i++) {
            positions[i] = new Positio(1, i+1);
        }
        // System.out.println("Row positions");
        //Arrays.asList(positions).forEach(System.out::println);
        return positions;
    }

    public Positio[] getColoumnPositios(int length) {
        Positio[] positions = new Positio[length];

        for(int i=0; i<length; i++) {
            positions[i] = new Positio(i+1, 1);
        }
        return positions;
    }

    public Positio[] getRightDiagPositios(int... length) {
        if(length.length !=2)
            return null;
        int lengthX = length[0];
        int lengthY = length[1];
        Positio[] positions = new Positio[lengthX+lengthY-1];

        for(int i=0; i<lengthX; i++) {
            positions[i] = new Positio(i+1,1);
        }
        for(int i=lengthX; i<lengthX+lengthY-1; i++) {
            positions[i] = new Positio(1,i+2-lengthX);
        }
       //   System.out.println("Right positions");
        //  Arrays.asList(positions).forEach(System.out::println);
        return positions;
    }

    public Positio[] getLeftDiagPositios(int... length) {
        if(length.length !=2)
            return null;
        int lengthX = length[0];
        int lengthY = length[1];
        Positio[] positions = new Positio[lengthX+lengthY-1];

        for(int i=0; i<lengthX; i++) {
            positions[i] = new Positio(i+1,1);
        }
        for(int i=lengthX; i<lengthX+lengthY-1; i++) {
            positions[i] = new Positio(lengthX,i+2-lengthX);
        }
      //    System.out.println("Left positions");
       //  Arrays.asList(positions).forEach(System.out::println);

        return positions;
    }

    public String[] getColoumns(String[] table) {
        int lengthY = table.length;
        int lengthX = table[0].length();
        System.out.println("Length X:  " + lengthX);
        String[] wordColumns = new String[lengthX];

        for(int i=0; i<lengthX; i++) {
            StringBuilder builder = new StringBuilder();
            for(int j=0; j<lengthY; j++) {
                builder.append(table[j].substring(i, i+1));
            }
            wordColumns[i] = builder.toString();
        }

    /*    System.out.println("\n--------------Columns----------------\n");
        for(int i=0;i<lengthX;i++) {
            System.out.println("Coloumn" + i + ": " + wordColumns[i]);
        } */


        return wordColumns;
    }

    public String[] getRightDiagonals(String[] tableRows, String[] tableColumns) {
        int lengthY = tableRows.length;
        int lengthX = tableRows[0].length();
        System.out.println("X: " + lengthX);
        String[] diagColumns = new String[lengthX+lengthY-1];

        for(int i=0;i<lengthX;i++) {
            StringBuilder builder = new StringBuilder();

            for(int j=0;j<lengthY && (i+j< lengthX);j++) {
                builder.append(tableRows[j].substring(i+j, i+j+1));
            }
            diagColumns[i] = builder.toString();
        }
        for(int i=1;i<lengthY;i++) {
            StringBuilder builder = new StringBuilder();
            for(int j=0;j+i<lengthY && j<lengthX;j++) {
                builder.append(tableColumns[j].substring(i+j, i+j+1));
            }
            diagColumns[i+lengthX-1] = builder.toString();
        }

   /*    System.out.println("\n--------------Right Diags----------------\n");
        for(int i=0;i<lengthX+lengthY-1;i++) {
            System.out.println("Coloumn" + i + ": " + diagColumns[i]);
        }  */



        return diagColumns;
    }


    public String[] getLeftDiagonals(String[] tableRows, String[] tableColumns) {
        int lengthY = tableRows.length;
        int lengthX = tableRows[0].length();
        String[] diagColumns = new String[lengthX+lengthY-1];

        for(int i=lengthX-1;i>=0;i--) {
            StringBuilder builder = new StringBuilder();
            for(int j=0;i-j>=0 && j<lengthY ;j++) {
                builder.append(tableRows[j].substring(i-j, i-j+1));
            }
            diagColumns[i] = builder.toString();
        }
        for(int i=1;i<lengthY;i++) {
            StringBuilder builder = new StringBuilder();

            for(int  j=0;j+i<lengthY && j<lengthX;j++) {
                builder.append(tableColumns[lengthX-j-1].substring(i+j, i+j+1));
            }
            diagColumns[i+lengthX-1] = builder.toString();
        }


    /*     System.out.println("\n--------------Left Diags----------------\n");
        for(int i=0;i<lengthX+lengthY-1;i++) {
            System.out.println("Coloumn" + i + ": " + diagColumns[i]);
        }   */



        return diagColumns;
    }


}
