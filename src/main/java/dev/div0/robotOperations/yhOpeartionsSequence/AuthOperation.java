package dev.div0.robotOperations.yhOpeartionsSequence;

import dev.div0.application.page.YahooPage;
import dev.div0.events.EventDispatcher;
import dev.div0.robotOperations.*;
import dev.div0.robotOperations.events.OperationEvent;
import dev.div0.robotOperations.operationData.OperationData;
import dev.div0.robotOperations.screen.TakePageScreenshot;
import dev.div0.robotOperations.yhOpeartionsSequence.bidding.events.BiddingResultEvent;
import dev.div0.steps.ElementSearchType;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.seleniumhq.jetty9.util.security.Credential;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class AuthOperation extends BaseOperation {

    private final static String LOADING_LOGIN_PAGE = "LOADING_LOGIN_PAGE";
    private final static String ENTERING_LOGIN = "ENTERING_LOGIN";
    private final static String ENTERING_PASSWORD = "ENTERING_PASSWORD";
    private final static String CHECKING_ERROR_CONTENT_PRESENTS = "CHECKING_ERROR_CONTENT_PRESENTS";
    private final static String LOGIN_ERROR = "LOGIN_ERROR";

    private String state = ENTERING_LOGIN;
    private int currentTryout = 0;
    private int maxTryouts = 5;
    private int[]timeouts = {1,7};

    private String login = "";
    private String pass = "";

    private DetectPageHasElementOperation detectAlreadyLoggedInOperation;
    private DetectPageHasElementOperation detectPageHasUserDataErrorOperation;

    public AuthOperation() {
        super();
        log("Am AuthOperation");
    }

    @Override
    public void setOperationData(OperationData operationData) {
        super.setOperationData(operationData);
        log("AuthOperation setOperationData "+operationData);
        log("payload = "+operationData.getPayload());

        JSONObject json;

        JSONParser parser = new JSONParser();

        try {
            json = (JSONObject) parser.parse(new StringReader(operationData.getPayload().toString()));
            login = json.get("login").toString();
            pass = json.get("pass").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean execute() throws OperationException {
        log("auth execute. currentTryout: "+ currentTryout);

        log("loading login page...");
        loadLoginPage();

        // detect is captcha
        boolean isCaptchaPage = detectIsCaptcha();

        if(isCaptchaPage){
            OperationEvent operationEvent = new OperationEvent(OperationEvent.CAPTCHA_PAGE_ERROR);
            EventDispatcher.getInstance().dispatchEvent(operationEvent);
            return true;
        }

        // detect if logged in previously
        boolean isLogged = detectIsLogged();


        isLogged = !detectAlreadyLoggedInOperation.hasCssClass("dispNone");

        log("isLogged = "+isLogged);

        boolean pageHasLoginErrorContent = false;
        boolean authResult = false;
        boolean isLoginIncorrect = false;

        if(!isLogged){
            boolean enterLoginOperationComplete = enterLoginOperation();
            log("complete entering login with result: "+enterLoginOperationComplete);
            // click button
            boolean clickNextButtonResult = clickNextButton();
            log("complete clickNextButton with result: "+clickNextButtonResult);

            // detect is login correct
            detectIsUserDataNotCorrect();
            isLoginIncorrect = detectPageHasUserDataErrorOperation.isElementVisible();

            if(!isLoginIncorrect){
                log("checking has error page content ...");
                pageHasLoginErrorContent = detectPageHasLoginErrorContent();
                log("complete checking has error page content with result "+pageHasLoginErrorContent);
            }
        }

        if(isLoginIncorrect == true){
            OperationEvent operationEvent = new OperationEvent(OperationEvent.LOGIN_INCORRECT);
            EventDispatcher.getInstance().dispatchEvent(operationEvent);
            return true;
        }
        else{
            if(pageHasLoginErrorContent){
                authResult = externalLogin();
            }
            else{
                authResult = normalLogin();
            }

            if(authResult!=true){
                currentTryout++;
                if(currentTryout < maxTryouts){
                    return execute();
                }
                else{
                    log("currentTryout out - negative result. sorry");
                    return false;
                }
            }
            else{
                return true;
            }
        }
    }

    private boolean detectIsCaptcha() throws OperationException{
        log("detectIsCaptcha()");
        detectPageHasUserDataErrorOperation = new DetectPageHasElementOperation();
        detectPageHasUserDataErrorOperation.setWebDriver(webDriver);
        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_XPATH);
        internalOperationData.setElementSearchData("//*[@id=\"captchaV5Answer\"]");

        detectPageHasUserDataErrorOperation.setOperationData(internalOperationData);
        return detectPageHasUserDataErrorOperation.execute();
    }

    private boolean detectIsUserDataNotCorrect() throws OperationException {
        detectPageHasUserDataErrorOperation = new DetectPageHasElementOperation();
        detectPageHasUserDataErrorOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_ID);
        internalOperationData.setElementSearchData("errMsg");

        detectPageHasUserDataErrorOperation.setOperationData(internalOperationData);
        return detectPageHasUserDataErrorOperation.execute();
    }

    private void createScreenshot() throws OperationException {
        TakePageScreenshot takePageScreenshot = new TakePageScreenshot();
        takePageScreenshot.setWebDriver(webDriver);
        takePageScreenshot.execute();
    }

    private boolean detectIsLogged() throws OperationException {
        log("detectIsLogged()");
        createScreenshot();

        detectAlreadyLoggedInOperation = new DetectPageHasElementOperation();
        detectAlreadyLoggedInOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_ID);
        internalOperationData.setElementSearchData("inpAuthMethod");

        detectAlreadyLoggedInOperation.setOperationData(internalOperationData);
        return detectAlreadyLoggedInOperation.execute();
    }

    private boolean clickNextButton() throws OperationException {
        ClickElementOperation clickElementOperation = new ClickElementOperation();
        clickElementOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_ID);
        internalOperationData.setElementSearchData(YahooPage.auth_loginBtnId);

        clickElementOperation.setOperationData(internalOperationData);
        return clickElementOperation.execute();
    }

    private boolean externalLogin() throws OperationException {
        log("external login");
        ClickLinkOperation clickErrorLoginLinkOperation = new ClickLinkOperation();
        clickErrorLoginLinkOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_XPATH);
        internalOperationData.setElementSearchData(YahooPage.auth_loginErrorBoxXPath);

        clickErrorLoginLinkOperation.setOperationData(internalOperationData);

        return clickErrorLoginLinkOperation.execute();
    }
    private boolean normalLogin() throws OperationException {
        log("normal login");
        boolean enterPasswordOperation = enterPassword();
        boolean clickSubmitButtonOperation = clickSubmit();
        boolean loginComplete = false;

        // detect is password correct
        detectIsUserDataNotCorrect();
        boolean isPasswordIncorrect = detectPageHasUserDataErrorOperation.isElementVisible();

        if(isPasswordIncorrect){
            OperationEvent operationEvent = new OperationEvent(OperationEvent.PASSWORD_INCORRECT);
            EventDispatcher.getInstance().dispatchEvent(operationEvent);
            return true;
        }
        else{
            loginComplete = checkLoginComplete();
        }

        return loginComplete;
    }

    private boolean checkLoginComplete() throws OperationException {
        DetectPageHasTextOperation detectPageHasLoginText = new DetectPageHasElementOperation();
        detectPageHasLoginText.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_XPATH);
        internalOperationData.setElementSearchData("//*[contains(text(), '"+login+"')]");

        detectPageHasLoginText.setOperationData(internalOperationData);
        return detectPageHasLoginText.execute();
    }

    private boolean clickSubmit() throws OperationException {
        ClickElementOperation clickElementOperation = new ClickElementOperation();
        clickElementOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_ID);
        internalOperationData.setElementSearchData(YahooPage.auth_submitBtnId);

        clickElementOperation.setOperationData(internalOperationData);
        return clickElementOperation.execute();
    }

    private boolean enterPassword() throws OperationException {
        log("entering password "+pass);
        state = ENTERING_PASSWORD;
        SetTextOperation authSetPasswordOperation = new SetTextOperation();
        authSetPasswordOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_XPATH);
        internalOperationData.setElementSearchData(YahooPage.auth_passwordInputXPath);
        internalOperationData.setPayload(pass);

        authSetPasswordOperation.setOperationData(internalOperationData);

        return authSetPasswordOperation.execute();
    }

    // enter loginOperation
    private boolean enterLoginOperation() throws OperationException {
        log("entering login "+login);
        state = ENTERING_LOGIN;

        SetTextOperation authSetLoginOperation = new SetTextOperation();
        authSetLoginOperation.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_ID);
        internalOperationData.setElementSearchData(YahooPage.auth_loginInputId);
        internalOperationData.setPayload(login);

        authSetLoginOperation.setOperationData(internalOperationData);

        return authSetLoginOperation.execute();
    }

    private boolean detectPageHasLoginErrorContent() throws OperationException {
        state = CHECKING_ERROR_CONTENT_PRESENTS;
        DetectPageHasElementOperation detectPageHasElement = new DetectPageHasElementOperation();
        detectPageHasElement.setWebDriver(webDriver);

        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchType(ElementSearchType.BY_XPATH);
        internalOperationData.setElementSearchData(YahooPage.auth_loginErrorBoxXPath);

        detectPageHasElement.setOperationData(internalOperationData);

        return detectPageHasElement.execute();
    }

    private boolean loadLoginPage() throws OperationException {
        log("loading auth page "+YahooPage.auth_pageUrl);
        state = LOADING_LOGIN_PAGE;
        OpenUrlOperation operation = new OpenUrlOperation();
        operation.setWebDriver(webDriver);
        OperationData internalOperationData = new OperationData();
        internalOperationData.setElementSearchData(YahooPage.auth_pageUrl);

        operation.setOperationData(internalOperationData);

        return  operation.execute();
    }
}
