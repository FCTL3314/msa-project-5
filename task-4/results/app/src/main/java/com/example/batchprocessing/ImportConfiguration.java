package com.example.batchprocessing;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ImportConfiguration {
    @Bean
    @StepScope
    FlatFileItemReader<Product> reader(@Value("#{jobParameters['sample']}") String sample) {
        String filename = "invalid".equals(sample) ? "invalid.csv" : "product-data.csv";
        return new FlatFileItemReaderBuilder<Product>()
            .name("warehouseCsv").resource(new ClassPathResource(filename))
            .delimited().names("productId", "productSku", "productName", "productAmount", "productData")
            .targetType(Product.class).build();
    }

    @Bean
    @StepScope
    ItemProcessor<Product, Product> enrich(JdbcTemplate database) {
        Map<Long, String> loyalty = new HashMap<>();
        database.query("select productsku, loyalitydata from loyality_data",
            (org.springframework.jdbc.core.RowCallbackHandler) row ->
                loyalty.put(row.getLong(1), row.getString(2)));
        return item -> {
            if (item.productAmount() < 0) {
                throw new IllegalArgumentException("Negative stock for SKU " + item.productSku());
            }
            return new Product(item.productId(), item.productSku(), item.productName(),
                item.productAmount(), loyalty.getOrDefault(item.productSku(), item.productData()));
        };
    }

    @Bean
    JdbcBatchItemWriter<Product> writer(DataSource database) {
        return new JdbcBatchItemWriterBuilder<Product>().dataSource(database).beanMapped()
            .sql("""
                insert into products (productid, productsku, productname, productamount, productdata)
                values (:productId, :productSku, :productName, :productAmount, :productData)
                on conflict (productid) do update set productsku=excluded.productsku,
                    productname=excluded.productname, productamount=excluded.productamount,
                    productdata=excluded.productdata
                """).build();
    }

    @Bean
    Step importRows(JobRepository repository, PlatformTransactionManager transactions,
                    FlatFileItemReader<Product> reader, ItemProcessor<Product, Product> enrich,
                    JdbcBatchItemWriter<Product> writer) {
        return new StepBuilder("importRows", repository).<Product, Product>chunk(100, transactions)
            .reader(reader).processor(enrich).writer(writer).build();
    }

    @Bean
    Job importProducts(JobRepository repository, Step importRows, JobExecutionListener metrics) {
        return new JobBuilder("importProducts", repository)
            .start(importRows).listener(metrics).build();
    }
}
