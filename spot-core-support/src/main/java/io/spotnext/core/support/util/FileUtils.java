package io.spotnext.core.support.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({ "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "OS_OPEN_STREAM" })
public class FileUtils {

	/**
	 * list files in the given directory and subdirs (with recursion)
	 *
	 * @param paths
	 */
	public static List<File> getFiles(final String paths) {
		final List<File> filesList = new ArrayList<>();
		for (final String path : paths.split(File.pathSeparator)) {
			final File file = new File(path);
			if (file.isDirectory()) {
				recurse(filesList, file);
			} else {
				filesList.add(file);
			}
		}
		return filesList;
	}

	protected static void recurse(final List<File> filesList, final File f) {
		final File[] list = f.listFiles();
		for (final File file : list) {
			if (file.isDirectory()) {
				recurse(filesList, file);
			} else {
				filesList.add(file);
			}
		}
	}

	/**
	 * Reads a file into an {@link InputStream}.
	 *
	 * @param filePath
	 * @throws FileNotFoundException
	 */
	public static InputStream readFile(final String filePath) throws FileNotFoundException {
		return new FileInputStream(filePath);
	}

	/**
	 * Reads a file into an {@link InputStream}.
	 *
	 * @param file
	 * @throws FileNotFoundException
	 */
	public static InputStream readFile(final File file) throws FileNotFoundException {
		return readFile(file.getAbsolutePath());
	}

	public static InputStream readFileFromZipFile(final ZipFile zipFile, final String relativeFilePath)
			throws FileNotFoundException {

		final Enumeration<? extends ZipEntry> e = zipFile.entries();

		while (e.hasMoreElements()) {
			final ZipEntry entry = e.nextElement();
			// if the entry is not directory and matches relative file then
			// extract it
			if (!entry.isDirectory() && entry.getName().equals(relativeFilePath)) {
				try {
					return zipFile.getInputStream(entry);
				} catch (final IOException e1) {
					throw new FileNotFoundException(String.format("Cannot read file '%s' from zip file '%s'.",
							relativeFilePath, zipFile.getName()));
				}
			}
		}

		throw new FileNotFoundException(
				String.format("File '%s' not found in zip file '%s'.", relativeFilePath, zipFile.getName()));
	}

	/**
	 * Reads a file from a zip file and returns an {@link InputStream} object.
	 * If the file is not found, an exception is thrown.
	 *
	 * @param zipFilePath
	 * @param relativeFilePath
	 * @throws FileNotFoundException
	 */
	public static InputStream readFileFromZipFile(final String zipFilePath, final String relativeFilePath)
			throws FileNotFoundException {

		try {
			return readFileFromZipFile(new ZipFile(zipFilePath), relativeFilePath);
		} catch (final IOException e) {
			throw new FileNotFoundException(
					String.format("Cannot read file '%s' from zip file '%s'.", relativeFilePath, zipFilePath));
		}
	}

	/**
	 * List the content of the given jar
	 *
	 * @param jarPath
	 * @throws IOException
	 */
	public static List<String> getFileListFromJar(final String jarPath) throws IOException {
		final List<String> content = new ArrayList<String>();
		final JarFile jarFile = new JarFile(jarPath);
		final Enumeration<JarEntry> e = jarFile.entries();

		while (e.hasMoreElements()) {
			final JarEntry entry = e.nextElement();
			final String name = entry.getName();
			content.add(name);
		}

		MiscUtil.closeQuietly(jarFile);

		return content;
	}

}
