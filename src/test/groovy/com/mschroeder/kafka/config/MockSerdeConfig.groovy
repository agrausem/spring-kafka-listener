package com.mschroeder.kafka.config

import com.mschroeder.kafka.avro.AvroSampleData
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@TestConfiguration
class MockSerdeConfig {
	private KafkaProperties props
	MockSerdeConfig(KafkaProperties kafkaProperties) {
		props = kafkaProperties
	}

	/**
	 * Mock schema registry bean used by Kafka Avro Serde since
	 * the @EmbeddedKafka setup doesn't include a schema registry.
	 * @return MockSchemaRegistryClient instance
	 */
	@Bean
	@Primary
	MockSchemaRegistryClient schemaRegistryClient() {
		new MockSchemaRegistryClient()
	}

	/**
	 * KafkaAvroSerializer that uses a MockSchemaRegistryClient
	 * @return KafkaAvroSerializer instance
	 */
	@Bean
	@Primary
	KafkaAvroSerializer kafkaAvroSerializer() {
		new KafkaAvroSerializer(schemaRegistryClient())
	}

	/**
	 * KafkaAvroDeserializer that uses a MockSchemaRegistryClient
	 * @return KafkaAvroDeserializer instance
	 */
	@Bean
	@Primary
	KafkaAvroDeserializer kafkaAvroDeserializer() {
		new KafkaAvroDeserializer(schemaRegistryClient(), props.buildConsumerProperties())
	}

	/**
	 * Configures the kafka producer factory to use the overridden
	 * KafkaAvroDeserializer so that the MockSchemaRegistryClient
	 * is used rather than trying to reach out via HTTP to a schema registry
	 * @param props KafkaProperties configured in application.yml
	 * @return DefaultKafkaProducerFactory instance
	 */
	@Bean
	@Primary
	ProducerFactory<String, AvroSampleData> producerFactory() {
		new DefaultKafkaProducerFactory(
				props.buildProducerProperties(),
				new StringSerializer(),
				kafkaAvroSerializer()
		)
	}

	@Bean
	@Primary
	KafkaTemplate<String, AvroSampleData> avroKafkaTemplate() {
		return new KafkaTemplate<>(producerFactory())
	}

	/**
	 * Configures the kafka consumer factory to use the overridden
	 * KafkaAvroSerializer so that the MockSchemaRegistryClient
	 * is used rather than trying to reach out via HTTP to a schema registry
	 * @param props KafkaProperties configured in application.yml
	 * @return DefaultKafkaConsumerFactory instance
	 */
	@Bean
	@Primary
	DefaultKafkaConsumerFactory<String, KafkaAvroDeserializer> consumerFactory() {
		new DefaultKafkaConsumerFactory(
				props.buildConsumerProperties(),
				new StringDeserializer(),
				kafkaAvroDeserializer()
		)
	}

	/**
	 * Configure the ListenerContainerFactory to use the overridden
	 * consumer factory so that the MockSchemaRegistryClient is used
	 * under the covers by all consumers when deserializing Avro data.
	 * @return ConcurrentKafkaListenerContainerFactory instance
	 */
	@Primary
	@Bean("avroListenerFactory")
	ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory()
		factory.setConsumerFactory(consumerFactory())
		return factory
	}
}
