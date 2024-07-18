package com.library.steps;


import com.library.pages.BasePage;
import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.ConfigurationReader;
import com.library.utility.DB_Util;
import com.library.utility.LibraryAPI_Util;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;


import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class APIStepDefs extends BasePage{

    RequestSpecification givenPart= RestAssured.given().log().uri();
    Response response;
    ValidatableResponse thenPart;

    LoginPage loginPage = new LoginPage();
    BookPage bookPage = new BookPage();

    Map<String,Object> newPayload;

    String apiUserPassword;
    String apiUserEmail;
    String apiFullName;

    String actualId;


    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {
        givenPart.header("x-library-token", LibraryAPI_Util.getToken(userType));
    }
    @Given("Accept header is {string}")
    public void accept_header_is(String acceptHeader) {
        givenPart.accept(acceptHeader);
    }
    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {
         response = givenPart.when().get(ConfigurationReader.getProperty("library.baseUri") + endpoint);

         thenPart = response.then();

    }
    @Then("status code should be {int}")
    public void status_code_should_be(int expectedStatusCode) {
        // OPT 1
        thenPart.statusCode(expectedStatusCode);

        // OPT 2
        assertEquals(expectedStatusCode,response.statusCode());

    }
    @Then("Response Content type is {string}")
    public void response_content_type_is(String contentType) {
        // OPT 1
        thenPart.contentType(contentType);

        // OPT 2
        assertEquals(contentType,response.contentType());
    }
    @Then("Each {string} field should not be null")
    public void each_field_should_not_be_null(String path) {
        thenPart.body(path, everyItem(notNullValue()));

        /*
        JsonPath jp = thenPart.extract().jsonPath();
        List<Object> allData = jp.getList(path);
        for (Object eachData : allData) {
            Assert.assertNotNull(eachData);
        }
         */


    }

    String actualPathParam;
    @Given("Path param is {string}")
    public void path_param_is(String pathParamId) {

        givenPart.pathParam("id",pathParamId);
        actualPathParam=pathParamId;
    }

    @Then("{string} field should be same with path param")
    public void field_should_be_same_with_path_param(String path) {

        thenPart.body(path,equalTo(actualPathParam));

    }
    @Then("following fields should not be null")
    public void following_fields_should_not_be_null(List<String> expectedFields) {

        for (String eachField : expectedFields) {
            thenPart.body(eachField,is(notNullValue()));
        }

    }

    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String reqContentType) {

        givenPart.contentType(reqContentType);

    }
    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String dataType) {

        newPayload = LibraryAPI_Util.generatingRandomData(dataType);
        System.out.println("newBook = " + newPayload);
        givenPart.formParams(newPayload);


    }
    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {

        response = givenPart.when().post(ConfigurationReader.getProperty("library.baseUri") + endpoint);
         thenPart = response.then();

    }
    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String actualMessage, String expectedMessage) {

        thenPart.body(actualMessage,equalTo(expectedMessage));

    }



    @Then("{string} field should not be null")
    public void field_should_not_be_null(String bookId) {

        thenPart.body(bookId,notNullValue());
        JsonPath jp = thenPart.extract().jsonPath();
            actualId = jp.getString(bookId);


        System.out.println("actualId = " + actualId);


    }

    @Given("I logged in Library UI as {string}")
    public void i_logged_in_library_ui_as(String userType) {

        loginPage.login(userType);

    }
    @Given("I navigate to {string} page")
    public void i_navigate_to_page(String booksPage) {

        navigateModule(booksPage);

    }
    @Then("UI, Database and API created book information must match")
    public void ui_database_and_api_created_book_information_must_match() {

        //API
        String apiBookName = newPayload.get("name").toString();
        String apiBookYear = newPayload.get("year").toString();
        String apiBookAuthor = newPayload.get("author").toString();


        //DB
        String query = "select * from books\n" +
                "where id = "+ actualId;
        DB_Util.runQuery(query);
        String dbBookName = DB_Util.getCellValue(1, "name");
        String dbBookYear = DB_Util.getCellValue(1, "year");
        String dbBookAuthor = DB_Util.getCellValue(1, "author");
        System.out.println("name DB = " + dbBookName);

        //UI
        BrowserUtil.waitForVisibility(bookPage.search,4);
        bookPage.search.sendKeys(apiBookAuthor);
        BrowserUtil.waitFor(3);


        //List<String> uiBookInfo = new LinkedList<>();
//        String uiBookInfo;
//        for (WebElement eachRow : bookPage.allRows) {
//            uiBookInfo = eachRow.getText();
//        }
        String uiName = bookPage.bookName.getText();
        String uiBookAuthor = bookPage.author.getText();
        String uiBookYear = bookPage.year.getText();
        //API vs DB
        assertEquals(apiBookName,dbBookName);
        assertEquals(apiBookYear,dbBookYear);
        assertEquals(apiBookAuthor,dbBookAuthor);

        //API vs UI
        assertEquals(apiBookName,uiName);
        assertEquals(apiBookYear,uiBookYear);
        assertEquals(apiBookAuthor,uiBookAuthor);

    }
    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {

        String query = "select * from users\n" +
                "where id = "+ actualId;
        DB_Util.runQuery(query);
        String dbFullName = DB_Util.getCellValue(1, "full_name");
        String dbEmail = DB_Util.getCellValue(1, "email");
        String dbAddress = DB_Util.getCellValue(1, "address");

        apiFullName = newPayload.get("full_name").toString();
        apiUserEmail = newPayload.get("email").toString();
        String apiAddress = newPayload.get("address").toString();
        apiUserPassword = newPayload.get("password").toString();
        System.out.println("apiPassword = " + apiUserPassword);

        assertEquals(apiFullName,dbFullName);
        assertEquals(apiUserEmail,dbEmail);
        assertEquals(apiAddress,dbAddress);


    }
    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {

        loginPage.login(apiUserEmail,apiUserPassword);


    }
    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {


        BrowserUtil.waitForVisibility(bookPage.accountHolderName,10);
        assertEquals(apiFullName,bookPage.accountHolderName.getText());


    }
    String token;
    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {

        givenPart.formParams(email,password);
        token = LibraryAPI_Util.getToken(email, password);
        System.out.println("token = " + token);

    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {

        givenPart.formParam("token",token);

    }

}
