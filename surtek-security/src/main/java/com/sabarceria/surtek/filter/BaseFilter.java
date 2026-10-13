package com.sabarceria.surtek.filter;

import com.sabarceria.surtek.exception.AccessNotAllowException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.ClassUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base class for every security filter in the dynamic chain.
 * <p>
 * <b>Contract for implementors — only {@link #onFilterChain} is exposed:</b>
 * <ul>
 *     <li>Annotate the subclass with {@code @Component} (it becomes a bean and is picked up by
 *     {@link FilterRegistry}) and {@code @Order} to position it in the chain.</li>
 *     <li>To let the request through, call {@code chain.doFilter(request, response)}.</li>
 *     <li>To reject, throw an exception <i>before</i> calling the chain. It is rendered through
 *     the application's {@link HandlerExceptionResolver} ({@code @ControllerAdvice}); if no advice
 *     handles it, a status code is derived (401 for {@link AuthenticationException}, 403 for
 *     {@link AccessDeniedException}/{@link AccessNotAllowException}, the given status for
 *     {@link ResponseStatusException}, otherwise 500).</li>
 *     <li>To answer the request yourself (e.g. a token endpoint), write the response and do not
 *     call the chain.</li>
 * </ul>
 * WebSocket upgrades and endpoints exempted via
 * {@link com.sabarceria.surtek.model.annotation.BypassFilter} never reach {@link #onFilterChain}.
 */
@Slf4j
public abstract class BaseFilter extends OncePerRequestFilter {

    private FilterBypassRegistry bypassRegistry;

    private HandlerExceptionResolver handlerExceptionResolver;

    @Autowired
    public final void setBypassRegistry(FilterBypassRegistry bypassRegistry) {
        this.bypassRegistry = bypassRegistry;
    }

    @Autowired
    public final void setHandlerExceptionResolver(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * The single extension point of the filter.
     */
    protected abstract void onFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException;

    @Override
    protected final boolean shouldNotFilter(HttpServletRequest request) {
        if ("websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            return true;
        }
        return bypassRegistry != null && bypassRegistry.isBypassed(ClassUtils.getUserClass(this), request);
    }

    @Override
    protected final void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TrackingFilterChain tracking = new TrackingFilterChain(chain);
        try {
            onFilterChain(request, response, tracking);
        } catch (RuntimeException ex) {
            // Exceptions from downstream (controllers, later filters) are not ours to render.
            if (tracking.invoked || response.isCommitted()) {
                throw ex;
            }
            reject(request, response, ex);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws IOException {
        log.debug("{} rejected {} {}: {}", getClass().getSimpleName(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        if (handlerExceptionResolver != null
                && handlerExceptionResolver.resolveException(request, response, null, ex) != null) {
            return;
        }
        // No @ControllerAdvice handled it: never fall through with an empty 200.
        if (!response.isCommitted()) {
            response.sendError(fallbackStatus(ex).value(), ex.getMessage());
        }
    }

    private static HttpStatus fallbackStatus(RuntimeException ex) {
        if (ex instanceof AuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ex instanceof AccessDeniedException || ex instanceof AccessNotAllowException) {
            return HttpStatus.FORBIDDEN;
        }
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatus();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static final class TrackingFilterChain implements FilterChain {
        private final FilterChain delegate;
        private boolean invoked;

        private TrackingFilterChain(FilterChain delegate) {
            this.delegate = delegate;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            invoked = true;
            delegate.doFilter(request, response);
        }
    }
}
