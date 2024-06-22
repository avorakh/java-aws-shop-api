package dev.avorakh.shop.svc

import dev.avorakh.shop.dao.ProductDao
import dev.avorakh.shop.dao.StockDao
import dev.avorakh.shop.function.model.Product
import dev.avorakh.shop.function.model.ProductInputResource
import dev.avorakh.shop.function.model.ProductOutputResource
import dev.avorakh.shop.function.model.Stock
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Function

class DefaultProductServiceTest extends Specification {

    @Shared
    def productId = 'someProductId'
    @Shared
    def productTitle = 'someTitle'
    @Shared
    def productDescription = 'someDescription'
    @Shared
    def productPrice = 12
    @Shared
    def productCount = 10

    @Shared
    ProductDao productDao
    @Shared
    StockDao stockDao

    DefaultProductService service

    def setup() {
        productDao = Mock()
        stockDao = Mock()
        service = new DefaultProductService(productDao, stockDao)
    }

    def 'should successfully create product'() {
        given:
        def input = new ProductInputResource(
                title: productTitle,
                description: productDescription,
                price: productPrice,
                count: productCount
        )
        when:
        def actual = service.create(productId, input)
        then:
        1 * productDao.create({ Product product ->
            product.id == productId
            product.title == productTitle
            product.description == productDescription
            product.price == productPrice
        })
        then:
        1 * stockDao.create({ Stock stock ->
            stock.productId == productId
            stock.count == productCount
        })
        then:
        actual != null
        actual == productId
    }

    def 'should successfully toProductOutputResource'() {
        given:
        def product = new Product(
                id: productId,
                title: productTitle,
                description: productDescription,
                price: productPrice
        )
        def testFunction = new Function<String, Integer>() {

            @Override
            Integer apply(String s) {
                return productCount
            }
        }
        when:
        def actual = service.toOutputResource(product, testFunction)

        then:
        actual != null
        with(actual) {
            it.id == productId
            it.title == productTitle
            it.description == productDescription
            it.price == productPrice
            it.count == productCount
        }
    }

    def "should successfully getAll"() {
        given:
        def product1 = new Product(
                id: productId,
                title: productTitle,
                description: productDescription,
                price: productPrice
        )
        def stock1 = new Stock(productId: productId, count: productCount)

        def id2 = "productId2"
        def title2 = "title2"
        def description2 = 'description2'
        def price2 = 20
        def product2 = new Product(
                id: id2,
                title: title2,
                description: description2,
                price: price2
        )

        and:
        def expectedOutput1 = new ProductOutputResource(
                id: productId,
                title: productTitle,
                description: productDescription,
                price: productPrice,
                count: productCount
        )
        def expectedOutput2 = new ProductOutputResource(
                id: id2,
                title: title2,
                description: description2,
                price: price2,
                count: 0
        )
        when:
        def actual = service.getAll()
        then:
        1 * productDao.getAll() >> [product1, product2]
        1 * stockDao.getAll() >> [stock1]
        then:
        actual != null
        actual.size() == 2
        actual.contains(expectedOutput1)
        actual.contains(expectedOutput2)
    }
}
