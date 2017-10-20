package at.spot.core.persistence.service.impl.hibernate;

import javax.annotation.Resource;

import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

import at.spot.core.infrastructure.exception.UnknownTypeException;
import at.spot.core.infrastructure.service.TypeService;
import at.spot.core.infrastructure.service.impl.AbstractService;
import at.spot.core.infrastructure.support.ItemTypeDefinition;

public class TypeServicePersistenceUnitPostProcessor extends AbstractService implements PersistenceUnitPostProcessor {

	@Resource
	protected TypeService typeService;

	@Override
	public void postProcessPersistenceUnitInfo(final MutablePersistenceUnitInfo pui) {
		try {
			for (final ItemTypeDefinition def : typeService.getItemTypeDefinitions().values()) {
				loggingService.debug(String.format("Register item type JPA entity %s", def.getTypeClass()));
				pui.addManagedClassName(def.getTypeClass());
			}
		} catch (final UnknownTypeException e) {
			throw new BeanCreationException("Could not register Item type JPA entity.", e);
		}
	}

	/**
	 * Registers item types to the hibernate {@link Configuration}.
	 */
	public Configuration buildHibernatePersistenceInfo() {
		Configuration cfg = new Configuration();

		try {
			for (final ItemTypeDefinition def : typeService.getItemTypeDefinitions().values()) {
				loggingService.debug(String.format("Register item type Hibernate entity %s", def.getTypeClass()));
				Class<?> typeClass = typeService.getType(def.getTypeCode());

				cfg.addAnnotatedClass(typeClass);
			}
		} catch (final UnknownTypeException e) {
			throw new BeanCreationException("Could not register Item type JPA entity.", e);
		}

		return cfg;
	}
}
