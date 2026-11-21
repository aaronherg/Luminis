package com.aarondevs.luminis.Controllers.General;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PagesController {
    @GetMapping({"/pages", "/pages/"})
    public void pages(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!request.getRequestURI().endsWith("/")) {
            response.sendRedirect(request.getContextPath() + "/pages/");
            return;
        }
        request.getRequestDispatcher("/pages/index.html").forward(request, response);
    }

    @GetMapping({"/pages/{pagina:login|registro|docs|center|customer}", "/pages/{pagina:login|registro|docs|center|customer}/"})
    public void pagina(@PathVariable String pagina, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!request.getRequestURI().endsWith("/")) {
            response.sendRedirect(request.getContextPath() + "/pages/" + pagina + "/");
            return;
        }
        request.getRequestDispatcher("/pages/" + pagina + "/index.html").forward(request, response);
    }
}
