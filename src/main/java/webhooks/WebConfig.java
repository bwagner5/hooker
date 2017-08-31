package webhooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Autowired
    HandlerInterceptor localTrafficInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Adding interceptor patterns");
        registry.addInterceptor(localTrafficInterceptor).excludePathPatterns("/github");
    }

}
