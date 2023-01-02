package code.snippets.main;

import java.util.ArrayList;
import java.util.Scanner;

public class Test {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    ArrayList<String> words = new ArrayList<>();
    String line;
    while( !(line = in.nextLine()).equals("stop") ) {
      words.add(line);
    }
    for( int i = 0; i < words.size(); i++ ) {
      String val = words.get(i);
      words.set(i, "<word>" + val + "</word>");
    }
    
    for( String s : words ) {
      System.out.println(s);
    }
    in.close();
  }
}
