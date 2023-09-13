package webmvc.org.springframework.web.servlet.mvc.tobe;

import context.org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import web.org.springframework.web.bind.annotation.RequestMapping;
import web.org.springframework.web.bind.annotation.RequestMethod;
import webmvc.org.springframework.web.servlet.mvc.exception.CanNotInstanceHandlerException;

public class AnnotationHandlerMapping {

    private static final Logger log = LoggerFactory.getLogger(AnnotationHandlerMapping.class);
    private static final Set<Class<?>> supportParameters = Set.of(HttpServletRequest.class,
            HttpServletResponse.class);

    private final Object[] basePackage;
    private final Map<HandlerKey, HandlerExecution> handlerExecutions;

    public AnnotationHandlerMapping(final Object... basePackage) {
        this.basePackage = basePackage;
        this.handlerExecutions = new HashMap<>();
    }

    public void initialize() {
        final var reflections = new Reflections(basePackage);
        final var classes = reflections.getTypesAnnotatedWith(Controller.class);
        for (final Class<?> clazz : classes) {
            final var handler = getHandlerInstance(clazz);
            final var methods = clazz.getMethods();

            Arrays.stream(methods)
                    .filter(this::supportParameters)
                    .filter(method -> method.isAnnotationPresent(RequestMapping.class))
                    .forEach(method -> putHandlerExecutions(handler, method));
        }
        log.info("Initialized AnnotationHandlerMapping!");
    }

    private boolean supportParameters(final Method method) {
        final var parameterTypes = Arrays.stream(method.getParameterTypes())
                .collect(Collectors.toList());

        return supportParameters.containsAll(parameterTypes) && parameterTypes.size() == 2;
    }

    private void putHandlerExecutions(final Object handler, final Method method) {
        final var annotation = method.getDeclaredAnnotation(RequestMapping.class);
        final var handlerExecution = new HandlerExecution(handler, method);

        Arrays.stream(annotation.method())
                .map(requestMethod -> new HandlerKey(annotation.value(), requestMethod))
                .forEach(handlerKey -> handlerExecutions.put(handlerKey, handlerExecution));
    }

    private static Object getHandlerInstance(final Class<?> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (NoSuchMethodException |
                IllegalAccessException |
                InstantiationException |
                 InvocationTargetException e
        ) {
            throw new CanNotInstanceHandlerException();
        }
    }

    public Object getHandler(final HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        RequestMethod requestMethod = RequestMethod.valueOf(request.getMethod());
        HandlerKey handlerKey = new HandlerKey(requestURI, requestMethod);

        return handlerExecutions.get(handlerKey);
    }
}
