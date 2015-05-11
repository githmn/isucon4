package net.isucon.isucon4;

import com.google.inject.Injector;
import me.geso.avans.Controller;
import net.isucon.isucon4.module.WebRequestScopedModule;
import net.isucon.isucon4.provider.ConnectionProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

public class Dispatcher extends me.geso.avans.Dispatcher {
    private final Injector injector;

    public Dispatcher(final Injector injector) {
        this.injector = injector;
    }

    @Override
    public void runController(
            final Class<? extends Controller> controllerClass,
            final Method method, final HttpServletRequest request,
            final HttpServletResponse response,
            final Map<String, String> captured) {
        // Close connection provider after work.
        try (ConnectionProvider connectionProvider = injector.getInstance(ConnectionProvider.class)) {
            final WebRequestScopedModule webModule = new WebRequestScopedModule(request, connectionProvider);
            final Injector childInjector = injector.createChildInjector(webModule);
            try (Controller controller = childInjector.getInstance(controllerClass)) {
                controller.invoke(method, request, response, captured);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
