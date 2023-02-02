package paper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.apache.jena.ext.com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import helio.blueprints.TranslationUnit;
import helio.blueprints.UnitBuilder;
import helio.blueprints.components.ComponentType;
import helio.blueprints.components.Components;
import helio.blueprints.exceptions.ExtensionNotFoundException;
import helio.blueprints.exceptions.IncompatibleMappingException;
import helio.blueprints.exceptions.IncorrectMappingException;
import helio.blueprints.exceptions.TranslationUnitExecutionException;

public class Main {


	private static String DEFAULT_COMPONENTS = "./default-components.json";

	public static void main(String[] args) throws Exception {
		Map<String, Object> arguments = Maps.newHashMap();
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
				
				result = unit.getTask(arguments).call();
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
		 GSON = new Gson();
		 loadDefaultComponents();
	}

	// -- default components methods
		public static void loadDefaultComponents() {
			JsonArray components = readDefaultComponents();
			for(int i = 0; i < components.size(); i++) {
				JsonObject json = components.get(i).getAsJsonObject();
				try {
					Components.register(json.get("source").getAsString(), json.get("clazz").getAsString(), ComponentType.valueOf(json.get("type").getAsString()));
				}catch(Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}

		private static JsonArray readDefaultComponents() {
			try {
				
				String content = Files.readString(Paths.get(DEFAULT_COMPONENTS));
				return GSON.fromJson(content, JsonArray.class);
			} catch (IOException e) {
				e.printStackTrace();
				//System.exit(-1);
			}
			return null;

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

		UnitBuilder builder = Components.newBuilderInstance("SIoTRxBuilder");
		Set<TranslationUnit> list = builder.parseMapping(mapping);
		unit = list.iterator().next();

		return unit;
	}

	

	public static Gson GSON = new Gson();

	public static boolean equals(String result, String expected) {
		JsonObject object1 = GSON.fromJson(result, JsonObject.class);
		JsonObject object2 = GSON.fromJson(expected, JsonObject.class);

		return object1.equals(object2);
	}
}
