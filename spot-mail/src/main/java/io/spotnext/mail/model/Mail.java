package io.spotnext.mail.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import io.spotnext.core.infrastructure.annotation.ItemType;
import io.spotnext.core.infrastructure.annotation.Property;
import io.spotnext.core.model.Item;

@ItemType(typeCode = "mail")
public class Mail extends Item {

	private static final long serialVersionUID = 1L;

	@Property
	public String sender;

	@Property
	final public List<String> toRecipients = new ArrayList<>();

	@Property
	final public List<String> ccReceipients = new ArrayList<>();

	@Property
	final public List<String> forwardToReceipients = new ArrayList<>();

	@Property
	public String content;

	@Property
	public DateTime receivedAt;

	@Property
	public DateTime sentAt;
}
