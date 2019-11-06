package string;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtilities {
    public static List<String> GetLines(String i_Str){
        Reader inputString = new StringReader(i_Str);
        BufferedReader br = new BufferedReader(inputString);
        Stream<String> stringLinesStream = br.lines();
        List<String> stringLines = stringLinesStream.collect(Collectors.toList());

        return stringLines;
    }
}