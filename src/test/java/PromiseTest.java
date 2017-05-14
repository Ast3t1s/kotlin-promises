import net.kit.promises.Promise;
import net.kit.functions.Consumer;
import net.kit.functions.Function;
import org.junit.Assert;
import org.junit.Test;

public class PromiseTest {

    @Test
    public void mapTestPromise() {
        String expected = "Hello, World";
        String inputString = "Hello";
        Promise.Companion.success(inputString).map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + ", World";
            }
        }).then(new Consumer<String>() {
            @Override
            public void apply(String s) {
                Assert.assertEquals(expected, s);
            }
        }).flatMap(new Function<String, Promise<String>>() {
            @Override
            public Promise<String> apply(String s) {
                return s.equals(expected)? Promise.Companion.success(s) : Promise.Companion.failure(new RuntimeException("Error"));
            }
        });
    }

    @Test
    public void filterTest() {
        int n = 5;
        Promise.Companion.success(n).map(new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {
                return integer > 2;
            }
        }).then(new Consumer<Boolean>() {
            @Override
            public void apply(Boolean aBoolean) {
                Assert.assertEquals(aBoolean, n > 2);
            }
        });
    }
}
