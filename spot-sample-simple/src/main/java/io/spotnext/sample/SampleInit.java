package io.spotnext.sample;

import javax.servlet.http.HttpSessionListener;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;

import io.spotnext.core.infrastructure.exception.ModuleInitializationException;
import io.spotnext.core.infrastructure.support.init.Bootstrap;
import io.spotnext.core.infrastructure.support.init.ModuleInit;
import io.spotnext.spring.web.session.WebSessionListener;

@SpringBootApplication(scanBasePackages = { "io.spotnext.sample" })
public class SampleInit extends ModuleInit {

	@Override
	protected void initialize() throws ModuleInitializationException {
		//
	}

	@Override
	protected void importInitialData() throws ModuleInitializationException {
		super.importInitialData();
	}

	@Override
	protected void importSampleData() throws ModuleInitializationException {
		super.importSampleData();
	}

	@Bean
	public ServletListenerRegistrationBean<HttpSessionListener> sessionListener() {
		return new ServletListenerRegistrationBean<HttpSessionListener>(new WebSessionListener());
	}

	public static void main(final String[] args) throws Exception {
		Bootstrap.bootstrap(SampleInit.class, new String[] { "io.spotnext.sample.types" }, args).run();
	}

}
