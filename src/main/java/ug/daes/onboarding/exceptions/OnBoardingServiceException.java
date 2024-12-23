package ug.daes.onboarding.exceptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import ug.daes.onboarding.config.SentryClientExceptions;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.util.AppUtil;

import java.net.UnknownHostException;
import java.util.Locale;

public class OnBoardingServiceException {

    @Autowired
    MessageSource messageSource;

    @Autowired
    SentryClientExceptions sentryClientExceptions;

    // Helper method to handle exceptions
    public ApiResponse handleExceptionWithStaticMessage(Exception e)
            throws NoSuchMessageException {
        return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH),null);
    }

    public ApiResponse handleExceptionWithStaticMessageWithSentry(Exception e,String suid)
            throws NoSuchMessageException, UnknownHostException {
        // Dynamically get the method and controller name
        String methodName = getCurrentMethodName();
        String controllerName = getCurrentControllerName();
        captureExceptionInSentry(e,suid,methodName,controllerName);
        return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH),null);
    }

    // Helper to get the current method name
    private static String getCurrentMethodName() {
        return Thread.currentThread().getStackTrace()[3].getMethodName(); // Adjust index for correct context
    }

    // Helper to get the current controller (class) name
    private static String getCurrentControllerName() {
        return Thread.currentThread().getStackTrace()[3].getClassName(); // Adjust index for correct context
    }

    // Helper method to handle exceptions
    public ApiResponse handleExceptionWithDynamicMessage(String msg)
            throws NoSuchMessageException {
        return AppUtil.createApiResponse(false,messageSource.getMessage(msg, null, Locale.ENGLISH),null);
    }



    public ApiResponse handleErrorRestTemplateResponse(int statusCode) {
        switch (statusCode) {
            case 500:
                return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.internal.server.error", null, Locale.ENGLISH), statusCode);
            case 400:
                return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.bad.request", null, Locale.ENGLISH), null);
            case 401:
                return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.unauthorized", null, Locale.ENGLISH), null);
            case 403:
                return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.forbidden", null, Locale.ENGLISH), null);
            case 408:
                return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.request.timeout", null, Locale.ENGLISH), null);
            default:
                return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }

    // Separate method for handling Sentry exception capturing
    public void captureExceptionInSentry(Exception e, String suid,String methodName,String controllerName ) throws UnknownHostException {
            sentryClientExceptions.captureTags(suid, null, methodName, controllerName);
            sentryClientExceptions.captureExceptions(e);
    }
}
