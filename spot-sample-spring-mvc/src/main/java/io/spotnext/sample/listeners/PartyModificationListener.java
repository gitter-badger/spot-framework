package io.spotnext.sample.listeners;

import javax.annotation.Resource;

import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import io.spotnext.core.infrastructure.event.ItemModificationEvent;
import io.spotnext.itemtype.core.user.User;
import io.spotnext.sample.service.EmailService;
import io.spotnext.sample.types.itemtypes.Party;

@Service
public class PartyModificationListener {

	@Resource
	private EmailService emailService;

	@EventListener(condition = "#event.item.fixed == true && #event.modificationType.name() == 'SAVE'")
	public void handleEvent(final ItemModificationEvent<Party> event) {
		final Party fixedParty = event.getItem();

		// build email for every guest
		for (final User guest : fixedParty.getInvitedGuests()) {
			final Email email = EmailBuilder.startingBlank().to(guest.getShortName(), guest.getId())
					.from("Party Service", "service@party.at")
					.withSubject(fixedParty.getTitle() + " is on " + fixedParty.getDate())
					.withHTMLText("Let's get this party started!").buildEmail();

			emailService.send(email);
		}
	}
}
