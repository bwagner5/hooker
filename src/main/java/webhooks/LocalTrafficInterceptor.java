package webhooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LocalTrafficInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LocalTrafficInterceptor.class);

    private final Config config;

    @Autowired
    public LocalTrafficInterceptor(Config config) {
        this.config = config;
    }

    /**
     * This precheck interceptor checks that the request has come from localhost, private dns, or a local IP (i.e. not called from outside the network)
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        log.info("Interceptor Evaluating: " + request.getLocalName());
        String destinationHost = request.getLocalName();

        if(destinationHost.equalsIgnoreCase(this.config.getPrivateDns()) || destinationHost.equalsIgnoreCase("localhost")){
            log.info(String.format("Allowing private connection to: %s", request.getRequestURL().toString()));
            return true;
        }
        log.info(String.format("Blocking connection to: %s", request.getRequestURL().toString()));
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        return;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        return;
    }
}
