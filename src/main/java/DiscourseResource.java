import java.io.IOException;

import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Prefix("discourse")
public class DiscourseResource {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Get()
    public Payload getMethod(Context context) throws IOException {
        logger.info(String.valueOf(context.request()));
        return Payload.ok();
    }

    @Post()
    public Payload postMethod(Context context) throws IOException {
        logger.info(String.valueOf(context.request()));
        return Payload.ok();
    }
}
