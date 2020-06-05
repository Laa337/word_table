package utils;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author akosp
 */
public class Positio {
    public int x;
    public int y;

    public Positio(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Positio() {
    }

    @Override
    public String toString() {
        return "x: " + x + "\ty: " + y + "\n";  //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
