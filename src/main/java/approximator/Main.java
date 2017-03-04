package approximator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Promise;
import ratpack.handling.Handler;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.server.RatpackServer;

import java.io.File;
import java.util.Arrays;

import static ratpack.jackson.Jackson.json;

/**
 * Created by markus on 3.3.2017.
 */
public class Main {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        RatpackServer.start(spec -> spec
                .serverConfig(c -> c.baseDir(new File("public").getAbsoluteFile()))
                .handlers(chain -> chain
                        .files(f -> f.indexFiles("index.html"))
                        .prefix("api", api -> api
                                .get("approximate/:key", approximateApi())
                        )
                )
        );
    }

    private static Handler approximateApi() {
        return ctx -> Promise
                .value(ctx.getPathTokens().get("key"))
                .map(key -> approximate(key))
                .then(responseStr -> ctx
                        .getResponse()
                        .contentType(HttpHeaderConstants.JSON)
                        .send("{\"function\":\"" + responseStr + "\"}")
                );
    }

    private static String approximate(String key) {
        return Approximator.approximate(
                Arrays.stream(key.split(";"))
                .map( s ->
                        Arrays.stream(s.split(","))
                                .map(Double::valueOf)
                                .toArray(Double[]::new)
                )
                .toArray(Double[][]::new),
                100000000
        ).toString();
    }


}
