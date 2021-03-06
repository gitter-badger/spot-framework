package io.spotnext.core.infrastructure.service;

import java.util.List;
import java.util.Set;

import io.spotnext.core.infrastructure.exception.CannotCreateUserException;
import io.spotnext.core.security.service.AuthenticationService;
import io.spotnext.itemtype.core.user.User;
import io.spotnext.itemtype.core.user.UserGroup;

public interface UserService<U extends User, G extends UserGroup> {

	/**
	 * Creates an {@link User} object, only setting the given userId.
	 * 
	 * @param userId
	 * @return the newly created {@link User}.
	 * @throws CannotCreateUserException
	 */
	U createUser(Class<U> type, String userId) throws CannotCreateUserException;

	/**
	 * Creates an {@link User} object, only setting the given userId and
	 * password (using {@link AuthenticationService#setPassword(User, String)}).
	 * 
	 * @param userId
	 * @return the newly created {@link User}.
	 * @throws CannotCreateUserException
	 */
	U createUser(Class<U> type, String userId, String password) throws CannotCreateUserException;

	/**
	 * @param uid
	 *            the user's uid
	 * @return the given user or null, if the user is not found.
	 */
	U getUser(String uid);

	/**
	 * @return all available {@link User}s (even subtypes).
	 */
	List<U> getAllUsers();

	/**
	 * @return all available {@link UserGroup}s (even subtypes).
	 */
	List<G> getAllUserGroups();

	/**
	 * Get user group with the given id.
	 * 
	 * @param uid
	 * @return the {@link UserGroup}
	 */
	G getUserGroup(String uid);

	Set<G> getAllGroupsOfUser(String uid);

	boolean isUserInGroup(String userUid, String groupUid);

	/**
	 * Returns the current user in the session.
	 * 
	 */
	U getCurrentUser();

	/**
	 * Sets the given user as the current session user.
	 * 
	 * @param user
	 */
	void setCurrentUser(U user);

	/**
	 * Returns true if there is no user registered in the current session.
	 */
	boolean isCurrentUserAnonymous();

}
