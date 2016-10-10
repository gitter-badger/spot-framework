package at.spot.core.infrastructure.service.impl;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.FieldSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import at.spot.core.data.model.Item;
import at.spot.core.infrastructure.annotation.model.ItemType;
import at.spot.core.infrastructure.annotation.model.Property;
import at.spot.core.infrastructure.service.LoggingService;
import at.spot.core.infrastructure.service.TypeService;
import at.spot.core.infrastructure.type.ItemPropertyDefinition;

/**
 * Provides functionality to manage the typesystem.
 *
 */
@Service
public class DefaultTypeService extends AbstractService implements TypeService {

	@Resource(name = "itemTypePackageScanPaths")
	protected List<String> itemTypePackageScanPaths;

	@Autowired
	protected LoggingService loggingService;

	@Override
	public <A extends Annotation> boolean hasAnnotation(JoinPoint joinPoint, Class<A> annotation) {
		return getAnnotation(joinPoint, annotation) != null;
	}

	public <A extends Annotation> A getAnnotation(JoinPoint joinPoint, Class<A> annotation) {
		A ret = null;

		Signature sig = joinPoint.getSignature();

		if (sig instanceof MethodSignature) {
			final MethodSignature methodSignature = (MethodSignature) sig;
			Method method = methodSignature.getMethod();

			if (method.getDeclaringClass().isInterface()) {
				try {
					method = joinPoint.getTarget().getClass().getMethod(methodSignature.getName());
				} catch (NoSuchMethodException | SecurityException e) {
					//
				}
			}

			ret = AnnotationUtils.findAnnotation(method, annotation);
		} else {
			FieldSignature fieldSignature = (FieldSignature) sig;

			ret = fieldSignature.getField().getAnnotation(annotation);
		}

		return ret;

	}

	@Override
	public List<Class<?>> getItemConcreteTypes(List<String> packages) {
		List<Class<?>> itemTypes = new ArrayList<>();

		for (String pack : packages) {
			Reflections reflections = new Reflections(pack);
			Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ItemType.class);

			for (Class<?> clazz : annotated) {
				if (clazz.isAnnotationPresent(ItemType.class)) {
					itemTypes.add(clazz);
				}
			}
		}

		return itemTypes;
	}

	@Override
	public List<Class<? extends Item>> getAvailableTypes() {
		Map<String, Item> types = getApplicationContext().getBeansOfType(Item.class);

		List<Class<? extends Item>> allTypes = new ArrayList<>(types.keySet().size());

		for (Item i : types.values()) {
			if (hasAnnotation(i.getClass(), ItemType.class)) {
				allTypes.add(i.getClass());
			}
		}

		return allTypes;
	}

	@Override
	public void registerTypes() {

		for (Class<?> clazz : getItemConcreteTypes(itemTypePackageScanPaths)) {
			if (clazz.isAnnotationPresent(ItemType.class)) {
				registerType(clazz, "prototype");
			}
		}
	}

	protected void registerType(Class<?> type, String scope) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(type);
		beanDefinition.setLazyInit(false);
		beanDefinition.setAbstract(Modifier.isAbstract(type.getModifiers()));
		beanDefinition.setAutowireCandidate(true);
		beanDefinition.setScope(scope);

		String beanName = type.getSimpleName();

		ItemType ann = type.getAnnotation(ItemType.class);

		// use the annotated itemtype name, it should
		if (ann != null && StringUtils.isNotBlank(ann.beanName())) {
			beanName = ann.beanName();
		}

		getBeanFactory().registerBeanDefinition(beanName, beanDefinition);

		loggingService.debug(String.format("Registering type: %s", type.getSimpleName()));
	}

	@Override
	public <A extends Annotation> boolean hasAnnotation(Class<? extends Item> type, Class<A> annotation) {
		return getAnnotation(type, annotation) != null;
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<? extends Item> type, Class<A> annotation) {
		return type.getAnnotation(annotation);
	}

	@Override
	public <A extends Annotation> boolean hasAnnotation(AccessibleObject member, Class<A> annotation) {
		return getAnnotation(member, annotation) != null;
	}

	@Override
	public <A extends Annotation> A getAnnotation(AccessibleObject member, Class<A> annotation) {
		return member.getAnnotation(annotation);
	}

	@Override
	public Map<String, ItemPropertyDefinition> getItemProperties(Class<? extends Item> itemType) {
		Map<String, ItemPropertyDefinition> propertyMembers = new HashMap<>();

		// add all the fields
		for (Field m : itemType.getFields()) {
			Property annotation = getAnnotation(m, Property.class);

			if (annotation != null) {
				ItemPropertyDefinition def = new ItemPropertyDefinition(m.getName(), m.getDeclaringClass().getName(),
						annotation.readable(), annotation.writable(), annotation.initial(), annotation.unique(),
						annotation.itemValueProvider());

				propertyMembers.put(m.getName(), def);
			}
		}

		// add all the getter methods
		for (Method m : itemType.getMethods()) {
			Property annotation = getAnnotation(m, Property.class);

			if (annotation != null && m.getReturnType() != Void.class) {
				String name = m.getName();

				if (StringUtils.startsWithIgnoreCase(name, "get")) {
					name = StringUtils.substring(name, 3);
				} else if (StringUtils.startsWithIgnoreCase(name, "get")) {
					name = StringUtils.substring(name, 2);
				}

				name = Introspector.decapitalize(name);

				ItemPropertyDefinition def = new ItemPropertyDefinition(m.getName(), m.getDeclaringClass().getName(),
						annotation.readable(), annotation.writable(), annotation.initial(), annotation.unique(),
						annotation.itemValueProvider());

				propertyMembers.put(name, def);
			}
		}

		return propertyMembers;
	}

	@Override
	public Class<? extends Item> getType(String typeCode) {
		Class<? extends Item> type = (Class<? extends Item>) getApplicationContext().getBean(typeCode).getClass();

		return type;
	}
}
