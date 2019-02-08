package com.oydipi.apigateway.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;
import static org.springframework.util.StreamUtils.copyToString;

@Service
public class CensorFilter extends ZuulFilter {

    @Value("${censored.words}")
    private List<String> censoredWords;

    public String filterType() {
        return PRE_TYPE;
    }

    public int filterOrder() {
        return 1;
    }

    public boolean shouldFilter() {
        RequestContext context = getCurrentContext();
        try {
            InputStream in = (InputStream) context.get("requestEntity");
            if (in == null) {
                in = context.getRequest().getInputStream();
            }
            String body = copyToString(in, UTF_8);
            context.set("requestEntity", new ByteArrayInputStream(body.getBytes("UTF-8")));
            return censoredWords.stream().parallel().anyMatch(body::contains);
        } catch (IOException e) {
            rethrowRuntimeException(e);
        }
        return true;
    }

    public Object run() {
        RequestContext context = getCurrentContext();
        try {
            InputStream in = (InputStream) context.get("requestEntity");
            if (in == null) {
                in = context.getRequest().getInputStream();
            }
            String body = copyToString(in, UTF_8);
            for (String censoredWord : censoredWords) {
                body = replace(body, censoredWord, repeat("*", censoredWord.length()));
            }
            context.set("requestEntity", new ByteArrayInputStream(body.getBytes("UTF-8")));
        } catch (IOException e) {
            rethrowRuntimeException(e);
        }
        return null;
    }
}
