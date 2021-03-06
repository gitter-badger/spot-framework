package io.spotnext.maven.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import de.hunsicker.jalopy.Jalopy;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.spotnext.core.infrastructure.annotation.Accessor;
import io.spotnext.core.infrastructure.annotation.Relation;
import io.spotnext.core.infrastructure.maven.TypeDefinitions;
import io.spotnext.core.infrastructure.maven.xml.Argument;
import io.spotnext.core.infrastructure.maven.xml.AtomicType;
import io.spotnext.core.infrastructure.maven.xml.BaseComplexType.Interfaces;
import io.spotnext.core.infrastructure.maven.xml.BaseType;
import io.spotnext.core.infrastructure.maven.xml.BeanType;
import io.spotnext.core.infrastructure.maven.xml.CollectionType;
import io.spotnext.core.infrastructure.maven.xml.CollectionsType;
import io.spotnext.core.infrastructure.maven.xml.EnumType;
import io.spotnext.core.infrastructure.maven.xml.EnumValue;
import io.spotnext.core.infrastructure.maven.xml.Interface;
import io.spotnext.core.infrastructure.maven.xml.ItemType;
import io.spotnext.core.infrastructure.maven.xml.MapType;
import io.spotnext.core.infrastructure.maven.xml.Property;
import io.spotnext.core.infrastructure.maven.xml.RelationNode;
import io.spotnext.core.infrastructure.maven.xml.RelationType;
import io.spotnext.core.infrastructure.maven.xml.RelationshipCardinality;
import io.spotnext.core.infrastructure.maven.xml.Validator;
import io.spotnext.core.infrastructure.maven.xml.ValidatorArgument;
import io.spotnext.core.infrastructure.maven.xml.Validators;
import io.spotnext.core.infrastructure.type.AccessorType;
import io.spotnext.core.infrastructure.type.RelationCollectionType;
import io.spotnext.core.infrastructure.type.RelationNodeType;
import io.spotnext.core.support.util.ClassUtil;
import io.spotnext.core.support.util.MiscUtil;
import io.spotnext.core.types.Bean;
import io.spotnext.core.types.Item;
import io.spotnext.core.types.Localizable;
import io.spotnext.maven.exception.IllegalItemTypeDefinitionException;
import io.spotnext.maven.util.ItemTypeDefinitionUtil;
import io.spotnext.maven.velocity.JavaMemberModifier;
import io.spotnext.maven.velocity.TemplateFile;
import io.spotnext.maven.velocity.Visibility;
import io.spotnext.maven.velocity.type.AbstractComplexJavaType;
import io.spotnext.maven.velocity.type.AbstractJavaObject;
import io.spotnext.maven.velocity.type.annotation.JavaAnnotation;
import io.spotnext.maven.velocity.type.annotation.JavaValueType;
import io.spotnext.maven.velocity.type.base.JavaClass;
import io.spotnext.maven.velocity.type.base.JavaEnum;
import io.spotnext.maven.velocity.type.base.JavaInterface;
import io.spotnext.maven.velocity.type.parts.JavaEnumValue;
import io.spotnext.maven.velocity.type.parts.JavaExpression;
import io.spotnext.maven.velocity.type.parts.JavaField;
import io.spotnext.maven.velocity.type.parts.JavaGenericTypeArgument;
import io.spotnext.maven.velocity.type.parts.JavaMemberType;
import io.spotnext.maven.velocity.type.parts.JavaMethod;
import io.spotnext.maven.velocity.util.VelocityUtil;

/**
 * @description Generates the java source code for the defined item types.
 */
@Mojo(name = "generate-types", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true)
public class GenerateTypesMojo extends AbstractMojo {

	protected Jalopy jalopy = new Jalopy();
	protected VelocityEngine velocityEngine = new VelocityEngine();

	@Parameter(property = "localRepository", defaultValue = "${localRepository}", readonly = true, required = true)
	protected ArtifactRepository localRepository;

	@Parameter(property = "formatSources")
	protected boolean formatSource = true;

	@Parameter(property = "project", defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(property = "basedir", defaultValue = "${project.basedir}", readonly = true, required = true)
	protected String projectBaseDir;

	@Parameter(property = "sourceDirectory", defaultValue = "src/gen/java", readonly = true)
	protected String sourceDirectory;

	@Parameter(property = "resourceDirectory", defaultValue = "src/gen/resources", readonly = true)
	protected String generatedResourcesDirectory;

	protected File targetClassesDirectory = null;
	protected File targetResourcesDirectory = null;

	@Parameter(property = "title")
	protected String title;

	protected ItemTypeDefinitionUtil loader;

	// data
	protected TypeDefinitions typeDefinitions;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Generting item types from XML.");

		initTemplateEngine();
		createPaths();

		// do the actual work
		try {
			loader = new ItemTypeDefinitionUtil(project, localRepository, getLog());
			typeDefinitions = loader.fetchItemTypeDefinitions();
			loader.saveTypeDefinitions(typeDefinitions, getGeneratedResourcesFolder());
			generateTypes();
		} catch (final IllegalItemTypeDefinitionException | IOException e) {
			throw new MojoExecutionException("Could not generate Java source code!", e);
		}
	}

	protected File getGeneratedResourcesFolder() {
		return new File(projectBaseDir + "/" + generatedResourcesDirectory);
	}

	protected void createPaths() {
		targetClassesDirectory = new File(projectBaseDir + "/" + sourceDirectory);
		targetResourcesDirectory = new File(projectBaseDir + "/" + generatedResourcesDirectory);

		if (this.project != null) {
			// add generated sources
			this.project.addCompileSourceRoot(targetClassesDirectory.getAbsolutePath());

			// add generated resources
			final Resource resourceDir = new Resource();
			resourceDir.setDirectory(targetResourcesDirectory.getAbsolutePath());
			this.project.addResource(resourceDir);
		}

		if (!targetClassesDirectory.mkdirs()) {
			if (!targetClassesDirectory.delete()) {
				getLog().warn("Could not delete target dir.");
			}
		}
	}

	protected void initTemplateEngine() {
		velocityEngine.setProperty("resource.loader", "class");
		velocityEngine.setProperty("class.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		velocityEngine.init();
	}

	protected void generateTypes() throws MojoExecutionException {
		final List<AbstractComplexJavaType> types = new ArrayList<>();

		types.addAll(generateEnums());
		types.addAll(generateBeans());
		types.addAll(generateItemTypes());

		try {// write all java classes
			writeJavaTypes(types);
		} catch (final IOException e) {
			throw new MojoExecutionException("Could not write item types.", e);
		}
	}

	protected List<JavaEnum> generateEnums() {
		final List<JavaEnum> ret = new ArrayList<>();

		for (final EnumType enumType : typeDefinitions.getEnumTypes().values()) {
			final JavaEnum enumeration = new JavaEnum(enumType.getName(), enumType.getPackage());
			enumeration.setDescription(enumType.getDescription());

			populateInterfaces(enumType.getInterfaces(), enumeration);

			for (final EnumValue value : enumType.getValue()) {
				final JavaEnumValue v = new JavaEnumValue();
				v.setName(value.getCode());
				v.setInternalName(value.getValue());

				enumeration.addValue(v);
			}

			ret.add(enumeration);
		}

		return ret;
	}

	protected void populateInterfaces(final Interfaces interfaces, final AbstractComplexJavaType javaType) {
		if (interfaces != null && CollectionUtils.isNotEmpty(interfaces.getInterface())) {
			for (final Interface i : interfaces.getInterface()) {
				final int split = StringUtils.lastIndexOf(i.getJavaClass(), ".");
				final JavaInterface iface = new JavaInterface(
						StringUtils.substring(i.getJavaClass(), split + 1, i.getJavaClass().length()),
						StringUtils.substring(i.getJavaClass(), 0, split));

				if (i.getGenericType() != null && CollectionUtils.isNotEmpty(i.getGenericType())) {
					for (final Argument a : i.getGenericType()) {
						final JavaMemberType argumentType = new JavaMemberType(a.getValue());
						iface.addGenericArgument(new JavaGenericTypeArgument(argumentType, false));
					}
				}

				javaType.addInterface(iface);
			}
		}
	}

	protected List<JavaClass> generateBeans() throws MojoExecutionException {
		final List<JavaClass> ret = new ArrayList<>();

		for (final BeanType beanType : typeDefinitions.getBeanTypes().values()) {
			final JavaClass bean = new JavaClass(beanType.getName(), beanType.getPackage());
			bean.setDescription(beanType.getDescription());
			bean.setVisibility(Visibility.PUBLIC);
			populateSuperType(beanType, typeDefinitions.getBeanTypes().get(beanType.getExtends()), bean, Bean.class);
			populateInterfaces(beanType.getInterfaces(), bean);

			if (beanType.getProperties() != null) {
				for (final Property prop : beanType.getProperties().getProperty()) {
					final JavaMemberType propType = createMemberType(prop.getType());

					final JavaField field = new JavaField();
					field.setName(prop.getName());

					if (prop.getDefaultValue() != null && prop.getDefaultValue().getContent() != null) {
						try {
							field.setAssignement(new JavaExpression(prop.getDefaultValue().getContent(), propType));
						} catch (final ClassNotFoundException e) {
							throw new MojoExecutionException(String
									.format(String.format("Could not set default value for property %s of bean type %s",
											field.getName(), bean.getFullyQualifiedName())),
									e);
						}
					}

					field.setType(propType);
					bean.addField(field);
					populatePropertyValidators(prop.getValidators(), field);

					addGetter(field, bean);
					addSetter(field, bean);
				}
			}

			ret.add(bean);
		}

		return ret;
	}

	protected List<JavaClass> generateItemTypes() throws MojoExecutionException {
		final List<JavaClass> ret = new ArrayList<>();

		for (final Map.Entry<String, ItemType> typeEntry : typeDefinitions.getItemTypes().entrySet()) {
			final ItemType itemType = typeEntry.getValue();

			// don't generate the base Item type
			if (StringUtils.equals(Item.class.getSimpleName(), itemType.getName())) {
				continue;
			}

			final JavaClass itemTypeClass = createItemTypeClass(itemType);
			populateSuperType(itemType, typeDefinitions.getItemTypes().get(itemType.getExtends()), itemTypeClass,
					Item.class);
			populateInterfaces(itemType.getInterfaces(), itemTypeClass);
			populateProperties(itemType, itemTypeClass);
			populateRelationProperties(itemType, itemTypeClass);

			ret.add(itemTypeClass);
		}

		return ret;
	}

	protected void populateProperties(final ItemType type, final JavaClass javaClass) throws MojoExecutionException {
		// add item type constant
		final JavaField typeCodeConstant = new JavaField();
		typeCodeConstant.setVisibility(Visibility.PUBLIC);
		typeCodeConstant.addModifier(JavaMemberModifier.STATIC);
		typeCodeConstant.addModifier(JavaMemberModifier.FINAL);
		typeCodeConstant.setAssignement(new JavaExpression(type.getTypeCode(), JavaValueType.STRING));
		typeCodeConstant.setType(new JavaMemberType(String.class));
		typeCodeConstant.setName("TYPECODE");

		javaClass.addField(typeCodeConstant);

		if (type.getProperties() != null) {
			for (final Property prop : type.getProperties().getProperty()) {
				final JavaMemberType propType = createMemberType(prop.getType());

				final JavaField field = new JavaField();
				field.setVisibility(Visibility.PROTECTED);
				field.setType(propType);
				field.setName(prop.getName());
				field.setDescription(StringUtils.trim(prop.getDescription()));

				if (prop.getDefaultValue() != null && prop.getDefaultValue().getContent() != null) {
					try {
						field.setAssignement(
								new JavaExpression(prop.getDefaultValue().getContent(), JavaValueType.LITERAL));
					} catch (final Exception e) {
						throw new MojoExecutionException(String
								.format(String.format("Could not set default value for property %s of item type %s",
										field.getName(), javaClass.getFullyQualifiedName())),
								e);
					}
				}

				populatePropertyAnnotation(prop, field);
				populatePropertyValidators(prop.getValidators(), field);

				javaClass.addField(field);

				boolean isReadable = true;
				boolean isWritable = true;

				if (prop.getModifiers() != null) {
					isReadable = prop.getModifiers().isReadable();
					isWritable = prop.getModifiers().isWritable();
				}

				if (isReadable) {
					if (prop.isLocalized()) {
						addLocalizedGetters(prop, field, javaClass);
					} else {
						addGetter(field, javaClass);
					}
				}

				if (isWritable) {
					if (prop.isLocalized()) {
						addLocalizedSetters(prop, field, javaClass);
					} else {
						addSetter(field, javaClass);
					}
				}

				// add constant for each property
				final JavaField constant = new JavaField();
				constant.setVisibility(Visibility.PUBLIC);
				constant.addModifier(JavaMemberModifier.STATIC);
				constant.addModifier(JavaMemberModifier.FINAL);
				constant.setAssignement(new JavaExpression(prop.getName(), JavaValueType.STRING));
				constant.setType(new JavaMemberType(String.class));
				constant.setName("PROPERTY_" + generateConstantName(prop.getName()));

				javaClass.addField(constant);
			}
		}
	}

	protected String generateConstantName(final String fieldName) {
		final StringBuilder builder = new StringBuilder();
		int index = 0;
		boolean lastCharWasUpperCase = false;
		for (final char c : fieldName.toCharArray()) {
			if (Character.isUpperCase(c) && index > 0 && !lastCharWasUpperCase) {
				builder.append("_");
				lastCharWasUpperCase=true;
			} else {
				lastCharWasUpperCase = false;
			}

			builder.append(c);

			index++;
		}

		return builder.toString().toUpperCase(Locale.ENGLISH);
	}

	private Optional<String> getLocalizedType(final String localizableTypeCode) {
		final ItemType localizableType = typeDefinitions.getItemTypes().get(localizableTypeCode);

		if (localizableType.getInterfaces() != null
				&& CollectionUtils.isNotEmpty(localizableType.getInterfaces().getInterface())) {
			final Optional<String> genericType = localizableType.getInterfaces().getInterface().stream()//
					.filter(i -> Localizable.class.getName().equals(i.getJavaClass())) //
					.map(i -> i.getGenericType() != null ? i.getGenericType().get(0).getValue() : null).findFirst();

			return genericType;
		}

		return Optional.empty();
	}

	/**
	 * This generates 2 getters, one with a {@link Locale} as single parameter.
	 * Furthermore it calls {@link Localizable#get()} on the field, which
	 * implies that the field type is implementing {@link Localizable}!
	 *
	 * @throws MojoExecutionException
	 */
	protected void addLocalizedGetters(final Property prop, final JavaField field, final JavaClass javaClass)
			throws MojoExecutionException {

		// TODO: needs refactoring
		final Optional<String> localizedType = getLocalizedType(prop.getType());

		if (!localizedType.isPresent()) {
			throw new MojoExecutionException(
					"Cannot generate localized getters because field type is not of type Localizable");
		}

		final JavaMethod getter = addGetter(field, javaClass,
				String.format(getNullInitializationString(prop) + "return this.%s.get();", field.getName()));
		getter.setType(new JavaMemberType(localizedType.get()));

		final JavaMethod locGetter = addGetter(field, javaClass,
				String.format(getNullInitializationString(prop) + "return this.%s.get(locale);", field.getName()));
		locGetter.addArgument("locale", new JavaMemberType(Locale.class));
		locGetter.setType(getter.getType());
	}

	private String getNullInitializationString(final Property property) {
		// initialize the field on first access if null
		final String nullInitializationAssignment = String.format("if (this.%s == null) this.%s = new %s(); ",
				property.getName(), property.getName(), property.getType());

		return nullInitializationAssignment;
	}

	protected JavaMethod addGetter(final JavaField field, final JavaClass javaClass) {
		return addGetter(field, javaClass, String.format("return this.%s;", field.getName()));
	}

	protected JavaMethod addGetter(final JavaField field, final JavaClass javaClass, final String codeBlock) {
		final JavaMethod getter = new JavaMethod();

		if ("boolean".equals(field.getType().getFullyQualifiedName())) {
			getter.setName(generateMethodName("is", field.getName()));
		} else {
			getter.setName(generateMethodName("get", field.getName()));
		}
		getter.setType(field.getType());
		getter.setDescription(field.getDescription());
		getter.setVisibility(Visibility.PUBLIC);
		getter.setCodeBlock(codeBlock);

		final JavaAnnotation accessorAnnotation = new JavaAnnotation(Accessor.class);
		accessorAnnotation.addParameter("propertyName", field.getName(), JavaValueType.STRING);
		accessorAnnotation.addParameter("type", AccessorType.get, JavaValueType.ENUM_VALUE);
		getter.addAnnotation(accessorAnnotation);

		javaClass.addMethod(getter);

		return getter;
	}

	/**
	 * This generates 2 getters, one with a {@link Locale} as single parameter.
	 * Furthermore it calls {@link Localizable#get()} on the field, which
	 * implies that the field type is implementing {@link Localizable}!
	 *
	 * @param prop
	 * @throws MojoExecutionException
	 */
	protected void addLocalizedSetters(final Property prop, final JavaField field, final JavaClass javaClass)
			throws MojoExecutionException {
		// TODO: needs refactoring
		final Optional<String> localizedType = getLocalizedType(prop.getType());

		if (!localizedType.isPresent()) {
			throw new MojoExecutionException(
					"Cannot generate localized setters because field type is not of type Localizable");
		}

		final JavaMethod setter = addSetter(field, javaClass, String
				.format(getNullInitializationString(prop) + "this.%s.set(%s);", field.getName(), field.getName()));
		setter.getArguments().get(0).setType(new JavaMemberType(localizedType.get()));

		final JavaMethod locSetter = addSetter(field, javaClass, String.format(
				getNullInitializationString(prop) + "this.%s.set(locale, %s);", field.getName(), field.getName()));
		locSetter.addArgument("locale", new JavaMemberType(Locale.class));
		locSetter.getArguments().get(0).setType(new JavaMemberType(localizedType.get()));
	}

	protected JavaMethod addSetter(final JavaField field, final JavaClass javaClass) {
		return addSetter(field, javaClass, String.format("this.%s = %s;", field.getName(), field.getName()));
	}

	protected JavaMethod addSetter(final JavaField field, final JavaClass javaClass, final String codeBlock) {
		final JavaMethod setter = new JavaMethod();
		setter.setName(generateMethodName("set", field.getName()));
		setter.setType(JavaMemberType.VOID);
		setter.setDescription(field.getDescription());
		setter.addArgument(field.getName(), field.getType());
		setter.setCodeBlock(codeBlock);
		setter.setVisibility(Visibility.PUBLIC);

		final JavaAnnotation accessorAnnotation = new JavaAnnotation(Accessor.class);
		accessorAnnotation.addParameter("propertyName", field.getName(), JavaValueType.STRING);
		accessorAnnotation.addParameter("type", AccessorType.set, JavaValueType.ENUM_VALUE);
		setter.addAnnotation(accessorAnnotation);

		javaClass.addMethod(setter);

		return setter;
	}

	protected JavaClass createItemTypeClass(final ItemType type) throws MojoExecutionException {
		final JavaClass javaClass = new JavaClass(type.getName(), type.getPackage());
		javaClass.setDescription(type.getDescription());
		javaClass.setVisibility(Visibility.PUBLIC);

		// add itemtype annotation, ignore base item type
		final JavaAnnotation typeAnnotation = new JavaAnnotation(
				io.spotnext.core.infrastructure.annotation.ItemType.class);

		if (StringUtils.isBlank(type.getTypeCode())) {
			throw new MojoExecutionException(String.format("No typecode set for type %s", type.getName()));
		}

		typeAnnotation.addParameter("typeCode", type.getTypeCode(), JavaValueType.STRING);
		javaClass.addAnnotation(typeAnnotation);

		typeAnnotation.addParameter("persistable", type.isPersistable(), JavaValueType.LITERAL);

		if (type.isAbstract() != null && type.isAbstract()) {
			javaClass.setAbstract(true);
		}

		return javaClass;
	}

	/**
	 * Populates the super class for the given JavaType.
	 * 
	 * @param javaClass
	 *            the class to populate with a super types
	 * @param defaultSuperclass
	 *            is used when there is no superType given, can be null too
	 */
	protected void populateSuperType(final io.spotnext.core.infrastructure.maven.xml.JavaType type,
			final io.spotnext.core.infrastructure.maven.xml.JavaType superType, final JavaClass javaClass,
			final Class<?> defaultSuperclass) throws MojoExecutionException {

		final JavaInterface superClass = new JavaInterface();

		if (StringUtils.isNotBlank(type.getExtends())) {
			if (superType != null) {
				superClass.setName(superType.getName());
				superClass.setPackagePath(superType.getPackage());
			} else {
				throw new MojoExecutionException(
						String.format("Super type %s not found for type %s", type.getExtends(), type.getName()));
			}
		} else {
			if (defaultSuperclass != null) {
				superClass.setName(defaultSuperclass.getSimpleName());
				superClass.setPackagePath(defaultSuperclass.getPackage().getName());
			}
		}

		javaClass.setSuperClass(superClass);
	}

	protected String generateMethodName(final String prefix, final String name) {
		return prefix + StringUtils.capitalize(name);
	}

	protected JavaMemberType createMemberType(final String typeName) throws MojoExecutionException {
		final BaseType propType = typeDefinitions.getType(typeName);

		JavaMemberType ret = null;

		if (propType instanceof AtomicType) {
			ret = new JavaMemberType(((AtomicType) propType).getClassName());

			if (BooleanUtils.isTrue(((AtomicType) propType).isArray())) {
				ret.setArray(true);
			}
		} else if (propType instanceof EnumType) {
			ret = new JavaMemberType(((EnumType) propType).getName(), ((EnumType) propType).getPackage());
		} else if (propType instanceof CollectionType) {
			final CollectionType t = (CollectionType) propType;

			// check if the element type is an enum or an atomic type, all
			// others are not supported
			if (!typeDefinitions.getAtomicTypes().containsKey(t.getElementType())
					&& !typeDefinitions.getEnumTypes().containsKey(t.getElementType())) {
				throw new MojoExecutionException(
						String.format("Type '%s' is not supported as collection element type", t.getElementType()));
			}

			ret = createCollectionMemberType(t.getCollectionType(), t.getElementType());

		} else if (propType instanceof MapType) {
			final MapType t = (MapType) propType;

			// check if the key type is an enum or an atomic type, all others
			// are not supported
			if (!typeDefinitions.getAtomicTypes().containsKey(t.getKeyType())
					&& !typeDefinitions.getEnumTypes().containsKey(t.getKeyType())) {
				throw new MojoExecutionException(
						String.format("Type '%s' is not supported as map key type", t.getKeyType()));
			}

			// check if the value type is an enum or an atomic type, all others
			// are not supported
			if (!typeDefinitions.getAtomicTypes().containsKey(t.getValueType())
					&& !typeDefinitions.getEnumTypes().containsKey(t.getValueType())) {
				throw new MojoExecutionException(
						String.format("Type '%s' is not supported as map key type", t.getValueType()));
			}

			ret = createMapMemberType(t.getKeyType(), t.getValueType());

		} else if (propType instanceof ItemType) {
			ret = new JavaMemberType(((ItemType) propType).getName(), ((ItemType) propType).getPackage());
		}

		if (ret == null) {
			throw new MojoExecutionException(String.format("Could not resolve type '%s'", typeName));
		}

		return ret;
	}

	protected JavaMemberType createCollectionMemberType(final CollectionsType collectionType, final String elementType)
			throws MojoExecutionException {

		JavaMemberType ret = null;

		// TODO: temporarily disabled, this would not work with hibernate FETCH
		// JOINS!
		// if (CollectionsType.COLLECTION.equals(collectionType)) {
		// ret = new JavaMemberType(Collection.class);
		// } else if (CollectionsType.SET.equals(collectionType)) {
		// ret = new JavaMemberType(Set.class);
		// } else {
		// ret = new JavaMemberType(List.class);
		// }

		ret = new JavaMemberType(Set.class);

		// add generic collection type
		final JavaMemberType genType = createMemberType(elementType);
		final JavaGenericTypeArgument arg = new JavaGenericTypeArgument(genType, false);
		ret.addGenericArgument(arg);

		return ret;
	}

	protected JavaMemberType createMapMemberType(final String keyTypeName, final String valueTypeName)
			throws MojoExecutionException {

		final JavaMemberType ret = new JavaMemberType(Map.class);

		// add generic key type
		final JavaMemberType keyType = createMemberType(keyTypeName);
		final JavaGenericTypeArgument keyArg = new JavaGenericTypeArgument(keyType, false);
		ret.addGenericArgument(keyArg);

		// add generic value type
		final JavaMemberType valType = createMemberType(valueTypeName);
		final JavaGenericTypeArgument valArg = new JavaGenericTypeArgument(valType, false);
		ret.addGenericArgument(valArg);

		return ret;
	}

	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	protected void writeJavaTypes(final List<AbstractComplexJavaType> types)
			throws IOException, MojoExecutionException {

		for (final AbstractComplexJavaType type : types) {
			final String srcPackagePath = type.getPackagePath().replaceAll("\\.", File.separator);

			final Path filePath = Paths.get(targetClassesDirectory.getAbsolutePath(), srcPackagePath,
					type.getName() + ".java");

			if (filePath == null) {
				throw new IOException("Could not read access target file path");
			}

			if (Files.exists(filePath)) {
				Files.delete(filePath);
			} else {
				if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
					Files.createDirectories(filePath.getParent());
				}
			}

			Files.createFile(filePath);

			Writer writer = null;

			try {
				writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8));

				final String encodedType = encodeType(type);
				writer.write(encodedType);
			} finally {
				MiscUtil.closeQuietly(writer);
			}

			// format code
			if (formatSource) {
				formatSourceCode(filePath.toFile());
			}
		}
	}

	protected void formatSourceCode(final File sourceFile) throws FileNotFoundException {
		jalopy.setInput(sourceFile);
		jalopy.setOutput(sourceFile);
		jalopy.format();
	}

	protected String encodeType(final AbstractJavaObject type) throws MojoExecutionException {
		final TemplateFile template = ClassUtil.getAnnotation(type.getClass(), TemplateFile.class);

		if (StringUtils.isEmpty(template.value())) {
			throw new MojoExecutionException(
					String.format("No velocity template defined for type %s", type.getClass()));
		}

		final Template t = velocityEngine.getTemplate("templates/" + template.value());
		final Context context = VelocityUtil.createSingletonObjectContext(type);

		final StringWriter writer = new StringWriter();
		t.merge(context, writer);

		return writer.toString();
	}

	protected void populatePropertyAnnotation(final Property propertyDefinition, final JavaField field) {
		final JavaAnnotation propAnn = new JavaAnnotation(io.spotnext.core.infrastructure.annotation.Property.class);
		field.addAnnotation(propAnn);

		boolean addGetter = io.spotnext.core.infrastructure.annotation.Property.DEFAULT_READABLE;
		boolean addSetter = io.spotnext.core.infrastructure.annotation.Property.DEFAULT_WRITABLE;

		if (propertyDefinition.getModifiers() != null) {
			if (io.spotnext.core.infrastructure.annotation.Property.DEFAULT_UNIQUE != propertyDefinition.getModifiers()
					.isUnique()) {
				propAnn.addParameter("unique", propertyDefinition.getModifiers().isUnique(), JavaValueType.LITERAL);
			}

			if (io.spotnext.core.infrastructure.annotation.Property.DEFAULT_INITIAL != propertyDefinition.getModifiers()
					.isInitial()) {
				propAnn.addParameter("initial", propertyDefinition.getModifiers().isInitial(), JavaValueType.LITERAL);
			}

			addGetter = propertyDefinition.getModifiers().isReadable();
			addSetter = propertyDefinition.getModifiers().isWritable();
		}

		if (addGetter) {
			propAnn.addParameter("readable", addGetter, JavaValueType.LITERAL);
		}

		if (addSetter) {
			propAnn.addParameter("writable", addSetter, JavaValueType.LITERAL);
		}

		if (propertyDefinition.getAccessors() != null
				&& StringUtils.isNotBlank(propertyDefinition.getAccessors().getValueProvider())) {

			propAnn.addParameter("itemValueProvider", propertyDefinition.getAccessors().getValueProvider(),
					JavaValueType.STRING);
		}

		if (propertyDefinition.getPersistence() != null
				&& propertyDefinition.getPersistence().getColumnType() != null) {
			propAnn.addParameter("columnType", propertyDefinition.getPersistence().getColumnType(),
					JavaValueType.ENUM_VALUE);
		}
	}

	protected void populateRelationProperties(final ItemType type, final JavaClass javaClass)
			throws MojoExecutionException {

		// TODO: needs refactoring

		for (final RelationType rel : typeDefinitions.getRelationTypes().values()) {
			RelationNode sourceNode = null;
			RelationNode targetNode = null;

			if (rel.getSource() != null && type.getName().equals(rel.getSource().getItemType())) {
				// this means that the current type is on the "source side" of
				// the relation
				sourceNode = rel.getSource();
			}

			if (rel.getTarget() != null && type.getName().equals(rel.getTarget().getItemType())) {
				// this means that the current type is on the "target side" of
				// the relation
				targetNode = rel.getTarget();
			}

			if (sourceNode != null || targetNode != null) {
				final JavaAnnotation relationAnn = new JavaAnnotation(Relation.class);

				// only create relation properties if an actual relation exists
				final JavaField property = new JavaField();
				property.setDescription(rel.getDescription());

				relationAnn.addParameter("relationName", rel.getName(), JavaValueType.STRING);

				// use the mappedBy value of the other node as the property name
				if (sourceNode != null) {
					populateRelationProperty(sourceNode, rel.getTarget(), RelationNodeType.SOURCE, javaClass, property,
							relationAnn);
				} else if (targetNode != null) {
					populateRelationProperty(targetNode, rel.getSource(), RelationNodeType.TARGET, javaClass, property,
							relationAnn);
				}
			}
		}
	}

	protected void populateRelationProperty(final RelationNode from, final RelationNode to,
			final RelationNodeType nodeType, final JavaClass javaClass, final JavaField property,
			final JavaAnnotation relationAnn) throws MojoExecutionException {

		final String mappedTo = to.getMappedBy();
		RelationCollectionType collectionType = getCollectionType(from.getCollectionType());

		if (StringUtils.isNotBlank(mappedTo)) {
			property.setName(mappedTo);

			final io.spotnext.core.infrastructure.type.RelationType relationType = getRelationType(from, to);
			relationAnn.addParameter("type", relationType, JavaValueType.ENUM_VALUE);
			relationAnn.addParameter("mappedTo", from.getMappedBy(), JavaValueType.STRING);
			relationAnn.addParameter("nodeType", nodeType, JavaValueType.ENUM_VALUE);

			if (to.getCardinality().equals(RelationshipCardinality.MANY)) {
				collectionType = getCollectionType(to.getCollectionType());
				relationAnn.addParameter("collectionType", collectionType, JavaValueType.ENUM_VALUE);
			}

			// always use Sets for now, because hibernate can't handle multiple
			// Collections
			// when using FETCH JOINS.
			final JavaMemberType propType = createRelationPropertyMemberType(to.getCardinality(), to.getItemType(),
					collectionType);
			property.setType(propType);

			property.addAnnotation(relationAnn);
			javaClass.addField(property);

			// add modifiers
			final JavaAnnotation propAnn = new JavaAnnotation(
					io.spotnext.core.infrastructure.annotation.Property.class);
			property.addAnnotation(propAnn);

			populatePropertyValidators(to.getValidators(), property);

			boolean addGetter = io.spotnext.core.infrastructure.annotation.Property.DEFAULT_READABLE;
			boolean addSetter = io.spotnext.core.infrastructure.annotation.Property.DEFAULT_WRITABLE;

			if (to.getModifiers() != null) {
				if (io.spotnext.core.infrastructure.annotation.Property.DEFAULT_UNIQUE != to.getModifiers()
						.isUnique()) {
					propAnn.addParameter("unique", to.getModifiers().isUnique(), JavaValueType.LITERAL);
				}

				if (addGetter != to.getModifiers().isInitial()) {
					propAnn.addParameter("initial", to.getModifiers().isInitial(), JavaValueType.LITERAL);
				}

				addGetter = to.getModifiers().isReadable();
				addSetter = to.getModifiers().isWritable();
			}

			if (addGetter) {
				propAnn.addParameter("readable", addGetter, JavaValueType.LITERAL);

				if (io.spotnext.core.infrastructure.type.RelationType.OneToMany.equals(relationType)) {
					// wrap the collection into proxy collection that allows us
					// to intercept
					// mutating calls (like add, remove) -> needed to update
					// relation infos
					javaClass.getImports().add("io.spotnext.core.infrastructure.support.ItemCollectionFactory");
					addGetter(property, javaClass,
							String.format("return ItemCollectionFactory.wrap(this, \"%s\", this.%s);",
									property.getName(), property.getName()));

				} else {
					addGetter(property, javaClass);
				}
			}

			if (addSetter) {
				propAnn.addParameter("writable", addSetter, JavaValueType.LITERAL);
				addSetter(property, javaClass);
			}

			// add constant for each property
			final JavaField constant = new JavaField();
			constant.setVisibility(Visibility.PUBLIC);
			constant.addModifier(JavaMemberModifier.STATIC);
			constant.addModifier(JavaMemberModifier.FINAL);
			constant.setAssignement(new JavaExpression(property.getName(), JavaValueType.STRING));
			constant.setType(new JavaMemberType(String.class));
			constant.setName("PROPERTY_" + generateConstantName(property.getName()));

			javaClass.addField(constant);
		}

	}

	protected io.spotnext.core.infrastructure.type.RelationType getRelationType(final RelationNode thisNode,
			final RelationNode otherNode) {

		if (RelationshipCardinality.ONE.equals(thisNode.getCardinality())
				&& RelationshipCardinality.ONE.equals(otherNode.getCardinality())) {
			return io.spotnext.core.infrastructure.type.RelationType.OneToOne;
		} else if (RelationshipCardinality.MANY.equals(thisNode.getCardinality())
				&& RelationshipCardinality.ONE.equals(otherNode.getCardinality())) {
			return io.spotnext.core.infrastructure.type.RelationType.ManyToOne;
		} else if (RelationshipCardinality.ONE.equals(thisNode.getCardinality())
				&& RelationshipCardinality.MANY.equals(otherNode.getCardinality())) {
			return io.spotnext.core.infrastructure.type.RelationType.OneToMany;
		} else if (RelationshipCardinality.MANY.equals(thisNode.getCardinality())
				&& RelationshipCardinality.MANY.equals(otherNode.getCardinality())) {
			return io.spotnext.core.infrastructure.type.RelationType.ManyToMany;
		}

		return null;
	}

	protected RelationCollectionType getCollectionType(final CollectionsType collectionType) {
		if (CollectionsType.SET.equals(collectionType)) {
			return RelationCollectionType.Set;
		} else if (CollectionsType.COLLECTION.equals(collectionType)) {
			return RelationCollectionType.Collection;
		}

		return RelationCollectionType.List;
	}

	protected JavaMemberType createRelationPropertyMemberType(final RelationshipCardinality cardinality,
			final String elementType, final RelationCollectionType collectionType) throws MojoExecutionException {

		JavaMemberType type = null;

		if (RelationshipCardinality.MANY.equals(cardinality)) {
			final CollectionsType colType = RelationCollectionType.List.equals(collectionType) ? CollectionsType.LIST
					: CollectionsType.SET;

			type = createCollectionMemberType(colType, elementType);
		} else {
			type = createMemberType(elementType);
		}

		return type;
	}

	/**
	 * Adds JSR-303 validators to the property.
	 */
	protected void populatePropertyValidators(final Validators validators, final JavaField field) {
		if (validators != null) {
			for (final Validator v : validators.getValidator()) {
				final JavaAnnotation ann = new JavaAnnotation(new JavaMemberType(v.getJavaClass()));

				if (CollectionUtils.isNotEmpty(v.getArgument())) {
					for (final ValidatorArgument a : v.getArgument()) {
						if (a.getNumberValue() != null) {
							ann.addParameter(a.getName(), a.getNumberValue(), JavaValueType.LITERAL);
						} else if (a.getStringValue() != null) {
							ann.addParameter(a.getName(), a.getStringValue(), JavaValueType.STRING);
						} else {
							getLog().warn(String.format(
									"Validator for property %s misconfigured, all attribute values are empty",
									field.getName()));
						}
					}
				}

				field.addAnnotation(ann);
			}
		}
	}

	protected String getSimpleClassName(final String className) {
		final int start = StringUtils.lastIndexOf(className, ".") + 1;
		final int end = StringUtils.length(className);
		return StringUtils.substring(className, start, end);
	}

	protected String getDefaultFieldAssignment(final JavaClass type, final JavaField field, final String typeName) {
		final BaseType propType = typeDefinitions.getType(typeName);
		String ret = null;

		if (propType instanceof CollectionType) {
			final CollectionType attrType = (CollectionType) propType;
			if (CollectionsType.SET.equals(attrType.getCollectionType())) {
				ret = "new HashSet<>();";
				type.addImport(HashSet.class);
			} else {
				ret = "new ArrayList<>();";
				type.addImport(ArrayList.class);
			}
		} else if (propType instanceof MapType) {
			ret = "new HashMap<>();";
			type.addImport(HashMap.class);
		}

		return ret;
	}
}
