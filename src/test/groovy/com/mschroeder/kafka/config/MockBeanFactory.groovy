package com.mschroeder.kafka.config

import com.mschroeder.kafka.service.ImportantDataService
import com.mschroeder.kafka.service.ImportantDataServiceImpl
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import spock.mock.DetachedMockFactory

@TestConfiguration
class MockBeanFactory {
    def mockFactory = new DetachedMockFactory()

    @Bean
    @Primary
    ImportantDataService importantDataServiceMock() {
        ImportantDataService mock = mockFactory.Mock(ImportantDataService)

        return mock
    }
}
