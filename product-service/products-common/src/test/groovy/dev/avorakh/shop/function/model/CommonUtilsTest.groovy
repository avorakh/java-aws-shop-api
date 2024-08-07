package dev.avorakh.shop.function.model

import spock.lang.Shared
import spock.lang.Specification

import static dev.avorakh.shop.function.model.CommonUtils.*

class CommonUtilsTest extends Specification {
    @Shared
    def productId = 'productId'
    @Shared
    def productTitle = 'someTitle'
    @Shared
    def productDescription = 'someDescription'
    @Shared
    def productPrice = 12
    @Shared
    def productCount = 10

    def 'should successfully validate input resource'() {
        given:
        def input = new ProductInputResource(
                title: productTitle,
                description: productDescription,
                price: productPrice,
                count: productCount
        )
        when:
        def actual = CommonUtils.validate(input)
        then:
        actual != null
        actual.empty
    }

    def 'should validate ProductInputResource and return 4 error details if all fields are missed'() {
        when:
        def actual = CommonUtils.validate(new ProductInputResource())
        then:
        actual != null
        actual.size() == 4
    }

    def 'should validate the fields and return 1 error details'(
            String aTitle, String aDescription, Integer aPrice, Integer aCount, String expectedMessage
    ) {
        given:
        def input = new ProductInputResource(
                title: aTitle,
                description: aDescription,
                price: aPrice,
                count: aCount
        )
        when:
        def actual = CommonUtils.validate(input)
        then:
        actual != null
        actual.size() == 1
        with(actual[0]) {
            it.errorMessage == expectedMessage
        }
        where:
        aTitle       | aDescription       | aPrice       | aCount       || expectedMessage
        " "          | productDescription | productPrice | productCount || TITLE_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK
        productTitle | ""                 | productPrice | productCount || DESCRIPTION_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK
        productTitle | productDescription | null         | productCount || PRICE_FIELD_SHOULD_BE_PRESENT
        productTitle | productDescription | 0            | productCount || PRICE_FIELD_SHOULD_BE_GREATER_THAN_0
        productTitle | productDescription | -1           | productCount || PRICE_FIELD_SHOULD_BE_GREATER_THAN_0
        productTitle | productDescription | productPrice | null         || COUNT_FIELD_SHOULD_BE_PRESENT
        productTitle | productDescription | productPrice | 0            || COUNT_FIELD_SHOULD_BE_EQUAL_TO_OR_GREATER_THAN_0
        productTitle | productDescription | productPrice | -1           || COUNT_FIELD_SHOULD_BE_EQUAL_TO_OR_GREATER_THAN_0
    }


    def 'should successfully validate ProductOutputResource'() {
        given:
        def input = new ProductOutputResource(
                id: productId,
                title: productTitle,
                description: productDescription,
                price: productPrice,
                count: productCount
        )
        when:
        def actual = CommonUtils.validate(input)
        then:
        actual != null
        actual.empty
    }

    def 'should validate ProductOutputResource and return 4 error details if all fields are missed'() {
        when:
        def actual = CommonUtils.validate(new ProductOutputResource())
        then:
        actual != null
        actual.size() == 5
    }

    def 'should validate the ProductOutputResource fields and return 1 error details'(
            String aId, String aTitle, String aDescription, Integer aPrice, Integer aCount, String expectedMessage
    ) {
        given:
        def input = new ProductOutputResource(
                id: aId,
                title: aTitle,
                description: aDescription,
                price: aPrice,
                count: aCount
        )
        when:
        def actual = CommonUtils.validate(input)
        then:
        actual != null
        actual.size() == 1
        with(actual[0]) {
            it.errorMessage == expectedMessage
        }
        where:
        aId       | aTitle       | aDescription       | aPrice       | aCount       || expectedMessage
        null      | productTitle | productDescription | productPrice | productCount || ID_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK
        productId | " "          | productDescription | productPrice | productCount || TITLE_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK
        productId | productTitle | ""                 | productPrice | productCount || DESCRIPTION_FIELD_SHOULD_BE_PRESENT_OR_NOT_BLANK
        productId | productTitle | productDescription | null         | productCount || PRICE_FIELD_SHOULD_BE_PRESENT
        productId | productTitle | productDescription | 0            | productCount || PRICE_FIELD_SHOULD_BE_GREATER_THAN_0
        productId | productTitle | productDescription | -1           | productCount || PRICE_FIELD_SHOULD_BE_GREATER_THAN_0
        productId | productTitle | productDescription | productPrice | null         || COUNT_FIELD_SHOULD_BE_PRESENT
        productId | productTitle | productDescription | productPrice | 0            || COUNT_FIELD_SHOULD_BE_EQUAL_TO_OR_GREATER_THAN_0
        productId | productTitle | productDescription | productPrice | -1           || COUNT_FIELD_SHOULD_BE_EQUAL_TO_OR_GREATER_THAN_0
    }
}
