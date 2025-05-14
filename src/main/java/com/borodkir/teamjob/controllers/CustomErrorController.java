package com.borodkir.teamjob.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get error details
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object error = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        // Add details to model
        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("status", status);
        model.addAttribute("error", error != null ? error.getClass().getSimpleName() : "");
        model.addAttribute("message", message);
        model.addAttribute("path", path);

        // Include stack trace if available
        if (error instanceof Exception) {
            model.addAttribute("trace", ExceptionUtils.getStackTrace((Exception) error));
        }

        return "error-debug";
    }
}