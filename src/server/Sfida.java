package server;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Sfida {
    private static final int k = 10; //ms
    private static final int timer = 600; //ms
    private String n1;
    private int pN1;
    private String n2;
    private int pN2;
    private List<String> parole;
    public Sfida(String n1, String n2, List<String> parole) {

        this.n1=n1;
        this.n2=n2;
        pN1=0;
        pN2=0;
        this.parole=parole;

    }


}
