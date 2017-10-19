package at.spot.core.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import at.spot.core.CoreInit;
import at.spot.core.infrastructure.service.LoggingService;
import at.spot.core.infrastructure.service.ModelService;
import at.spot.core.persistence.service.PersistenceService;
import at.spot.core.persistence.service.TransactionService;

/**
 * This is the base class for all integration tasks..
 */
@TestPropertySource(properties = { "service.persistence.mapdb.filepath=" + AbstractIntegrationTest.MAPDB_FILE })
@RunWith(SpotJunitRunner.class)
@IntegrationTest
@SpringBootTest(classes = { CoreInit.class })
@Transactional
public abstract class AbstractIntegrationTest {

	public static final String MAPDB_FILE = "/var/tmp/spot-core.test.db";

	@Resource
	protected PersistenceService persistenceService;

	@Resource
	protected TransactionService transactionService;

	@Resource
	protected LoggingService loggingService;

	@Resource
	protected ModelService modelService;

	protected String getTestPackagePath() {
		return this.getClass().getPackage().getName();
	}

	/**
	 * Called before all tests are executed.
	 */
	@BeforeClass
	public static void initialize() {
		removeMapDbFile();
	}

	/**
	 * Called when all tests have been executed.
	 */
	@AfterClass
	public static void shutdown() {
		removeMapDbFile();
	}

	/**
	 * Called before each test is executed.
	 */
	@Before
	public void beforeTest() {
		MockitoAnnotations.initMocks(this);

		try {
			transactionService.start();
			prepareTest();
		} catch (final Exception e) {
			loggingService.exception(String.format("Could not prepare test %s", this.getClass().getName()), e);
		}
	}

	/**
	 * Called after each test has been executed.
	 */
	@After
	public void afterTest() {
		try {
			transactionService.rollback();
			teardownTest();
		} catch (final Exception e) {
			loggingService.exception(String.format("Could not teardown test %s", this.getClass().getName()), e);
		}

		// TODO: revert transaction
	}

	/**
	 * Removes the temporary MapDB database file.
	 */
	protected static void removeMapDbFile() {
		try {
			Files.deleteIfExists(Paths.get(MAPDB_FILE));
		} catch (IOException e) {
			System.out.println("Could not remove temporary MapDB storage file.");
		}
	}

	/**
	 * Runs custom code before a test is executed, eg. to prepare test data.
	 */
	protected abstract void prepareTest();

	/**
	 * Runs after each test, eg. to clean up stuff.
	 */
	protected abstract void teardownTest();
}
