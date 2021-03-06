package paper;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import helio.blueprints.TranslationUnit;
import helio.blueprints.UnitBuilder;
import helio.blueprints.components.ComponentType;
import helio.blueprints.components.Components;
import helio.blueprints.exceptions.ExtensionNotFoundException;
import helio.blueprints.exceptions.IncompatibleMappingException;
import helio.blueprints.exceptions.IncorrectMappingException;
import helio.blueprints.exceptions.TranslationUnitExecutionException;
import helio.builder.jld11map.JLD11Builder;

public class Main {


	public static void main(String[] args) throws Exception {
		try {
			if (Arrays.asList(args).contains("-debug")) {
				Scanner s = new Scanner(System.in);
				System.out.println("Press enter to continue.....");
				s.nextLine();
				s.close();
			}
			String outputFile = getParameter(args, "-o");
			String mappingFile = getParameter(args, "-m");
			String iterations = getParameter(args, "-i");
			int times = 1;
			if (mappingFile == null)
				throw new IllegalArgumentException("A mapping file must be provided using -m argument");
			if (outputFile == null) {
				System.out.println("Since no output file was provided with -o, results will be stored into output.rdf");
				outputFile = "./output.rdf";
			}
			if (iterations != null) {
				times = Integer.valueOf(iterations);
			}
			int index = 1;
			String result = "";
			while (index <= times) {
				long start1 = System.currentTimeMillis();
				TranslationUnit unit = build(mappingFile);
				unit.getTask().run();
				result = unit.getDataTranslated().get(0);
				unit.flushDataTranslated();
				long end2 = System.currentTimeMillis();
				System.out.println("Elapsed Time in milli seconds: " + (end2 - start1));
				index++;
			}
			Writer writer = new FileWriter(outputFile);
			writer.write(result);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(1);
	}

	public static String getParameter(String[] args, String parameter) {
		Integer index = Arrays.asList(args).indexOf(parameter);
		if (index > -1) {
			return args[index + 1];
		}
		return null;
	}

	static {

		try {
			Components.registerAndLoad(
					"https://github.com/helio-ecosystem/helio-providers-web/releases/download/v0.1.1/helio-providers-web-0.1.1.jar",
					"helio.providers.HttpProvider", ComponentType.PROVIDER);
		} catch (ExtensionNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Components.registerAndLoad("/Users/andreacimmino/Desktop/helio-provider-url-0.1.0.jar",
					"provider.URLProvider", ComponentType.PROVIDER);
		} catch (ExtensionNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Components.registerAndLoad("/Users/andreacimmino/Desktop/helio-handler-csv-0.1.0.jar",
					"handlers.CsvHandler", ComponentType.HANDLER);
		} catch (ExtensionNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Components.registerAndLoad(
					"https://github.com/helio-ecosystem/helio-handler-jayway/releases/download/v0.1.1/helio-handler-jayway-0.1.1.jar",
					"handlers.JsonHandler", ComponentType.HANDLER);
		} catch (ExtensionNotFoundException e) {
			e.printStackTrace();
		}

		try {
			Components.registerAndLoad(
					"https://github.com/helio-ecosystem/helio-provider-files/releases/download/v0.1.1/helio-provider-files-0.1.1.jar",
					"helio.providers.files.FileProvider", ComponentType.PROVIDER);

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Components.registerAndLoad(
					"https://github.com/helio-ecosystem/helio-provider-files/releases/download/v0.1.1/helio-provider-files-0.1.1.jar",
					"helio.providers.files.FileWatcherProvider", ComponentType.PROVIDER);

		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Components.registerAndLoad(null, "helio.builder.jld11map.DummyProvider", ComponentType.PROVIDER);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String readFile(String file) {
		try {
			return Files.readString(Path.of(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static TranslationUnit build(String mappingFile) throws IncompatibleMappingException,
			TranslationUnitExecutionException, IncorrectMappingException, ExtensionNotFoundException {
		TranslationUnit unit = null;

		String mapping = readFile(mappingFile);

		UnitBuilder builder = new JLD11Builder();
		Set<TranslationUnit> list = builder.parseMapping(mapping);
		unit = list.iterator().next();

		return unit;
	}

	public static String runUnit(TranslationUnit unit, ExecutorService service)
			throws InterruptedException, ExecutionException, TranslationUnitExecutionException {
		String result = "";

		Future<?> f = service.submit(unit.getTask());
		f.get();
		result = unit.getDataTranslated().get(0);
		f.cancel(true);
		service.shutdown();

		return result;
	}

	public static final Gson GSON = new Gson();

	public static boolean equals(String result, String expected) {
		JsonObject object1 = GSON.fromJson(result, JsonObject.class);
		JsonObject object2 = GSON.fromJson(expected, JsonObject.class);

		return object1.equals(object2);
	}
}
