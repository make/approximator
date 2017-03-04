package approximator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by github.com/~make on 3.3.2017.
 */
public class Approximator {

    private final static Logger LOG = LoggerFactory.getLogger(Approximator.class);

    private static BiFunction<Double, Double, Double> MULTIPLY_IMPL = (a, b) -> a * b;
    private static BiFunction<Double, Double, Double> DIVIDE_IMPL = (a, b) -> a / b;
    private static BiFunction<Double, Double, Double> PLUS_IMPL = (a, b) -> a + b;
    private static BiFunction<Double, Double, Double> MINUS_IMPL = (a, b) -> a - b;
    private static BiFunction<Double, Double, Double> POWER_IMPL = Math::pow;
    private static Function<Double, Double> LOG_IMPL = Math::log;

    static ValueFunction V1N = new ValueFunction(-1.0, "-1");
    static ValueFunction V0 = new ValueFunction(0.0, "0");
    static ValueFunction V1 = new ValueFunction(1.0, "1");
    static ValueFunction V2 = new ValueFunction(2.0, "2");
    static ValueFunction VINF = new ValueFunction(Double.POSITIVE_INFINITY, "INF");

    static VariableFunction X = new VariableFunction("X");

    static Function<Double, Double> approximate(Double[][] pairs, int iters) {
        Function<Double, Double> bestCandidate = null;
        Double min = Double.MAX_VALUE;
        LOG.info("Findind best function using iters:" + iters);
        long startMillis = System.currentTimeMillis();
        for(int i = 0; i < iters; i++) {
            Function<Double, Double> f = buildRandomFunction(1);
            double fMin = Arrays.stream(pairs)
                    .map(arr ->
                            Math.abs(f.apply(arr[0]) - arr[1])
                    )
                    .reduce((a, b) -> a + b)
                    .get();
            if(fMin < min) {
                min = fMin;
                bestCandidate = f;
                LOG.info("Found better function " + f + " at iter " + i + " after " + (System.currentTimeMillis() - startMillis) + " ms with difference of " + min);
                if(min < 0.000001)
                    return bestCandidate;
            }
        }
        return bestCandidate;
    }

    private static Random rand = new Random();

    static Function<Double, Double> buildReducedRandomFunction(int depth) {
        return reduceIfPossible(buildRandomFunction(depth));
    }

    private static Function<Double, Double> reduceIfPossible(Function<Double, Double> f) {
        return f instanceof PrintableParentBiFunction ? ((PrintableParentBiFunction) f).reduce() : f;
    }

    private static Function<Double, Double> buildRandomFunction(int depth) {
        if(depth < 4)
            switch (rand.nextInt(6)) {
                case 0:
                    return new PrintableParentBiFunction(MULTIPLY_IMPL,'*', depth);
                case 1:
                    return new PrintableParentBiFunction(DIVIDE_IMPL,'/', depth);
                case 2:
                    return new PrintableParentBiFunction(PLUS_IMPL,'+', depth);
                case 3:
                    return new PrintableParentBiFunction(MINUS_IMPL,'-', depth);
                case 4:
                    return new PrintableParentBiFunction(POWER_IMPL,'^', depth);
                case 5:
                    return new PrintableParentFunction(LOG_IMPL, "log", depth);
            }
        else if(depth < 10)
            switch (rand.nextInt(11)) {
                case 0:
                    return new PrintableParentBiFunction(MULTIPLY_IMPL,'*', depth);
                case 1:
                    return new PrintableParentBiFunction(DIVIDE_IMPL,'/', depth);
                case 2:
                    return new PrintableParentBiFunction(PLUS_IMPL,'+', depth);
                case 3:
                    return new PrintableParentBiFunction(MINUS_IMPL,'-', depth);
                case 4:
                    return new PrintableParentBiFunction(POWER_IMPL,'^', depth);
                case 5:
                    return new PrintableParentFunction(LOG_IMPL, "log", depth);
                case 6:
                    return V1N;
                case 7:
                    return V0;
                case 8:
                    return V1;
                case 9:
                    return V2;
                case 10:
                    return X;
            }
        else
            switch (rand.nextInt(5)) {
                case 0:
                    return V1N;
                case 1:
                    return V0;
                case 2:
                    return V1;
                case 3:
                    return V2;
                case 4:
                    return X;
            }
        throw new RuntimeException("Invalid random");
    }

}

class ValueFunction implements Function<Double, Double> {

    private Double d;
    private String val;

    ValueFunction(Double d, String val) {
        this.d = d;
        this.val = val;
    }

    public Double apply(Double aDouble) {
        return d;
    }

    @Override
    public String toString() {
        return val;
    }
}

class VariableFunction implements Function<Double, Double> {

    private String val;

    VariableFunction(String val) {
        this.val = val;
    }

    public Double apply(Double aDouble) {
        return aDouble;
    }

    @Override
    public String toString() {
        return val;
    }
}

/**
 * Implements f(g(x))
 */
class PrintableParentFunction implements Function<Double, Double> {

    private final Function<Double, Double> fa;
    private final Function<Double, Double> f;
    private final String fName;

    PrintableParentFunction(Function<Double, Double> f, String fName, int depth) {
        this.f = f;
        this.fName = fName;
        this.fa = Approximator.buildReducedRandomFunction(depth + 1);
    }

    public Double apply(Double aDouble) {
        return f.apply(fa.apply(aDouble));
    }

    @Override
    public String toString() {
        return fName + "(" + fa.toString() + ")";
    }
}

/**
 * Implements f(g(x), h(x))
 */
class PrintableParentBiFunction implements Function<Double, Double> {

    private final Function<Double, Double> fa;
    private final Function<Double, Double> fb;
    private final BiFunction<Double, Double, Double> f;
    private final char op;

    PrintableParentBiFunction(BiFunction<Double, Double, Double> f, char op, int depth) {
        this.fa = Approximator.buildReducedRandomFunction(depth + 1);
        this.fb = Approximator.buildReducedRandomFunction(depth + 1);
        this.f = f;
        this.op = op;
    }

    Function reduce() {
        if(fa instanceof ValueFunction && fb instanceof ValueFunction) {
            Double val = f.apply(fa.apply(null), fb.apply(null));
            return new ValueFunction(val, val.toString());
        } else if(fa instanceof ValueFunction) {
            Double val = fa.apply(null);
            switch (op) {
                case '*':
                    if(val == 0.0)
                        return fa;
                    if(val == 1.0)
                        return fb;
                    break;
                case '+':
                    if(val == 0.0)
                        return fb;
                    break;
                case '-':
                    if(val == 0.0)
                        return fb;
                    break;
                case '^':
                    if(val == 0.0)
                        return fa;
                    if(val == 1.0)
                        return fa;
                    break;
                case '/':
                    if(val == 0.0)
                        return fa;
            }
        } else if(fb instanceof ValueFunction) {
            Double val = fb.apply(null);
            switch (op) {
                case '*':
                    if(val == 0.0)
                        return fb;
                    if(val == 1.0)
                        return fa;
                    break;
                case '+':
                    if(val == 0.0)
                        return fa;
                    break;
                case '-':
                    if(val == 0.0)
                        return fa;
                    break;
                case '^':
                    if(val == 0.0)
                        return Approximator.V1;
                    if(val == 1.0)
                        return fa;
                    break;
                case '/':
                    if(val == 0.0)
                        return Approximator.VINF;
                    if(val == 1.0)
                        return fa;
            }
        }
        return this;
    }

    public Double apply(Double aDouble) {
        return f.apply(fa.apply(aDouble), fb.apply(aDouble));
    }

    @Override
    public String toString() {
        return "(" + fa.toString() + op + fb.toString() + ")";
    }
}
