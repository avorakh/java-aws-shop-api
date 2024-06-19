package dev.avorakh.shop.svc

import dev.avorakh.shop.dao.ProductDao
import dev.avorakh.shop.dao.StockDao
import dev.avorakh.shop.function.model.Product
import dev.avorakh.shop.function.model.ProductInputResource
import dev.avorakh.shop.function.model.Stock
import spock.lang.Shared
import spock.lang.Specification

class DefaultProductServiceTest extends Specification {

    @Shared
    def productId = 'someProductId'
    @Shared
    def title = 'someTitle'
    @Shared
    def description = 'someDescription'
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
                title: title,
                description: description,
                price: productPrice,
                count: productCount
        )
        when:
        def actual = service.create(productId, input)
        then:
        1 * productDao.create({ Product product ->
            product.id == productId
            product.title == title
            product.description == description
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
}
