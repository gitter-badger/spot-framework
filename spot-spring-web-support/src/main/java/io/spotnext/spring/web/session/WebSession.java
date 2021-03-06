package io.spotnext.spring.web.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import io.spotnext.core.infrastructure.http.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
public class WebSession extends Session implements HttpSession {

	protected ServletContext servletContext;
	protected HttpSessionContext sessionContext;

	public WebSession(final String id) {
		super(id);
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public HttpSessionContext getSessionContext() {
		return sessionContext;
	}

	@Override
	public Object getValue(final String key) {
		return getAttribute(key);
	}

	@Override
	public String[] getValueNames() {
		return attributes.keySet().toArray(new String[0]);
	}

	@Override
	public void putValue(final String key, final Object value) {
		setAttribute(key, value);
	}

	@Override
	public void removeValue(final String key) {
		removeAttribute(key);
	}

	@Override
	public boolean isNew() {
		// TODO Auto-generated method stub
		return false;
	}

}
