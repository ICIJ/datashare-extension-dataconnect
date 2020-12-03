package org.icij.datashare;

import net.codestory.http.filters.basic.BasicAuthFilter;
import net.codestory.http.security.Users;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.HashMap;

public class DiscourseResourceTest extends AbstractProdWebServerTest{
    @ClassRule
    public static ProdWebServerRule discourse = new ProdWebServerRule();
    @BeforeClass
    public static void setUpDiscourse() {
        discourse.configure(routes -> routes
                .get("/my/url", new HashMap<String, String>() {{
                    put("Test", "Value");
                }}));
    }
    @Before
    public void setUp() throws MalformedURLException {
        DiscourseResource discourseResource = new DiscourseResource(new PropertiesProvider(new HashMap<String, String>() {{
            put("discourseUrl", "http://localhost:" + discourse.port());
            put("discourseApiKey", "testApiKey");
        }}));
        configure(routes -> routes.add(discourseResource).filter(new BasicAuthFilter("/api","ds", Users.singleUser("foo","bar"))));
    }

    @Test
    public void test_get() {
        get("/api/proxy/projectid/my/url").withPreemptiveAuthentication("foo","bar").should().respond(200).contain("\"Test\":\"Value\"");
    }
}
