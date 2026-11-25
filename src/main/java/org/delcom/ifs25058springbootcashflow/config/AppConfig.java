package org.delcom.ifs25058springbootcashflow.config;

import org.delcom.ifs25058springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25058springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25058springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25058springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25058springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25058springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25058springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25058springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25058springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25058springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25058springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25058springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25058springbootcashflow.usecase.UserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

/**
 * Composition root Spring: merakit dependency antar-layer.
 */
@Configuration
public class AppConfig {

    @Bean
    public IUserRepository userRepository(DataSource dataSource) {
        SchemaInitializer.initialize(dataSource);
        return new JdbcUserRepository(dataSource);
    }

    @Bean
    public ITransactionRepository transactionRepository(DataSource dataSource) {
        return new JdbcTransactionRepository(dataSource);
    }

    @Bean
    public FinanceUseCase financeUseCase(ITransactionRepository transactionRepository) {
        return new FinanceUseCase(transactionRepository);
    }

    @Bean
    public IFinanceAiService financeAiService(
            @Value("${OPENAI_ENDPOINT:https://generativelanguage.googleapis.com/v1beta/openai}") String endpoint,
            @Value("${OPENAI_KEY:}") String key,
            @Value("${OPENAI_MODEL:}") String model
    ) {
        return new OpenAiFinanceService(endpoint, key, model);
    }

    @Bean
    public FinanceAiUseCase financeAiUseCase(
            IFinanceAiService financeAiService,
            ITransactionRepository transactionRepository
    ) {
        return new FinanceAiUseCase(financeAiService, transactionRepository);
    }

    @Bean
    public AuthUseCase authUseCase(IUserRepository userRepository) {
        return new AuthUseCase(userRepository);
    }

    @Bean
    public UserUseCase userUseCase(IUserRepository userRepository) {
        return new UserUseCase(userRepository);
    }

    @Bean
    public FinancePresenter financePresenter() {
        return new FinancePresenter();
    }

    @Bean
    public FileStorage fileStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
        return new FileStorage(uploadDir);
    }

    @Bean
    public ApplicationRunner seedRootAdmin(
            AuthUseCase authUseCase,
            @Value("${app.root-username:admin}") String rootUsername,
            @Value("${app.root-password:admin}") String rootPassword
    ) {
        return args -> authUseCase.ensureRootAdmin(rootUsername, rootPassword);
    }

    @Bean
    public WebMvcConfigurer uploadResourceConfigurer(FileStorage fileStorage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String location = fileStorage.root().toUri().toString();
                if (!location.endsWith("/")) {
                    location = location + "/";
                }
                registry.addResourceHandler("/uploads/**").addResourceLocations(location);
            }
        };
    }
}
