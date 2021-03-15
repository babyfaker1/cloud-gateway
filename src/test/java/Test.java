import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Test {

    static class aaa{
        List<Double> doubles = new ArrayList<>();
    }

    public static void main(String[] args) {
        List<Double> doubles = new ArrayList<>();
        doubles.add(0D);
        doubles.add(0.5D);
        doubles.add(0.75D);
        doubles.get(0);

        Double previousRange = null;
        Double currentWeight = 0D;
        Double range = previousRange + currentWeight;

    }



}
