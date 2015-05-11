package net.isucon.isucon4.controller;

import freemarker.template.Configuration;
import me.geso.avans.ControllerBase;
import me.geso.avans.trigger.ResponseFilter;
import me.geso.webscrew.response.WebResponse;
import net.isucon.isucon4.view.FreemarkerView;

import javax.inject.Inject;

public abstract class BaseController extends ControllerBase {
    @Inject
    private Configuration freemarkerConfiguration;

    public FreemarkerView freemarker(String templatePath) {
        return new FreemarkerView(freemarkerConfiguration, templatePath, this);
    }

    @ResponseFilter
    public void securityFilters(WebResponse resp) {
        // Reducing MIME type security risks
        // http://msdn.microsoft.com/en-us/library/ie/gg622941(v=vs.85).aspx
        resp.addHeader("X-Content-Type-Options", "nosniff");

        // Avoid click jacking attacks
        // (If you want to display this site in frames, remove this header)
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/X-Frame-Options
        resp.addHeader("X-Frame-Options", "DENY");

        // MUST NOT be cached by a shared cache, such as proxy server.
        resp.addHeader("Cache-Control", "private");
    }
}

