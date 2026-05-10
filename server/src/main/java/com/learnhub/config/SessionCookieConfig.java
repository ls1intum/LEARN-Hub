package com.learnhub.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionCookieConfig {

	@Bean
	public CookieSerializer cookieSerializer(
			@Value("${server.servlet.session.cookie.name:LEARNHUBSESSION}") String cookieName,
			@Value("${server.servlet.session.cookie.http-only:true}") boolean httpOnly,
			@Value("${server.servlet.session.cookie.secure:true}") boolean secure,
			@Value("${server.servlet.session.cookie.same-site:lax}") String sameSite) {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName(cookieName);
		serializer.setUseHttpOnlyCookie(httpOnly);
		serializer.setUseSecureCookie(secure);
		serializer.setSameSite(sameSite);
		serializer.setRememberMeRequestAttribute(SpringSessionRememberMeServices.REMEMBER_ME_LOGIN_ATTR);
		return serializer;
	}

	@Bean
	public SpringSessionRememberMeServices rememberMeServices(
			@Value("${server.servlet.session.timeout:7d}") Duration sessionTimeout,
			@Value("${app.auth.always-remember:true}") boolean alwaysRemember) {
		SpringSessionRememberMeServices rememberMeServices = new SpringSessionRememberMeServices();
		rememberMeServices.setAlwaysRemember(alwaysRemember);
		rememberMeServices.setValiditySeconds(Math.toIntExact(sessionTimeout.getSeconds()));
		return rememberMeServices;
	}
}
